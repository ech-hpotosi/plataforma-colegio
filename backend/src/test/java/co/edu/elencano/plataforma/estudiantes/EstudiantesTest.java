package co.edu.elencano.plataforma.estudiantes;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.elencano.plataforma.PruebaIntegracion;
import co.edu.elencano.plataforma.usuarios.entidad.Rol;

class EstudiantesTest extends PruebaIntegracion {

    private static final AtomicInteger DOCUMENTO = new AtomicInteger(800000);

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

    private String jsonEstudiante(String documento, String nombres, String codigo) {
        return """
                {"tipoDocumento": "TI", "numeroDocumento": "%s", "nombres": "%s", "apellidos": "Jojoa Botina",
                 "codigo": %s, "fechaNacimiento": "2014-03-15", "genero": "FEMENINO", "eps": "Emssanar",
                 "tienePiar": false}
                """.formatted(documento, nombres, codigo == null ? "null" : "\"" + codigo + "\"");
    }

    private long crearEstudiante(String nombres) throws Exception {
        return leer(enviar(post("/api/estudiantes"), sesionAdmin,
                jsonEstudiante(String.valueOf(DOCUMENTO.incrementAndGet()), nombres, null))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    private String jsonAcudiente(String documento, String parentesco, boolean principal) {
        return """
                {"tipoDocumento": "CC", "numeroDocumento": "%s", "nombres": "Rosa", "apellidos": "Botina",
                 "telefono": "3101234567", "ocupacion": "Agricultora", "parentesco": "%s", "principal": %s}
                """.formatted(documento, parentesco, principal);
    }

    @Test
    void creaEstudianteYLoBuscaPorNombreYCodigo() throws Exception {
        String documento = String.valueOf(DOCUMENTO.incrementAndGet());
        enviar(post("/api/estudiantes"), sesionAdmin, jsonEstudiante(documento, "Valentina Unica", "E-9001"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ASPIRANTE"))
                .andExpect(jsonPath("$.acudientes", hasSize(0)));

        mockMvc.perform(get("/api/estudiantes?buscar=valentina unica").session(sesionAdmin))
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].numeroDocumento").value(documento));
        mockMvc.perform(get("/api/estudiantes?buscar=e-9001").session(sesionAdmin))
                .andExpect(jsonPath("$.totalElementos").value(1));

        enviar(post("/api/estudiantes"), sesionAdmin, jsonEstudiante(documento, "Otra", null))
                .andExpect(status().isConflict());
        enviar(post("/api/estudiantes"), sesionAdmin,
                jsonEstudiante(String.valueOf(DOCUMENTO.incrementAndGet()), "Otra", "E-9001"))
                .andExpect(status().isConflict());
    }

    @Test
    void elDocumentoDelEstudianteSePuedeCambiarDeTiACc() throws Exception {
        long id = crearEstudiante("Mayor Edad");
        String nuevo = String.valueOf(DOCUMENTO.incrementAndGet());
        enviar(put("/api/estudiantes/" + id), sesionAdmin,
                jsonEstudiante(nuevo, "Mayor Edad", null).replace("\"TI\"", "\"CC\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoDocumento").value("CC"))
                .andExpect(jsonPath("$.numeroDocumento").value(nuevo));
    }

    @Test
    void unAcudienteSeReutilizaEntreHermanosYSoloHayUnPrincipal() throws Exception {
        long hermano1 = crearEstudiante("Hermano Uno");
        long hermano2 = crearEstudiante("Hermano Dos");
        String documentoMadre = String.valueOf(DOCUMENTO.incrementAndGet());
        String documentoPadre = String.valueOf(DOCUMENTO.incrementAndGet());

        enviar(post("/api/estudiantes/" + hermano1 + "/acudientes"), sesionAdmin,
                jsonAcudiente(documentoMadre, "MADRE", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acudientes[0].principal").value(true));
        JsonNode conPadre = leer(enviar(post("/api/estudiantes/" + hermano1 + "/acudientes"), sesionAdmin,
                jsonAcudiente(documentoPadre, "PADRE", true)).andExpect(status().isOk()));
        org.junit.jupiter.api.Assertions.assertEquals("PADRE", conPadre.get("acudientes").get(0).get("parentesco").asText());
        org.junit.jupiter.api.Assertions.assertFalse(conPadre.get("acudientes").get(1).get("principal").asBoolean());

        // La misma madre en el segundo hermano reutiliza la persona
        long idMadre = conPadre.get("acudientes").get(1).get("id").asLong();
        enviar(post("/api/estudiantes/" + hermano2 + "/acudientes"), sesionAdmin,
                jsonAcudiente(documentoMadre, "MADRE", true))
                .andExpect(jsonPath("$.acudientes[0].id").value(idMadre));

        enviar(post("/api/estudiantes/" + hermano1 + "/acudientes"), sesionAdmin,
                jsonAcudiente(documentoMadre, "MADRE", false))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/personas/por-documento?tipo=CC&numero=" + documentoMadre).session(sesionAdmin))
                .andExpect(jsonPath("$.ocupacion").value("Agricultora"));

        // Al quitar al principal, el otro acudiente pasa a ser el principal
        long idPadre = conPadre.get("acudientes").get(0).get("id").asLong();
        mockMvc.perform(delete("/api/estudiantes/" + hermano1 + "/acudientes/" + idPadre)
                        .with(tokenCsrf()).session(sesionAdmin))
                .andExpect(jsonPath("$.acudientes", hasSize(1)))
                .andExpect(jsonPath("$.acudientes[0].principal").value(true));
    }

    @Test
    void matriculaAsignaGrupoRespetaCupoYPermiteRetirar() throws Exception {
        long sede = leer(enviar(post("/api/sedes"), sesionAdmin, "{\"nombre\": \"Sede Matricula\"}")).get("id").asLong();
        long anio = leer(enviar(post("/api/anios"), sesionAdmin, """
                {"anio": 2051, "fechaInicio": "2051-01-20", "fechaFin": "2051-11-30",
                 "periodos": [{"fechaInicio": "2051-01-20", "fechaFin": "2051-11-30", "porcentaje": 100}]}
                """)).get("id").asLong();
        long grado = leer(mockMvc.perform(get("/api/grados").session(sesionAdmin))).get(3).get("id").asLong();
        long grupo = leer(enviar(post("/api/grupos"), sesionAdmin, """
                {"anioLectivoId": %d, "sedeId": %d, "gradoId": %d, "nombre": "3-01", "jornada": "MANANA", "cupo": 1}
                """.formatted(anio, sede, grado))).get("id").asLong();
        long estudiante = crearEstudiante("Matriculado Uno");
        long otro = crearEstudiante("Matriculado Dos");

        long matricula = leer(enviar(post("/api/matriculas"), sesionAdmin,
                "{\"estudianteId\": %d, \"anioLectivoId\": %d, \"grupoId\": %d}".formatted(estudiante, anio, grupo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.grupo").value("Tercero 3-01"))).get("id").asLong();
        mockMvc.perform(get("/api/estudiantes/" + estudiante).session(sesionAdmin))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));

        enviar(post("/api/matriculas"), sesionAdmin,
                "{\"estudianteId\": %d, \"anioLectivoId\": %d}".formatted(estudiante, anio))
                .andExpect(status().isConflict());
        enviar(post("/api/matriculas"), sesionAdmin,
                "{\"estudianteId\": %d, \"anioLectivoId\": %d, \"grupoId\": %d}".formatted(otro, anio, grupo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El grupo Tercero 3-01 ya tiene el cupo completo (1)"));

        mockMvc.perform(get("/api/grupos/" + grupo + "/estudiantes").session(sesionAdmin))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].estudianteId").value(estudiante));

        enviar(post("/api/matriculas/" + matricula + "/retirar"), sesionAdmin, "{\"motivo\": \"Traslado de municipio\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RETIRADA"));
        mockMvc.perform(get("/api/grupos/" + grupo + "/estudiantes").session(sesionAdmin))
                .andExpect(jsonPath("$", hasSize(0)));

        // Con el cupo libre, el otro estudiante entra sin grupo y luego se le asigna
        long matriculaOtro = leer(enviar(post("/api/matriculas"), sesionAdmin,
                "{\"estudianteId\": %d, \"anioLectivoId\": %d}".formatted(otro, anio))).get("id").asLong();
        enviar(put("/api/matriculas/" + matriculaOtro + "/grupo"), sesionAdmin, "{\"grupoId\": %d}".formatted(grupo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grupoId").value(grupo));

        // El retirado vuelve el mismo anio: se reactiva su matricula
        enviar(post("/api/matriculas"), sesionAdmin,
                "{\"estudianteId\": %d, \"anioLectivoId\": %d}".formatted(estudiante, anio))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(matricula))
                .andExpect(jsonPath("$.estado").value("ACTIVA"));
    }

    @Test
    void unDocenteNoVeEstudiantesYLaCoordinadoraNoLosModifica() throws Exception {
        crearUsuario("docente.sin.acceso", Rol.DOCENTE);
        crearUsuario("coordinadora.lectura", Rol.COORDINADOR_ACADEMICO);
        MockHttpSession docente = iniciarSesion("docente.sin.acceso", CONTRASENA);
        MockHttpSession coordinadora = iniciarSesion("coordinadora.lectura", CONTRASENA);

        mockMvc.perform(get("/api/estudiantes").session(docente)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/estudiantes").session(coordinadora)).andExpect(status().isOk());
        enviar(post("/api/estudiantes"), coordinadora, jsonEstudiante("777777", "No Permitido", null))
                .andExpect(status().isForbidden());
    }

    @Test
    void laSecretariaCreaEstudiantes() throws Exception {
        crearUsuario("secretaria.crea", Rol.SECRETARIA);
        MockHttpSession secretaria = iniciarSesion("secretaria.crea", CONTRASENA);
        enviar(post("/api/estudiantes"), secretaria,
                jsonEstudiante(String.valueOf(DOCUMENTO.incrementAndGet()), "Por Secretaria", null))
                .andExpect(status().isCreated());
    }
}
