package co.edu.elencano.plataforma.familia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
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

class ConsultaFamiliaTest extends PruebaIntegracion {

    /** Documentos de esta clase de prueba, para no cruzarse con otras. */
    private static final AtomicInteger DOCUMENTO = new AtomicInteger(400000);

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

    private record Estudiante(long id, long matricula) {
    }

    /** Grupo de 2023 con una asignatura, dos periodos y dos estudiantes. */
    private record Escenario(long anio, long periodo1, long periodo2, long carga, Estudiante ana, Estudiante bruno) {
    }

    private Escenario crearEscenario() throws Exception {
        long sede = leer(enviar(post("/api/sedes"), sesionAdmin, "{\"nombre\": \"Sede Familia\"}")).get("id").asLong();
        JsonNode anio = leer(enviar(post("/api/anios"), sesionAdmin, """
                {"anio": 2023, "fechaInicio": "2023-01-20", "fechaFin": "2023-11-30",
                 "periodos": [{"fechaInicio": "2023-01-20", "fechaFin": "2023-06-15", "porcentaje": 50},
                              {"fechaInicio": "2023-06-16", "fechaFin": "2023-11-30", "porcentaje": 50}]}
                """).andExpect(status().isCreated()));
        long anioId = anio.get("id").asLong();
        long grado = leer(mockMvc.perform(get("/api/grados").session(sesionAdmin))).get(6).get("id").asLong();
        long area = leer(enviar(post("/api/areas"), sesionAdmin, "{\"nombre\": \"Area Familia\"}")).get("id").asLong();
        long asignatura = leer(enviar(post("/api/asignaturas"), sesionAdmin,
                "{\"areaId\": %d, \"nombre\": \"Ciencias Familia\"}".formatted(area))).get("id").asLong();
        enviar(put("/api/plan-estudio?anioId=%d&gradoId=%d".formatted(anioId, grado)), sesionAdmin,
                "{\"asignaturas\": [{\"asignaturaId\": %d, \"intensidadHoraria\": 3}]}".formatted(asignatura))
                .andExpect(status().isOk());
        long docente = crearUsuario("docente.familia", Rol.DOCENTE).getPersona().getId();
        enviar(put("/api/docentes/" + docente), sesionAdmin, "{\"sedeIds\": [%d]}".formatted(sede))
                .andExpect(status().isOk());
        long grupo = leer(enviar(post("/api/grupos"), sesionAdmin, """
                {"anioLectivoId": %d, "sedeId": %d, "gradoId": %d, "nombre": "01", "jornada": "MANANA",
                 "cupo": 30, "directorId": %d}
                """.formatted(anioId, sede, grado, docente)).andExpect(status().isCreated())).get("id").asLong();
        enviar(put("/api/grupos/" + grupo + "/carga"), sesionAdmin,
                "{\"asignaciones\": [{\"asignaturaId\": %d, \"docenteId\": %d}]}".formatted(asignatura, docente))
                .andExpect(status().isOk());
        long carga = -1;
        for (JsonNode c : leer(mockMvc.perform(get("/api/notas/cargas").session(sesionAdmin)))) {
            if (c.get("grupoId").asLong() == grupo) {
                carga = c.get("cargaId").asLong();
            }
        }
        return new Escenario(anioId, anio.get("periodos").get(0).get("id").asLong(),
                anio.get("periodos").get(1).get("id").asLong(), carga,
                matricular("Ana", anioId, grupo), matricular("Bruno", anioId, grupo));
    }

    private Estudiante matricular(String nombre, long anio, long grupo) throws Exception {
        long estudiante = leer(enviar(post("/api/estudiantes"), sesionAdmin, """
                {"tipoDocumento": "TI", "numeroDocumento": "%d", "nombres": "%s", "apellidos": "Familia",
                 "fechaNacimiento": "2008-05-10", "genero": "FEMENINO", "tienePiar": false}
                """.formatted(DOCUMENTO.incrementAndGet(), nombre))).get("id").asLong();
        long matricula = leer(enviar(post("/api/matriculas"), sesionAdmin,
                "{\"estudianteId\": %d, \"anioLectivoId\": %d, \"grupoId\": %d}".formatted(estudiante, anio, grupo))
                .andExpect(status().isCreated())).get("id").asLong();
        return new Estudiante(estudiante, matricula);
    }

