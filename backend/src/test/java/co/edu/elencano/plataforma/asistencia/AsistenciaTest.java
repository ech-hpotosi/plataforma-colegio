package co.edu.elencano.plataforma.asistencia;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
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

class AsistenciaTest extends PruebaIntegracion {

    private static final AtomicInteger DOCUMENTO = new AtomicInteger(900000);

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

    /** Datos de un grupo con una asignatura de 1 hora semanal: 40 horas al anio y maximo 6 sin justificar. */
    private record Escenario(long anio, long grupo, long carga, long matricula1, long matricula2) {
    }

    private Escenario crearEscenario(String prefijo, int anio, String fechaInicio, String fechaFin,
                                     String docente, String director) throws Exception {
        long sede = leer(enviar(post("/api/sedes"), sesionAdmin, "{\"nombre\": \"Sede " + prefijo + "\"}"))
                .get("id").asLong();
        long anioId = leer(enviar(post("/api/anios"), sesionAdmin, """
                {"anio": %d, "fechaInicio": "%s", "fechaFin": "%s",
                 "periodos": [{"fechaInicio": "%s", "fechaFin": "%s", "porcentaje": 100}]}
                """.formatted(anio, fechaInicio, fechaFin, fechaInicio, fechaFin))
                .andExpect(status().isCreated())).get("id").asLong();
        long grado = leer(mockMvc.perform(get("/api/grados").session(sesionAdmin))).get(6).get("id").asLong();
        long area = leer(enviar(post("/api/areas"), sesionAdmin, "{\"nombre\": \"Area " + prefijo + "\"}"))
                .get("id").asLong();
        long asignatura = leer(enviar(post("/api/asignaturas"), sesionAdmin,
                "{\"areaId\": %d, \"nombre\": \"Etica %s\"}".formatted(area, prefijo))).get("id").asLong();
        enviar(put("/api/plan-estudio?anioId=%d&gradoId=%d".formatted(anioId, grado)), sesionAdmin,
                "{\"asignaturas\": [{\"asignaturaId\": %d, \"intensidadHoraria\": 1}]}".formatted(asignatura))
                .andExpect(status().isOk());

        long docenteId = crearUsuario(docente, Rol.DOCENTE).getPersona().getId();
        long directorId = crearUsuario(director, Rol.DOCENTE).getPersona().getId();
        for (long id : new long[] {docenteId, directorId}) {
            enviar(put("/api/docentes/" + id), sesionAdmin, "{\"sedeIds\": [%d]}".formatted(sede))
                    .andExpect(status().isOk());
        }
        long grupo = leer(enviar(post("/api/grupos"), sesionAdmin, """
                {"anioLectivoId": %d, "sedeId": %d, "gradoId": %d, "nombre": "01", "jornada": "MANANA",
                 "cupo": 30, "directorId": %d}
                """.formatted(anioId, sede, grado, directorId)).andExpect(status().isCreated())).get("id").asLong();
        enviar(put("/api/grupos/" + grupo + "/carga"), sesionAdmin,
                "{\"asignaciones\": [{\"asignaturaId\": %d, \"docenteId\": %d}]}".formatted(asignatura, docenteId))
                .andExpect(status().isOk());
        long carga = -1;
        for (JsonNode c : leer(mockMvc.perform(get("/api/asistencia/cargas").session(sesionAdmin)))) {
            if (c.get("grupoId").asLong() == grupo) {
                carga = c.get("cargaId").asLong();
            }
        }
        return new Escenario(anioId, grupo, carga, matricular("Ana", anioId, grupo), matricular("Bruno", anioId, grupo));
    }

