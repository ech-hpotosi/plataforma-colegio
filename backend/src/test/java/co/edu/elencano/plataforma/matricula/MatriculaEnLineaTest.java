package co.edu.elencano.plataforma.matricula;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.elencano.plataforma.PruebaIntegracion;
import co.edu.elencano.plataforma.usuarios.entidad.Rol;

class MatriculaEnLineaTest extends PruebaIntegracion {

    /** Documentos de estudiantes y acudientes de esta clase de prueba, para no cruzarse con otras. */
    private static final AtomicInteger DOCUMENTO = new AtomicInteger(600000);
    private static final byte[] PDF = "%PDF-1.4 documento de prueba".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0};

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession sesionAdmin;

    @BeforeEach
    void iniciarSesionComoAdmin() throws Exception {
        sesionAdmin = iniciarSesion("admin", "clave-de-prueba");
    }

    private ResultActions enviar(MockHttpServletRequestBuilder peticion, MockHttpSession sesion, String json)
            throws Exception {
        return mockMvc.perform(peticion.with(tokenCsrf()).session(sesion)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private JsonNode leer(ResultActions resultado) throws Exception {
        return objectMapper.readTree(resultado.andReturn().getResponse().getContentAsString());
    }

    private record Escenario(long anioId, long sedeId, long gradoId, long grupoId) {
    }

    private Escenario crearEscenario(int anio, String estado) throws Exception {
        long sede = leer(enviar(post("/api/sedes"), sesionAdmin, "{\"nombre\": \"Sede Matricula " + anio + "\"}"))
                .get("id").asLong();
        long anioId = leer(enviar(post("/api/anios"), sesionAdmin, """
                {"anio": %d, "fechaInicio": "%d-01-20", "fechaFin": "%d-11-30",
                 "periodos": [{"fechaInicio": "%d-01-20", "fechaFin": "%d-11-30", "porcentaje": 100}]}
                """.formatted(anio, anio, anio, anio, anio)).andExpect(status().isCreated())).get("id").asLong();
        if (estado != null) {
            enviar(put("/api/anios/" + anioId + "/estado"), sesionAdmin, "{\"estado\": \"" + estado + "\"}")
                    .andExpect(status().isOk());
        }
        long grado = leer(mockMvc.perform(get("/api/grados").session(sesionAdmin))).get(6).get("id").asLong();
        long grupo = leer(enviar(post("/api/grupos"), sesionAdmin, """
                {"anioLectivoId": %d, "sedeId": %d, "gradoId": %d, "nombre": "01", "jornada": "MANANA", "cupo": 30}
                """.formatted(anioId, sede, grado)).andExpect(status().isCreated())).get("id").asLong();
        return new Escenario(anioId, sede, grado, grupo);
    }

    /** Crea la cuenta desde el registro publico y devuelve la sesion ya iniciada. */
    private MockHttpSession registrarAcudiente(String documento) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/api/auth/registro").with(tokenCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoDocumento": "CC", "numeroDocumento": "%s", "nombres": "Rosa", "apellidos": "Jojoa",
                                 "telefono": "3101234567", "correo": "rosa@correo.co", "contrasena": "clave-rosa-1"}
                                """.formatted(documento)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles", hasItem("ACUDIENTE")))
                .andReturn().getRequest().getSession(false);
    }

    private String jsonSolicitud(Escenario e, String tipo, String documento, String nombres) {
        return """
                {"anioLectivoId": %d, "sedeId": %d, "gradoId": %d, "tipo": "%s", "parentesco": "MADRE",
                 "tipoDocumento": "TI", "numeroDocumento": "%s", "nombres": "%s", "apellidos": "Jojoa Botina",
                 "fechaNacimiento": "2015-04-02", "genero": "FEMENINO", "direccion": "Vereda Santa Lucía",
                 "eps": "Emssanar"}
                """.formatted(e.anioId(), e.sedeId(), e.gradoId(), tipo, documento, nombres);
    }

    private ResultActions subir(MockHttpSession sesion, long solicitud, long tipo, String nombre, byte[] bytes)
            throws Exception {
        return mockMvc.perform(multipart("/api/solicitudes-matricula/" + solicitud + "/documentos/" + tipo)
                .file(new MockMultipartFile("archivo", nombre, "application/octet-stream", bytes))
                .with(tokenCsrf()).session(sesion));
    }

    private List<Long> tiposObligatorios() throws Exception {
        List<Long> tipos = new ArrayList<>();
        for (JsonNode t : leer(mockMvc.perform(get("/api/tipos-documento-matricula").session(sesionAdmin)))) {
            if (t.get("obligatorio").asBoolean()) {
                tipos.add(t.get("id").asLong());
            }
        }
        return tipos;
    }

    /** Crea la solicitud con todos los documentos obligatorios y la envia. Devuelve su id. */
    private long solicitudEnviada(MockHttpSession acudiente, Escenario e, String tipo, String documento)
            throws Exception {
        long id = leer(enviar(post("/api/solicitudes-matricula"), acudiente,
                jsonSolicitud(e, tipo, documento, "Luna")).andExpect(status().isCreated())).get("id").asLong();
        for (long t : tiposObligatorios()) {
            subir(acudiente, id, t, "documento.pdf", PDF).andExpect(status().isOk());
        }
        enviar(post("/api/solicitudes-matricula/" + id + "/enviar"), acudiente, "{\"autorizaDatos\": true}")
                .andExpect(status().isOk());
        return id;
    }

    @Test
    void elAcudienteSeRegistraHaceLaSolicitudYLaSecretariaLaApruebaDespuesDeUnaCorreccion() throws Exception {
        Escenario e = crearEscenario(2061, "MATRICULA");
        MockHttpSession acudiente = registrarAcudiente(String.valueOf(DOCUMENTO.incrementAndGet()));
        String documento = String.valueOf(DOCUMENTO.incrementAndGet());

        mockMvc.perform(get("/api/matricula-en-linea/oferta").session(acudiente))
                .andExpect(jsonPath("$[?(@.anioLectivoId == %d)]".formatted(e.anioId()), hasSize(1)));

        JsonNode creada = leer(enviar(post("/api/solicitudes-matricula"), acudiente,
                jsonSolicitud(e, "NUEVO", documento, "Luna María")).andExpect(status().isCreated()));
        long id = creada.get("id").asLong();
        assertThat(creada.get("estado").asText()).isEqualTo("BORRADOR");
        assertThat(creada.get("editable").asBoolean()).isTrue();

        // Sin documentos no se puede enviar
        enviar(post("/api/solicitudes-matricula/" + id + "/enviar"), acudiente, "{\"autorizaDatos\": true}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.startsWith("Faltan documentos")));

        // Un archivo que no es PDF ni imagen se rechaza aunque diga .pdf
        List<Long> obligatorios = tiposObligatorios();
        subir(acudiente, id, obligatorios.getFirst(), "virus.pdf", "MZ ejecutable".getBytes(StandardCharsets.UTF_8))
                .andExpect(status().isConflict());
        subir(acudiente, id, obligatorios.getFirst(), "../../tarjeta identidad.png", PNG)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentos[0].nombreArchivo").value("tarjeta identidad.png"))
                .andExpect(jsonPath("$.documentos[0].tipoMime").value("image/png"));
        for (long t : obligatorios.subList(1, obligatorios.size())) {
            subir(acudiente, id, t, "documento.pdf", PDF).andExpect(status().isOk());
        }

        // Sin autorizar el tratamiento de datos no se envia
        enviar(post("/api/solicitudes-matricula/" + id + "/enviar"), acudiente, "{\"autorizaDatos\": false}")
                .andExpect(status().isBadRequest());
        enviar(post("/api/solicitudes-matricula/" + id + "/enviar"), acudiente, "{\"autorizaDatos\": true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENVIADA"))
                .andExpect(jsonPath("$.autorizaDatos").value(true));
        enviar(put("/api/solicitudes-matricula/" + id), acudiente, jsonSolicitud(e, "NUEVO", documento, "Otra"))
                .andExpect(status().isConflict());

        // La secretaria la ve en su listado y rechaza la tarjeta de identidad
        MockHttpSession secretaria = iniciarSesion(crearUsuario("secretaria-matricula", Rol.SECRETARIA)
                .getNombreUsuario(), CONTRASENA);
        mockMvc.perform(get("/api/solicitudes-matricula?anioId=" + e.anioId()).session(secretaria))
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].estado").value("ENVIADA"))
                .andExpect(jsonPath("$.contenido[0].documentosPorRevisar").value(obligatorios.size()));
        JsonNode detalle = leer(mockMvc.perform(get("/api/solicitudes-matricula/" + id).session(secretaria))
                .andExpect(status().isOk()));
        long tarjeta = detalle.get("documentos").get(0).get("id").asLong();
        mockMvc.perform(get("/api/solicitudes-matricula/" + id + "/archivos/" + tarjeta).session(secretaria))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(PNG));
        enviar(put("/api/solicitudes-matricula/" + id + "/archivos/" + tarjeta + "/revision"), secretaria,
                "{\"estado\": \"RECHAZADO\"}")
                .andExpect(status().isConflict());
        enviar(put("/api/solicitudes-matricula/" + id + "/archivos/" + tarjeta + "/revision"), secretaria,
                "{\"estado\": \"RECHAZADO\", \"observacion\": \"La foto está borrosa\"}")
                .andExpect(status().isOk());
        enviar(post("/api/solicitudes-matricula/" + id + "/aprobar"), secretaria, "{}")
                .andExpect(status().isConflict());
        enviar(post("/api/solicitudes-matricula/" + id + "/devolver"), secretaria,
                "{\"comentario\": \"Vuelva a subir la tarjeta de identidad\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CON_OBSERVACIONES"));

        // El acudiente ve la observacion, reemplaza el archivo y vuelve a enviar
        mockMvc.perform(get("/api/solicitudes-matricula/" + id).session(acudiente))
                .andExpect(jsonPath("$.observacion").value("Vuelva a subir la tarjeta de identidad"))
                .andExpect(jsonPath("$.documentos[0].estado").value("RECHAZADO"))
                .andExpect(jsonPath("$.documentos[0].observacion").value("La foto está borrosa"));
        subir(acudiente, id, obligatorios.getFirst(), "tarjeta.pdf", PDF)
                .andExpect(jsonPath("$.documentos[0].estado").value("PENDIENTE"));
        enviar(post("/api/solicitudes-matricula/" + id + "/enviar"), acudiente, "{\"autorizaDatos\": true}")
                .andExpect(status().isOk());

        // Al aprobar queda el estudiante activo, matriculado en el grupo y con la madre como acudiente
        JsonNode aprobada = leer(enviar(post("/api/solicitudes-matricula/" + id + "/aprobar"), secretaria,
                "{\"grupoId\": %d}".formatted(e.grupoId())).andExpect(status().isOk()));
        assertThat(aprobada.get("estado").asText()).isEqualTo("APROBADA");
        assertThat(aprobada.get("revisiones")).hasSize(4);
        long estudiante = aprobada.get("estudianteId").asLong();
        mockMvc.perform(get("/api/estudiantes/" + estudiante).session(secretaria))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.nombres").value("Luna María"))
                .andExpect(jsonPath("$.direccion").value("Vereda Santa Lucía"))
                .andExpect(jsonPath("$.acudientes[0].parentesco").value("MADRE"))
                .andExpect(jsonPath("$.acudientes[0].principal").value(true))
                .andExpect(jsonPath("$.acudientes[0].accesoConsulta").value(true));
        mockMvc.perform(get("/api/grupos/" + e.grupoId() + "/estudiantes").session(secretaria))
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/solicitudes-matricula/mias").session(acudiente))
                .andExpect(jsonPath("$[0].estado").value("APROBADA"));
    }

    @Test
    void cadaAcudienteSoloVeYCambiaSusSolicitudes() throws Exception {
        Escenario e = crearEscenario(2062, "MATRICULA");
        MockHttpSession duena = registrarAcudiente(String.valueOf(DOCUMENTO.incrementAndGet()));
        MockHttpSession otra = registrarAcudiente(String.valueOf(DOCUMENTO.incrementAndGet()));
        long id = leer(enviar(post("/api/solicitudes-matricula"), duena,
                jsonSolicitud(e, "NUEVO", String.valueOf(DOCUMENTO.incrementAndGet()), "Sol"))).get("id").asLong();
        long tipo = tiposObligatorios().getFirst();
        long archivo = leer(subir(duena, id, tipo, "cedula.pdf", PDF)).get("documentos").get(0).get("id").asLong();

        mockMvc.perform(get("/api/solicitudes-matricula/" + id).session(otra)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/solicitudes-matricula/" + id + "/archivos/" + archivo).session(otra))
                .andExpect(status().isNotFound());
        subir(otra, id, tipo, "cambio.pdf", PDF).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/solicitudes-matricula/" + id + "/archivos/" + archivo).session(duena))
                .andExpect(status().isOk())
                .andExpect(content().bytes(PDF));

        // Un borrador no lo ve la secretaria, y el docente no entra al listado
        mockMvc.perform(get("/api/solicitudes-matricula/" + id).session(sesionAdmin)).andExpect(status().isNotFound());
        MockHttpSession docente = iniciarSesion(crearUsuario("docente-matricula", Rol.DOCENTE).getNombreUsuario(),
                CONTRASENA);
        mockMvc.perform(get("/api/solicitudes-matricula?anioId=" + e.anioId()).session(docente))
                .andExpect(status().isForbidden());
        // El acudiente no revisa solicitudes ni crea usuarios
        enviar(post("/api/solicitudes-matricula/" + id + "/aprobar"), duena, "{}").andExpect(status().isForbidden());
        mockMvc.perform(get("/api/usuarios").session(duena)).andExpect(status().isForbidden());
        // Las cuentas creadas desde el registro publico no ven datos internos del colegio
        mockMvc.perform(get("/api/grupos?anioId=" + e.anioId()).session(duena)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/docentes").session(duena)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/estudiantes/" + 1).session(duena)).andExpect(status().isForbidden());
    }

    @Test
    void validaElAnioLaCuentaYLasSolicitudesRepetidas() throws Exception {
        Escenario cerrada = crearEscenario(2063, null);
        Escenario abierta = crearEscenario(2064, "MATRICULA");
        String documentoAcudiente = String.valueOf(DOCUMENTO.incrementAndGet());
        MockHttpSession acudiente = registrarAcudiente(documentoAcudiente);

        mockMvc.perform(post("/api/auth/registro").with(tokenCsrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoDocumento": "CC", "numeroDocumento": "%s", "nombres": "Otra", "apellidos": "Persona",
                                 "telefono": "3100000000", "contrasena": "clave-otra-12"}
                                """.formatted(documentoAcudiente)))
                .andExpect(status().isConflict());

        String documento = String.valueOf(DOCUMENTO.incrementAndGet());
        enviar(post("/api/solicitudes-matricula"), acudiente, jsonSolicitud(cerrada, "NUEVO", documento, "Ana"))
                .andExpect(status().isConflict());
        enviar(post("/api/solicitudes-matricula"), acudiente,
                jsonSolicitud(abierta, "NUEVO", documentoAcudiente, "Ana"))
                .andExpect(status().isConflict());
        enviar(post("/api/solicitudes-matricula"), acudiente, jsonSolicitud(abierta, "NUEVO", documento, "Ana"))
                .andExpect(status().isCreated());
        enviar(post("/api/solicitudes-matricula"), acudiente, jsonSolicitud(abierta, "NUEVO", documento, "Ana"))
                .andExpect(status().isConflict());
    }

    @Test
    void alAprobarUnAntiguoSeActualizaSuFichaSinDuplicarla() throws Exception {
        Escenario e = crearEscenario(2065, "MATRICULA");
        String documento = String.valueOf(DOCUMENTO.incrementAndGet());
        long existente = leer(enviar(post("/api/estudiantes"), sesionAdmin, """
                {"tipoDocumento": "RC", "numeroDocumento": "%s", "nombres": "Luna", "apellidos": "Jojoa",
                 "codigo": "A-%s", "fechaNacimiento": "2015-04-02", "genero": "FEMENINO", "tienePiar": true}
                """.formatted(documento, documento)).andExpect(status().isCreated())).get("id").asLong();

        MockHttpSession acudiente = registrarAcudiente(String.valueOf(DOCUMENTO.incrementAndGet()));
        long id = solicitudEnviada(acudiente, e, "ANTIGUO", documento);
        enviar(post("/api/solicitudes-matricula/" + id + "/aprobar"), sesionAdmin, "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estudianteId").value(existente));

        // Pasa de registro civil a tarjeta de identidad y conserva el codigo y el PIAR
        mockMvc.perform(get("/api/estudiantes/" + existente).session(sesionAdmin))
                .andExpect(jsonPath("$.tipoDocumento").value("TI"))
                .andExpect(jsonPath("$.codigo").value("A-" + documento))
                .andExpect(jsonPath("$.tienePiar").value(true))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
        mockMvc.perform(get("/api/estudiantes/" + existente + "/matriculas").session(sesionAdmin))
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