    private MockHttpSession registrarAcudiente(String documento) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/api/auth/registro").with(tokenCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoDocumento": "CC", "numeroDocumento": "%s", "nombres": "Rosa", "apellidos": "Familia",
                                 "telefono": "3101234567", "correo": null, "contrasena": "clave-rosa-1"}
                                """.formatted(documento)))
                .andExpect(status().isCreated())
                .andReturn().getRequest().getSession(false);
    }

    private String jsonAcudiente(String documento, String nombres) {
        return """
                {"tipoDocumento": "CC", "numeroDocumento": "%s", "nombres": "%s", "apellidos": "Familia",
                 "parentesco": "MADRE", "principal": true}
                """.formatted(documento, nombres);
    }

    @Test
    void elAcudienteConPermisoConsultaNotasAsistenciaYBoletinDelPeriodoCerrado() throws Exception {
        Escenario e = crearEscenario();

        // Notas del primer periodo y una falta de Ana de dos horas
        long actividad = leer(enviar(post("/api/notas/cargas/%d/periodos/%d/actividades".formatted(e.carga(),
                e.periodo1())), sesionAdmin, "{\"dimension\": \"SABER\", \"nombre\": \"Evaluación\"}"))
                .get("actividades").get(0).get("id").asLong();
        enviar(put("/api/notas/cargas/%d/periodos/%d".formatted(e.carga(), e.periodo1())), sesionAdmin, """
                {"notas": [{"actividadId": %d, "matriculaId": %d, "valor": 4.0},
                           {"actividadId": %d, "matriculaId": %d, "valor": 2.0}]}
                """.formatted(actividad, e.ana().matricula(), actividad, e.bruno().matricula()))
                .andExpect(status().isOk());
        enviar(put("/api/asistencia/cargas/" + e.carga() + "?fecha=2023-03-04"), sesionAdmin, """
                {"horas": 2, "estudiantes": [{"matriculaId": %d, "estado": "FALTA"},
                                             {"matriculaId": %d, "estado": "ASISTIO"}]}
                """.formatted(e.ana().matricula(), e.bruno().matricula())).andExpect(status().isOk());

        // La madre crea su cuenta; mientras secretaria no le de el permiso, no ve a nadie
        String documentoMadre = String.valueOf(DOCUMENTO.incrementAndGet());
        MockHttpSession madre = registrarAcudiente(documentoMadre);
        mockMvc.perform(get("/api/familia/estudiantes").session(madre))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        enviar(post("/api/estudiantes/" + e.ana().id() + "/acudientes"), sesionAdmin,
                jsonAcudiente(documentoMadre, "Rosa")).andExpect(status().isOk());
        String resumenAna = "/api/familia/estudiantes/" + e.ana().id() + "/resumen";
        mockMvc.perform(get(resumenAna).session(madre)).andExpect(status().isNotFound());

        long madreId = leer(mockMvc.perform(get("/api/estudiantes/" + e.ana().id()).session(sesionAdmin)))
                .get("acudientes").get(0).get("id").asLong();
        enviar(put("/api/estudiantes/%d/acudientes/%d/acceso-consulta".formatted(e.ana().id(), madreId)), sesionAdmin,
                "{\"acceso\": true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acudientes[0].accesoConsulta").value(true));

        // Un acudiente sin cuenta no puede recibir el permiso
        enviar(post("/api/estudiantes/" + e.ana().id() + "/acudientes"), sesionAdmin,
                jsonAcudiente(String.valueOf(DOCUMENTO.incrementAndGet()), "Pedro").replace("true", "false"))
                .andExpect(status().isOk());
        long padreId = -1;
        for (JsonNode a : leer(mockMvc.perform(get("/api/estudiantes/" + e.ana().id()).session(sesionAdmin)))
                .get("acudientes")) {
            if (a.get("nombres").asText().equals("Pedro")) {
                padreId = a.get("id").asLong();
            }
        }
        enviar(put("/api/estudiantes/%d/acudientes/%d/acceso-consulta".formatted(e.ana().id(), padreId)), sesionAdmin,
                "{\"acceso\": true}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(containsString("todavía no tiene cuenta")));

        mockMvc.perform(get("/api/familia/estudiantes").session(madre))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombres").value("Ana"))
                .andExpect(jsonPath("$[0].parentesco").value("MADRE"))
                .andExpect(jsonPath("$[0].matriculas[0].anio").value(2023))
                .andExpect(jsonPath("$[0].matriculas[0].grupo").value("Sexto 01"));
        mockMvc.perform(get(resumenAna).session(madre))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estudiante").value("Ana Familia"))
                .andExpect(jsonPath("$.grupo").value("Sexto 01"))
                .andExpect(jsonPath("$.periodos", hasSize(2)))
                .andExpect(jsonPath("$.periodos[0].cerrado").value(false))
                .andExpect(jsonPath("$.asignaturas", hasSize(1)))
                .andExpect(jsonPath("$.asignaturas[0].nombre").value("Ciencias Familia"))
                .andExpect(jsonPath("$.asignaturas[0].periodos[0].nota").value(4.0))
                .andExpect(jsonPath("$.asignaturas[0].periodos[0].desempeno").value("ALTO"))
                .andExpect(jsonPath("$.asignaturas[0].periodos[1].nota").doesNotExist())
                .andExpect(jsonPath("$.asignaturas[0].faltasSinJustificar").value(2))
                .andExpect(jsonPath("$.novedades", hasSize(1)))
                .andExpect(jsonPath("$.novedades[0].estado").value("FALTA"))
                .andExpect(jsonPath("$.boletinDisponible").value(true));

        // Solo ve a su acudida; los funcionarios no usan esta consulta
        mockMvc.perform(get("/api/familia/estudiantes/" + e.bruno().id() + "/resumen").session(madre))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/familia/estudiantes").session(sesionAdmin)).andExpect(status().isForbidden());

        // El boletin sale cuando coordinacion cierra el periodo
        String boletin = "/api/familia/estudiantes/%d/boletin?matriculaId=%d&periodoId=%d"
                .formatted(e.ana().id(), e.ana().matricula(), e.periodo1());
        mockMvc.perform(get(boletin).session(madre))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(containsString("cuando el colegio cierre el periodo")));
        String cierre = "/api/anios/%d/periodos/%d/cierre".formatted(e.anio(), e.periodo1());
        enviar(put(cierre), iniciarSesion("docente.familia", CONTRASENA), "{\"cerrado\": true}")
                .andExpect(status().isForbidden());
        enviar(put(cierre), sesionAdmin, "{\"cerrado\": true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodos[0].cerrado").value(true))
                .andExpect(jsonPath("$.periodos[1].cerrado").value(false));
        byte[] pdf = mockMvc.perform(get(boletin).session(madre))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");

        // Con el periodo cerrado ya no se cambian sus notas
        enviar(put("/api/notas/cargas/%d/periodos/%d".formatted(e.carga(), e.periodo1())), sesionAdmin, """
                {"notas": [{"actividadId": %d, "matriculaId": %d, "valor": 5.0}]}
                """.formatted(actividad, e.ana().matricula()))
                .andExpect(status().isConflict());

        // Al quitar el permiso deja de verla
        enviar(put("/api/estudiantes/%d/acudientes/%d/acceso-consulta".formatted(e.ana().id(), madreId)), sesionAdmin,
                "{\"acceso\": false}").andExpect(status().isOk());
        mockMvc.perform(get(resumenAna).session(madre)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/familia/estudiantes").session(madre)).andExpect(jsonPath("$", hasSize(0)));
    }
}