    private long matricular(String nombre, long anio, long grupo) throws Exception {
        long estudiante = leer(enviar(post("/api/estudiantes"), sesionAdmin, """
                {"tipoDocumento": "TI", "numeroDocumento": "%d", "nombres": "%s", "apellidos": "Asistencia",
                 "fechaNacimiento": "2012-05-10", "genero": "FEMENINO", "tienePiar": false}
                """.formatted(DOCUMENTO.incrementAndGet(), nombre))).get("id").asLong();
        return leer(enviar(post("/api/matriculas"), sesionAdmin,
                "{\"estudianteId\": %d, \"anioLectivoId\": %d, \"grupoId\": %d}".formatted(estudiante, anio, grupo))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    private String jsonClase(int horas, long matricula1, String estado1, long matricula2, String estado2) {
        return """
                {"horas": %d, "estudiantes": [{"matriculaId": %d, "estado": "%s"},
                                              {"matriculaId": %d, "estado": "%s", "observacion": "Llego tarde"}]}
                """.formatted(horas, matricula1, estado1, matricula2, estado2);
    }

    @Test
    void elDocenteTomaAsistenciaSoloDeSusClasesYDentroDeLasReglas() throws Exception {
        Escenario e = crearEscenario("Asis1", 2021, "2021-01-18", "2021-11-30", "docente.asis1", "director.asis1");
        MockHttpSession docente = iniciarSesion("docente.asis1", CONTRASENA);
        MockHttpSession otroDocente = iniciarSesion("director.asis1", CONTRASENA);

        mockMvc.perform(get("/api/asistencia/cargas").session(docente))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].cargaId").value(e.carga()))
                .andExpect(jsonPath("$[0].asignatura").value("Etica Asis1"));
        mockMvc.perform(get("/api/asistencia/cargas").session(otroDocente))
                .andExpect(jsonPath("$", hasSize(0)));

        String url = "/api/asistencia/cargas/" + e.carga() + "?fecha=2021-03-01";
        mockMvc.perform(get(url).session(docente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrada").value(false))
                .andExpect(jsonPath("$.periodo").value(1))
                .andExpect(jsonPath("$.estudiantes", hasSize(2)))
                .andExpect(jsonPath("$.estudiantes[0].estado").value("ASISTIO"));

        enviar(put(url), docente, jsonClase(2, e.matricula1(), "FALTA", e.matricula2(), "RETARDO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrada").value(true))
                .andExpect(jsonPath("$.horas").value(2))
                .andExpect(jsonPath("$.estudiantes[0].estado").value("FALTA"))
                .andExpect(jsonPath("$.estudiantes[1].observacion").value("Llego tarde"));

        // Otro docente no puede ver ni tomar asistencia de una clase que no es suya
        mockMvc.perform(get(url).session(otroDocente)).andExpect(status().isForbidden());
        enviar(put(url), otroDocente, jsonClase(1, e.matricula1(), "ASISTIO", e.matricula2(), "ASISTIO"))
                .andExpect(status().isForbidden());

        // Fecha futura, fuera de los periodos, o marcar directamente una falta como justificada
        String manana = LocalDate.now().plusDays(1).toString();
        enviar(put("/api/asistencia/cargas/" + e.carga() + "?fecha=" + manana), sesionAdmin,
                jsonClase(1, e.matricula1(), "FALTA", e.matricula2(), "ASISTIO"))
                .andExpect(status().isConflict());
        enviar(put("/api/asistencia/cargas/" + e.carga() + "?fecha=2021-12-10"), docente,
                jsonClase(1, e.matricula1(), "FALTA", e.matricula2(), "ASISTIO"))
                .andExpect(status().isConflict());
        enviar(put(url), docente, jsonClase(2, e.matricula1(), "FALTA_JUSTIFICADA", e.matricula2(), "ASISTIO"))
                .andExpect(status().isConflict());
        enviar(put(url), docente, jsonClase(0, e.matricula1(), "FALTA", e.matricula2(), "ASISTIO"))
                .andExpect(status().isBadRequest());

        // Un estudiante que no es del grupo no se acepta
        enviar(put(url), docente, jsonClase(2, e.matricula1(), "FALTA", 999999, "ASISTIO"))
                .andExpect(status().isConflict());
    }

    @Test
    void elConsolidadoAlertaPasadoElQuinceporcientoYLaJustificacionTienePlazo() throws Exception {
        Escenario e = crearEscenario("Asis2", 2022, "2022-01-17", "2022-11-30", "docente.asis2", "director.asis2");
        MockHttpSession docente = iniciarSesion("docente.asis2", CONTRASENA);
        MockHttpSession director = iniciarSesion("director.asis2", CONTRASENA);
        String base = "/api/asistencia/cargas/" + e.carga() + "?fecha=";

        // 2 + 2 + 3 = 7 horas sin justificar de 40 al anio: 17.5 %, pasa el 15 %
        enviar(put(base + "2022-03-07"), docente, jsonClase(2, e.matricula1(), "FALTA", e.matricula2(), "RETARDO"))
                .andExpect(status().isOk());
        enviar(put(base + "2022-03-08"), docente, jsonClase(2, e.matricula1(), "FALTA", e.matricula2(), "PERMISO"))
                .andExpect(status().isOk());
        enviar(put(base + "2022-03-11"), docente, jsonClase(3, e.matricula1(), "FALTA", e.matricula2(), "ASISTIO"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/asistencia/grupos").session(director))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].grupoId").value(e.grupo()));
        mockMvc.perform(get("/api/asistencia/grupos").session(docente)).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/asistencia/grupos/" + e.grupo() + "/resumen").session(docente))
                .andExpect(status().isForbidden());

        String resumen = "/api/asistencia/grupos/" + e.grupo() + "/resumen";
        mockMvc.perform(get(resumen).session(director))
                .andExpect(jsonPath("$.asignaturas[0].horasAnuales").value(40))
                .andExpect(jsonPath("$.estudiantes[0].nombres").value("Ana"))
                .andExpect(jsonPath("$.estudiantes[0].superaLimite").value(true))
                .andExpect(jsonPath("$.estudiantes[0].asignaturas[0].horasSinJustificar").value(7))
                .andExpect(jsonPath("$.estudiantes[0].asignaturas[0].porcentaje").value(17.5))
                .andExpect(jsonPath("$.estudiantes[1].superaLimite").value(false))
                .andExpect(jsonPath("$.estudiantes[1].asignaturas[0].horasJustificadas").value(2))
                .andExpect(jsonPath("$.estudiantes[1].asignaturas[0].horasRetardo").value(2));

        // En el inicio el director ve a Ana en riesgo; sus faltas de 2022 ya no estan en plazo
        mockMvc.perform(get("/api/asistencia/pendientes").session(director))
                .andExpect(jsonPath("$.estudiantesEnRiesgo", hasSize(1)))
                .andExpect(jsonPath("$.estudiantesEnRiesgo[0].matriculaId").value(e.matricula1()))
                .andExpect(jsonPath("$.estudiantesEnRiesgo[0].asignatura").value("Etica Asis2"))
                .andExpect(jsonPath("$.estudiantesEnRiesgo[0].superaLimite").value(true))
                .andExpect(jsonPath("$.faltasPorJustificar", hasSize(0)));

        // El plazo es de 3 dias habiles: del viernes 11 de marzo al miercoles 16
        mockMvc.perform(get("/api/asistencia/matriculas/" + e.matricula1() + "/novedades").session(director))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].fecha").value("2022-03-11"))
                .andExpect(jsonPath("$[0].fechaLimite").value("2022-03-16"));

        // Vencido el plazo, el director ya no puede justificar; el coordinador o el administrador si
        String justificar = "/api/asistencia/matriculas/" + e.matricula1() + "/justificacion";
        String cita = "{\"fecha\": \"2022-03-11\", \"justificacion\": \"Cita medica, presento incapacidad\"}";
        enviar(post(justificar), director, cita).andExpect(status().isConflict());
        enviar(post(justificar), sesionAdmin, cita)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("FALTA_JUSTIFICADA"));
        enviar(post(justificar), sesionAdmin, cita).andExpect(status().isConflict());

        // Si el docente vuelve a guardar la clase con la falta, la justificacion se conserva
        enviar(put(base + "2022-03-11"), docente, jsonClase(3, e.matricula1(), "FALTA", e.matricula2(), "ASISTIO"))
                .andExpect(jsonPath("$.estudiantes[0].estado").value("FALTA_JUSTIFICADA"))
                .andExpect(jsonPath("$.estudiantes[0].justificacion").value("Cita medica, presento incapacidad"));

        mockMvc.perform(get(resumen).session(director))
                .andExpect(jsonPath("$.estudiantes[0].superaLimite").value(false))
                .andExpect(jsonPath("$.estudiantes[0].asignaturas[0].horasSinJustificar").value(4))
                .andExpect(jsonPath("$.estudiantes[0].asignaturas[0].horasJustificadas").value(3));

        // Con 4 horas (10 %) sigue en la lista como cercana al limite: dos tercios del 15 %
        mockMvc.perform(get("/api/asistencia/pendientes").session(director))
                .andExpect(jsonPath("$.estudiantesEnRiesgo", hasSize(1)))
                .andExpect(jsonPath("$.estudiantesEnRiesgo[0].porcentaje").value(10.0))
                .andExpect(jsonPath("$.estudiantesEnRiesgo[0].superaLimite").value(false));
    }

    @Test
    void elDirectorJustificaDentroDelPlazo() throws Exception {
        LocalDate hoy = LocalDate.now();
        int anio = hoy.getYear();
        Escenario e = crearEscenario("Asis3", anio, anio + "-01-01", anio + "-12-31", "docente.asis3", "director.asis3");
        MockHttpSession docente = iniciarSesion("docente.asis3", CONTRASENA);
        MockHttpSession director = iniciarSesion("director.asis3", CONTRASENA);
        MockHttpSession rector = iniciarSesion(crearUsuario("rector.asis3", Rol.RECTOR).getNombreUsuario(), CONTRASENA);

        boolean finDeSemana = hoy.getDayOfWeek() == DayOfWeek.SATURDAY || hoy.getDayOfWeek() == DayOfWeek.SUNDAY;
        if (!finDeSemana) {
            mockMvc.perform(get("/api/asistencia/pendientes").session(docente))
                    .andExpect(jsonPath("$.clasesHoy", hasSize(1)))
                    .andExpect(jsonPath("$.clasesHoy[0].cargaId").value(e.carga()))
                    .andExpect(jsonPath("$.clasesHoy[0].registrada").value(false));
        }

        enviar(put("/api/asistencia/cargas/" + e.carga() + "?fecha=" + hoy), docente,
                jsonClase(1, e.matricula1(), "ASISTIO", e.matricula2(), "FALTA"))
                .andExpect(status().isOk());

        // Pendientes del inicio: el docente ve su clase registrada; el director, la falta por justificar
        mockMvc.perform(get("/api/asistencia/pendientes").session(docente))
                .andExpect(jsonPath("$.clasesHoy", hasSize(finDeSemana ? 0 : 1)))
                .andExpect(jsonPath("$.faltasPorJustificar", hasSize(0)))
                .andExpect(jsonPath("$.estudiantesEnRiesgo", hasSize(0)));
        if (!finDeSemana) {
            mockMvc.perform(get("/api/asistencia/pendientes").session(docente))
                    .andExpect(jsonPath("$.clasesHoy[0].registrada").value(true));
        }
        mockMvc.perform(get("/api/asistencia/pendientes").session(director))
                .andExpect(jsonPath("$.faltasPorJustificar", hasSize(1)))
                .andExpect(jsonPath("$.faltasPorJustificar[0].matriculaId").value(e.matricula2()))
                .andExpect(jsonPath("$.faltasPorJustificar[0].grupoId").value(e.grupo()))
                .andExpect(jsonPath("$.faltasPorJustificar[0].horas").value(1))
                .andExpect(jsonPath("$.estudiantesEnRiesgo", hasSize(0)));
        mockMvc.perform(get("/api/asistencia/pendientes").session(rector))
                .andExpect(jsonPath("$.faltasPorJustificar", hasSize(0)));

        String justificar = "/api/asistencia/matriculas/" + e.matricula2() + "/justificacion";
        String json = "{\"fecha\": \"%s\", \"justificacion\": \"Calamidad familiar\"}".formatted(hoy);
        enviar(post(justificar), docente, json).andExpect(status().isForbidden());
        enviar(post(justificar), rector, json).andExpect(status().isForbidden());
        enviar(post(justificar), director, json)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("FALTA_JUSTIFICADA"))
                .andExpect(jsonPath("$[0].justificacion").value("Calamidad familiar"));

        mockMvc.perform(get("/api/asistencia/pendientes").session(director))
                .andExpect(jsonPath("$.faltasPorJustificar", hasSize(0)));

        // El rector puede consultar el consolidado de cualquier grupo
        mockMvc.perform(get("/api/asistencia/grupos/" + e.grupo() + "/resumen").session(rector))
                .andExpect(status().isOk());
    }
}
