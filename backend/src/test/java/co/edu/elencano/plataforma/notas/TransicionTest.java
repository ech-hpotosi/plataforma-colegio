package co.edu.elencano.plataforma.notas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
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
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

import co.edu.elencano.plataforma.PruebaIntegracion;
import co.edu.elencano.plataforma.usuarios.entidad.Rol;

/** Transicion se evalua con informes descriptivos por dimension, sin notas (SIEE 4.3.1). */
class TransicionTest extends PruebaIntegracion {

    private static final AtomicInteger DOCUMENTO = new AtomicInteger(300000);

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

    private String informe(long matricula, String comportamiento, String texto) {
        return """
                {"estudiantes": [{"matriculaId": %d, "comportamiento": %s, "observacion": "%s"}]}
                """.formatted(matricula, comportamiento, texto);
    }

    @Test
    void elDocenteEscribeElInformeDescriptivoYSaleEnElBoletinYEnLaConsultaDeLaFamilia() throws Exception {
        long sede = leer(enviar(post("/api/sedes"), sesionAdmin, "{\"nombre\": \"Sede Transicion\"}")).get("id").asLong();
        JsonNode anio = leer(enviar(post("/api/anios"), sesionAdmin, """
                {"anio": 2041, "fechaInicio": "2041-01-20", "fechaFin": "2041-11-30",
                 "periodos": [{"fechaInicio": "2041-01-20", "fechaFin": "2041-06-15", "porcentaje": 50},
                              {"fechaInicio": "2041-06-16", "fechaFin": "2041-11-30", "porcentaje": 50}]}
                """).andExpect(status().isCreated()));
        long anioId = anio.get("id").asLong();
        long periodo1 = anio.get("periodos").get(0).get("id").asLong();
        JsonNode transicion = leer(mockMvc.perform(get("/api/grados").session(sesionAdmin))).get(0);
        assertThat(transicion.get("evaluacionCualitativa").asBoolean()).isTrue();
        long grado = transicion.get("id").asLong();
        long area = leer(enviar(post("/api/areas"), sesionAdmin, "{\"nombre\": \"Dimensiones del desarrollo\"}"))
                .get("id").asLong();
        List<Long> dimensiones = new ArrayList<>();
        for (String nombre : List.of("Dimensión comunicativa", "Dimensión corporal")) {
            dimensiones.add(leer(enviar(post("/api/asignaturas"), sesionAdmin,
                    "{\"areaId\": %d, \"nombre\": \"%s\"}".formatted(area, nombre))).get("id").asLong());
        }
        enviar(put("/api/plan-estudio?anioId=%d&gradoId=%d".formatted(anioId, grado)), sesionAdmin,
                "{\"asignaturas\": [{\"asignaturaId\": %d, \"intensidadHoraria\": 5}, {\"asignaturaId\": %d, \"intensidadHoraria\": 3}]}"
                        .formatted(dimensiones.get(0), dimensiones.get(1)))
                .andExpect(status().isOk());
        long docente = crearUsuario("docente.transicion", Rol.DOCENTE).getPersona().getId();
        crearUsuario("otro.transicion", Rol.DOCENTE);
        enviar(put("/api/docentes/" + docente), sesionAdmin, "{\"sedeIds\": [%d]}".formatted(sede))
                .andExpect(status().isOk());
        long grupo = leer(enviar(post("/api/grupos"), sesionAdmin, """
                {"anioLectivoId": %d, "sedeId": %d, "gradoId": %d, "nombre": "01", "jornada": "MANANA",
                 "cupo": 25, "directorId": %d}
                """.formatted(anioId, sede, grado, docente)).andExpect(status().isCreated())).get("id").asLong();
        enviar(put("/api/grupos/" + grupo + "/carga"), sesionAdmin,
                "{\"asignaciones\": [{\"asignaturaId\": %d, \"docenteId\": %d}, {\"asignaturaId\": %d, \"docenteId\": %d}]}"
                        .formatted(dimensiones.get(0), docente, dimensiones.get(1), docente))
                .andExpect(status().isOk());
        String documento = String.valueOf(DOCUMENTO.incrementAndGet());
        long estudiante = leer(enviar(post("/api/estudiantes"), sesionAdmin, """
                {"tipoDocumento": "RC", "numeroDocumento": "%s", "nombres": "Sara", "apellidos": "Transición",
                 "fechaNacimiento": "2020-02-10", "genero": "FEMENINO", "tienePiar": false}
                """.formatted(documento))).get("id").asLong();
        long matricula = leer(enviar(post("/api/matriculas"), sesionAdmin,
                "{\"estudianteId\": %d, \"anioLectivoId\": %d, \"grupoId\": %d}".formatted(estudiante, anioId, grupo))
                .andExpect(status().isCreated())).get("id").asLong();

        MockHttpSession sesionDocente = iniciarSesion("docente.transicion", CONTRASENA);
        long comunicativa = -1;
        for (JsonNode c : leer(mockMvc.perform(get("/api/notas/cargas").session(sesionDocente)))) {
            assertThat(c.get("cualitativa").asBoolean()).isTrue();
            if (c.get("asignatura").asText().equals("Dimensión comunicativa")) {
                comunicativa = c.get("cargaId").asLong();
            }
        }
        String url = "/api/notas/cargas/%d/periodos/%d".formatted(comunicativa, periodo1);
        // No hay planilla de notas, pero si informe
        mockMvc.perform(get(url).session(sesionDocente)).andExpect(status().isConflict());
        mockMvc.perform(get(url + "/informe").session(sesionDocente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cualitativo").value(true))
                .andExpect(jsonPath("$.grupo").value("Transición 01"))
                .andExpect(jsonPath("$.estudiantes", hasSize(1)))
                .andExpect(jsonPath("$.estudiantes[0].notaDefinitiva").doesNotExist());
        mockMvc.perform(get(url + "/informe").session(iniciarSesion("otro.transicion", CONTRASENA)))
                .andExpect(status().isForbidden());

        String texto = "Sara se expresa con seguridad frente al grupo y narra con detalle lo que vive en casa. "
                + "Reconoce las vocales y escribe su nombre. Se recomienda leerle cuentos en casa cada noche.";
        enviar(put(url + "/informe"), sesionDocente, informe(matricula, "4.0", texto))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(containsString("cualitativa")));
        enviar(put(url + "/informe"), sesionDocente, informe(matricula, "null", "x".repeat(1501)))
                .andExpect(status().isBadRequest());
        enviar(put(url + "/informe"), sesionDocente, informe(matricula, "null", texto + " " + "a".repeat(1000)))
                .andExpect(status().isOk());
        enviar(put(url + "/informe"), sesionDocente, informe(matricula, "null", texto))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estudiantes[0].observacion").value(texto));

        // Boletin descriptivo del periodo, con el informe y sin notas
        byte[] pdf = mockMvc.perform(get("/api/notas/grupos/%d/periodos/%d/boletines?matriculaId=%d"
                        .formatted(grupo, periodo1, matricula)).session(sesionDocente))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        String contenido = new PdfTextExtractor(new PdfReader(pdf)).getTextFromPage(1);
        assertThat(contenido).contains("Informe descriptivo de desarrollo", "Dimensión comunicativa",
                "escribe su nombre", "Sin informe registrado", "no se reprueban grados");

        // La familia ve el informe cuando el periodo cierra
        String documentoMadre = String.valueOf(DOCUMENTO.incrementAndGet());
        MockHttpSession madre = (MockHttpSession) mockMvc.perform(post("/api/auth/registro").with(tokenCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoDocumento": "CC", "numeroDocumento": "%s", "nombres": "Rosa", "apellidos": "Transición",
                                 "telefono": "3101234567", "correo": null, "contrasena": "clave-rosa-1"}
                                """.formatted(documentoMadre)))
                .andExpect(status().isCreated())
                .andReturn().getRequest().getSession(false);
        enviar(post("/api/estudiantes/" + estudiante + "/acudientes"), sesionAdmin, """
                {"tipoDocumento": "CC", "numeroDocumento": "%s", "nombres": "Rosa", "apellidos": "Transición",
                 "parentesco": "MADRE", "principal": true}
                """.formatted(documentoMadre)).andExpect(status().isOk());
        long madreId = leer(mockMvc.perform(get("/api/estudiantes/" + estudiante).session(sesionAdmin)))
                .get("acudientes").get(0).get("id").asLong();
        enviar(put("/api/estudiantes/%d/acudientes/%d/acceso-consulta".formatted(estudiante, madreId)), sesionAdmin,
                "{\"acceso\": true}").andExpect(status().isOk());
        String resumen = "/api/familia/estudiantes/" + estudiante + "/resumen";
        mockMvc.perform(get(resumen).session(madre))
                .andExpect(jsonPath("$.cualitativo").value(true))
                .andExpect(jsonPath("$.boletinDisponible").value(true))
                .andExpect(jsonPath("$.asignaturas[0].nombre").value("Dimensión comunicativa"))
                .andExpect(jsonPath("$.asignaturas[0].periodos[0].informe").doesNotExist());
        enviar(put("/api/anios/%d/periodos/%d/cierre".formatted(anioId, periodo1)), sesionAdmin, "{\"cerrado\": true}")
                .andExpect(status().isOk());
        mockMvc.perform(get(resumen).session(madre))
                .andExpect(jsonPath("$.asignaturas[0].periodos[0].informe").value(texto))
                .andExpect(jsonPath("$.asignaturas[0].periodos[0].nota").doesNotExist());
        // Con el periodo cerrado el docente ya no cambia el informe
        enviar(put(url + "/informe"), sesionDocente, informe(matricula, "null", "Otro texto"))
                .andExpect(status().isConflict());
    }
}
