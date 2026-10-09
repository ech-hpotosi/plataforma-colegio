package co.edu.elencano.plataforma.notas;

import static org.hamcrest.Matchers.hasItem;
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

class NotasTest extends PruebaIntegracion {

    private static final AtomicInteger DOCUMENTO = new AtomicInteger(700000);

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

    /** Grupo con una asignatura, dos periodos del 50 % y dos estudiantes. */
    private record Escenario(long anio, long periodo1, long periodo2, long grupo, long carga, long ana, long bruno) {
    }

    private Escenario crearEscenario(String prefijo, int anio, String docente, String director) throws Exception {
        long sede = leer(enviar(post("/api/sedes"), sesionAdmin, "{\"nombre\": \"Sede " + prefijo + "\"}"))
                .get("id").asLong();
        JsonNode anioJson = leer(enviar(post("/api/anios"), sesionAdmin, """
                {"anio": %d, "fechaInicio": "%d-01-20", "fechaFin": "%d-11-30",
                 "periodos": [{"fechaInicio": "%d-01-20", "fechaFin": "%d-06-15", "porcentaje": 50},
                              {"fechaInicio": "%d-06-16", "fechaFin": "%d-11-30", "porcentaje": 50}]}
                """.formatted(anio, anio, anio, anio, anio, anio, anio)).andExpect(status().isCreated()));
        long anioId = anioJson.get("id").asLong();
        long grado = leer(mockMvc.perform(get("/api/grados").session(sesionAdmin))).get(6).get("id").asLong();
        long area = leer(enviar(post("/api/areas"), sesionAdmin, "{\"nombre\": \"Area " + prefijo + "\"}"))
                .get("id").asLong();
        long asignatura = leer(enviar(post("/api/asignaturas"), sesionAdmin,
                "{\"areaId\": %d, \"nombre\": \"Ciencias %s\"}".formatted(area, prefijo))).get("id").asLong();
        enviar(put("/api/plan-estudio?anioId=%d&gradoId=%d".formatted(anioId, grado)), sesionAdmin,
                "{\"asignaturas\": [{\"asignaturaId\": %d, \"intensidadHoraria\": 3}]}".formatted(asignatura))
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
        for (JsonNode c : leer(mockMvc.perform(get("/api/notas/cargas").session(sesionAdmin)))) {
            if (c.get("grupoId").asLong() == grupo) {
                carga = c.get("cargaId").asLong();
            }
        }
        return new Escenario(anioId, anioJson.get("periodos").get(0).get("id").asLong(),
                anioJson.get("periodos").get(1).get("id").asLong(), grupo, carga,
                matricular("Ana", anioId, grupo), matricular("Bruno", anioId, grupo));
    }

    private long matricular(String nombre, long anio, long grupo) throws Exception {
        long estudiante = leer(enviar(post("/api/estudiantes"), sesionAdmin, """
                {"tipoDocumento": "TI", "numeroDocumento": "%d", "nombres": "%s", "apellidos": "Notas",
                 "fechaNacimiento": "2012-05-10", "genero": "FEMENINO", "tienePiar": false}
                """.formatted(DOCUMENTO.incrementAndGet(), nombre))).get("id").asLong();
        return leer(enviar(post("/api/matriculas"), sesionAdmin,
                "{\"estudianteId\": %d, \"anioLectivoId\": %d, \"grupoId\": %d}".formatted(estudiante, anio, grupo))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    private long crearActividad(MockHttpSession sesion, Escenario e, long periodo, String dimension, String nombre)
            throws Exception {
        JsonNode planilla = leer(enviar(post("/api/notas/cargas/%d/periodos/%d/actividades".formatted(e.carga(), periodo)),
                sesion, "{\"dimension\": \"%s\", \"nombre\": \"%s\"}".formatted(dimension, nombre))
                .andExpect(status().isOk()));
        JsonNode actividades = planilla.get("actividades");
        return actividades.get(actividades.size() - 1).get("id").asLong();
    }

    private String nota(long actividad, long matricula, String valor) {
        return "{\"actividadId\": %d, \"matriculaId\": %d, \"valor\": %s}".formatted(actividad, matricula, valor);
    }

    @Test
    void elDocenteRegistraNotasYSeCalculaLaNotaDelPeriodo() throws Exception {
        Escenario e = crearEscenario("Notas1", 2038, "docente.notas1", "director.notas1");
        MockHttpSession docente = iniciarSesion("docente.notas1", CONTRASENA);
        MockHttpSession otro = iniciarSesion("director.notas1", CONTRASENA);
        String planilla = "/api/notas/cargas/%d/periodos/%d".formatted(e.carga(), e.periodo1());

        mockMvc.perform(get("/api/notas/cargas").session(docente))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].periodos", hasSize(2)))
                .andExpect(jsonPath("$[0].cualitativa").value(false));

        long evaluacion = crearActividad(docente, e, e.periodo1(), "SABER", "Evaluación célula");
        long taller = crearActividad(docente, e, e.periodo1(), "SABER", "Taller 1");
        long exposicion = crearActividad(docente, e, e.periodo1(), "HACER", "Exposición");
        long actitud = crearActividad(docente, e, e.periodo1(), "SER", "Actitud en clase");

        // Ana: Saber (4.0 + 3.0) / 2 = 3.5, Hacer 4.0, Ser 5.0 -> (140 + 160 + 100) / 100 = 4.0 Alto
        // Bruno: solo Saber 2.0 -> nota parcial 2.0 Bajo
        enviar(put(planilla), docente, "{\"notas\": [%s, %s, %s, %s, %s]}".formatted(
                nota(evaluacion, e.ana(), "4.0"), nota(taller, e.ana(), "3.0"), nota(exposicion, e.ana(), "4.0"),
                nota(actitud, e.ana(), "5.0"), nota(evaluacion, e.bruno(), "2.0")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actividades", hasSize(4)))
                .andExpect(jsonPath("$.estudiantes[0].nombres").value("Ana"))
                .andExpect(jsonPath("$.estudiantes[0].saber").value(3.5))
                .andExpect(jsonPath("$.estudiantes[0].notaPeriodo").value(4.0))
                .andExpect(jsonPath("$.estudiantes[0].desempeno").value("ALTO"))
                .andExpect(jsonPath("$.estudiantes[0].completa").value(true))
                .andExpect(jsonPath("$.estudiantes[1].notaPeriodo").value(2.0))
                .andExpect(jsonPath("$.estudiantes[1].desempeno").value("BAJO"))
                .andExpect(jsonPath("$.estudiantes[1].completa").value(false));

        // Con la evaluacion al 75 % del Saber, el taller sin porcentaje queda con el 25 %:
        // Saber (4.0 * 75 + 3.0 * 25) / 100 = 3.8 y la nota sube a 4.1
        enviar(put("/api/notas/actividades/" + evaluacion), docente,
                "{\"dimension\": \"SABER\", \"nombre\": \"Evaluación célula\", \"porcentaje\": 75}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actividades[0].porcentaje").value(75))
                .andExpect(jsonPath("$.actividades[0].porcentajeEfectivo").value(75))
                .andExpect(jsonPath("$.actividades[?(@.nombre == 'Taller 1')].porcentajeEfectivo", hasItem(25.0)))
                .andExpect(jsonPath("$.estudiantes[0].saber").value(3.8))
                .andExpect(jsonPath("$.estudiantes[0].notaPeriodo").value(4.1));
        enviar(put("/api/notas/actividades/" + evaluacion), docente,
                "{\"dimension\": \"SABER\", \"nombre\": \"Evaluación célula\", \"porcentaje\": 101}")
                .andExpect(status().isBadRequest());
        // Con el taller sin porcentaje, la evaluacion no puede quedarse con el 100 %; ni sumar mas de 100 %
        enviar(put("/api/notas/actividades/" + evaluacion), docente,
                "{\"dimension\": \"SABER\", \"nombre\": \"Evaluación célula\", \"porcentaje\": 100}")
                .andExpect(status().isConflict());
        enviar(post("/api/notas/cargas/%d/periodos/%d/actividades".formatted(e.carga(), e.periodo1())), docente,
                "{\"dimension\": \"SABER\", \"nombre\": \"Quiz\", \"porcentaje\": 30}")
                .andExpect(status().isConflict());

        // Fuera de escala, mas de una decimal, o de otro docente
        enviar(put(planilla), docente, "{\"notas\": [%s]}".formatted(nota(taller, e.bruno(), "5.5")))
                .andExpect(status().isConflict());
        enviar(put(planilla), docente, "{\"notas\": [%s]}".formatted(nota(taller, e.bruno(), "3.25")))
                .andExpect(status().isConflict());
        mockMvc.perform(get(planilla).session(otro)).andExpect(status().isForbidden());
        enviar(put(planilla), otro, "{\"notas\": [%s]}".formatted(nota(taller, e.bruno(), "3.0")))
                .andExpect(status().isForbidden());

        // Un valor nulo borra la nota; borrar la actividad de Ser deja la nota de Ana parcial
        enviar(put(planilla), docente, "{\"notas\": [%s]}".formatted(nota(evaluacion, e.bruno(), "null")))
                .andExpect(jsonPath("$.estudiantes[1].notaPeriodo").doesNotExist());
        enviar(delete("/api/notas/actividades/" + actitud), docente, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actividades", hasSize(3)))
                .andExpect(jsonPath("$.estudiantes[0].completa").value(false));
    }

    @Test
    void elSeguimientoMuestraElAvanceDeNotasYAsistencia() throws Exception {
        Escenario e = crearEscenario("Seguimiento", 2024, "docente.seg", "director.seg");
        MockHttpSession docente = iniciarSesion("docente.seg", CONTRASENA);
        MockHttpSession director = iniciarSesion("director.seg", CONTRASENA);
        long taller = crearActividad(docente, e, e.periodo1(), "SABER", "Taller");
        enviar(put("/api/notas/cargas/%d/periodos/%d".formatted(e.carga(), e.periodo1())), docente,
                "{\"notas\": [%s]}".formatted(nota(taller, e.ana(), "4.0"))).andExpect(status().isOk());
        enviar(put("/api/asistencia/cargas/" + e.carga() + "?fecha=2024-06-10"), docente, """
                {"horas": 1, "estudiantes": [{"matriculaId": %d, "estado": "ASISTIO"},
                                              {"matriculaId": %d, "estado": "FALTA"}]}
                """.formatted(e.ana(), e.bruno())).andExpect(status().isOk());

        // Periodo 1: una actividad para dos estudiantes, una nota; faltan actividades de Hacer y Ser
        mockMvc.perform(get("/api/seguimiento/avance?periodo=1").session(docente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clases", hasSize(1)))
                .andExpect(jsonPath("$.clases[0].periodoNumero").value(1))
                .andExpect(jsonPath("$.clases[0].estudiantes").value(2))
                .andExpect(jsonPath("$.clases[0].actividades").value(1))
                .andExpect(jsonPath("$.clases[0].notasRegistradas").value(1))
                .andExpect(jsonPath("$.clases[0].notasEsperadas").value(2))
                .andExpect(jsonPath("$.clases[0].porcentajeNotas").value(50))
                .andExpect(jsonPath("$.clases[0].dimensionesSinActividad", hasSize(2)))
                .andExpect(jsonPath("$.clases[0].diasConAsistencia").value(1))
                .andExpect(jsonPath("$.clases[0].ultimaAsistencia").value("2024-06-10"))
                // Hasta el cierre del periodo (15 de junio), no hasta hoy
                .andExpect(jsonPath("$.clases[0].diasSinAsistencia").value(5));

        // Sin periodo se toma el ultimo que empezo; sin registros, se cuenta desde su inicio
        mockMvc.perform(get("/api/seguimiento/avance").session(docente))
                .andExpect(jsonPath("$.clases[0].periodoNumero").value(2))
                .andExpect(jsonPath("$.clases[0].porcentajeNotas").value(0))
                .andExpect(jsonPath("$.clases[0].diasSinAsistencia").value(167));

        // El director de grupo sin carga no ve clases; directivos ven todas
        mockMvc.perform(get("/api/seguimiento/avance").session(director))
                .andExpect(jsonPath("$.clases", hasSize(0)));
        mockMvc.perform(get("/api/seguimiento/avance?periodo=1").session(sesionAdmin))
                .andExpect(jsonPath("$.clases[?(@.cargaId == %d)].porcentajeNotas".formatted(e.carga()), hasItem(50)));
    }

    @Test
    void elInformeDelPeriodoGuardaDescriptoresComportamientoYObservaciones() throws Exception {
        Escenario e = crearEscenario("Informe", 2040, "docente.informe", "director.informe");
        MockHttpSession docente = iniciarSesion("docente.informe", CONTRASENA);
        MockHttpSession otro = iniciarSesion("director.informe", CONTRASENA);
        String informe = "/api/notas/cargas/%d/periodos/%d/informe".formatted(e.carga(), e.periodo1());
        long taller = crearActividad(docente, e, e.periodo1(), "SABER", "Taller");
        enviar(put("/api/notas/cargas/%d/periodos/%d".formatted(e.carga(), e.periodo1())), docente,
                "{\"notas\": [%s, %s]}".formatted(nota(taller, e.ana(), "4.8"), nota(taller, e.bruno(), "2.0")))
                .andExpect(status().isOk());

        enviar(put(informe), docente, """
                {"descriptores": {"SUPERIOR": "Explica con autonomía las funciones de la célula.",
                                  "BAJO": "  Se le dificulta identificar las partes de la célula.  "},
                 "estudiantes": [{"matriculaId": %d, "comportamiento": 4.5, "observacion": "Participa en clase."},
                                 {"matriculaId": %d, "comportamiento": 3.0}]}
                """.formatted(e.ana(), e.bruno()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descriptores.SUPERIOR").value("Explica con autonomía las funciones de la célula."))
                .andExpect(jsonPath("$.descriptores.BAJO").value("Se le dificulta identificar las partes de la célula."))
                .andExpect(jsonPath("$.estudiantes[0].nombres").value("Ana"))
                .andExpect(jsonPath("$.estudiantes[0].desempeno").value("SUPERIOR"))
                .andExpect(jsonPath("$.estudiantes[0].comportamiento").value(4.5))
                .andExpect(jsonPath("$.estudiantes[0].observacion").value("Participa en clase."))
                .andExpect(jsonPath("$.estudiantes[1].desempeno").value("BAJO"))
                .andExpect(jsonPath("$.estudiantes[1].observacion").isEmpty());

        // Solo cambia lo enviado: texto vacio borra el descriptor y sin datos se borra el informe del estudiante
        enviar(put(informe), docente, """
                {"descriptores": {"BAJO": ""}, "estudiantes": [{"matriculaId": %d}]}
                """.formatted(e.bruno()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descriptores.SUPERIOR").exists())
                .andExpect(jsonPath("$.descriptores.BAJO").doesNotExist())
                .andExpect(jsonPath("$.estudiantes[0].comportamiento").value(4.5))
                .andExpect(jsonPath("$.estudiantes[1].comportamiento").isEmpty());

        // Fuera de escala, descriptor muy largo, o de otro docente
        enviar(put(informe), docente, "{\"estudiantes\": [{\"matriculaId\": %d, \"comportamiento\": 6}]}".formatted(e.ana()))
                .andExpect(status().isConflict());
        enviar(put(informe), docente, "{\"descriptores\": {\"ALTO\": \"%s\"}}".formatted("a".repeat(601)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(informe).session(otro)).andExpect(status().isForbidden());
    }

    @Test
    void elConsolidadoMuestraElPeriodoYElAcumuladoDelAnio() throws Exception {
        Escenario e = crearEscenario("Notas2", 2039, "docente.notas2", "director.notas2");
        MockHttpSession docente = iniciarSesion("docente.notas2", CONTRASENA);
        MockHttpSession director = iniciarSesion("director.notas2", CONTRASENA);

        String p1 = "/api/notas/cargas/%d/periodos/%d".formatted(e.carga(), e.periodo1());
        String p2 = "/api/notas/cargas/%d/periodos/%d".formatted(e.carga(), e.periodo2());
        long s1 = crearActividad(docente, e, e.periodo1(), "SABER", "Quiz");
        long h1 = crearActividad(docente, e, e.periodo1(), "HACER", "Taller");
        long r1 = crearActividad(docente, e, e.periodo1(), "SER", "Actitud");
        enviar(put(p1), docente, "{\"notas\": [%s, %s, %s, %s, %s, %s]}".formatted(
                nota(s1, e.ana(), "4.0"), nota(h1, e.ana(), "4.0"), nota(r1, e.ana(), "4.0"),
                nota(s1, e.bruno(), "2.0"), nota(h1, e.bruno(), "2.5"), nota(r1, e.bruno(), "3.0")))
                .andExpect(status().isOk());
        long s2 = crearActividad(docente, e, e.periodo2(), "SABER", "Quiz 2");
        long h2 = crearActividad(docente, e, e.periodo2(), "HACER", "Taller 2");
        long r2 = crearActividad(docente, e, e.periodo2(), "SER", "Actitud 2");
        enviar(put(p2), docente, "{\"notas\": [%s, %s, %s]}".formatted(
                nota(s2, e.ana(), "3.0"), nota(h2, e.ana(), "3.0"), nota(r2, e.ana(), "3.0")))
                .andExpect(status().isOk());

        String consolidado = "/api/notas/grupos/" + e.grupo() + "/consolidado";
        mockMvc.perform(get(consolidado + "?periodoId=" + e.periodo1()).session(director))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asignaturas", hasSize(1)))
                .andExpect(jsonPath("$.estudiantes[0].notas[0].nota").value(4.0))
                .andExpect(jsonPath("$.estudiantes[0].notas[0].desempeno").value("ALTO"))
                .andExpect(jsonPath("$.estudiantes[1].notas[0].nota").value(2.4))
                .andExpect(jsonPath("$.estudiantes[1].asignaturasEnBajo").value(1));
        // Anio: Ana (4.0 * 50 + 3.0 * 50) / 100 = 3.5; Bruno solo tiene el periodo 1, nota parcial
        mockMvc.perform(get(consolidado).session(director))
                .andExpect(jsonPath("$.periodoId").doesNotExist())
                .andExpect(jsonPath("$.estudiantes[0].notas[0].nota").value(3.5))
                .andExpect(jsonPath("$.estudiantes[0].notas[0].completa").value(true))
                .andExpect(jsonPath("$.estudiantes[1].notas[0].completa").value(false));
        mockMvc.perform(get(consolidado).session(docente)).andExpect(status().isForbidden());

        // Recuperacion del periodo 1: Bruno (2.4, Bajo) saca 4.5 pero la definitiva queda en el tope de 3.0
        String recuperaciones = p1 + "/recuperaciones";
        String rec = "{\"recuperaciones\": [{\"matriculaId\": %d, \"nota\": %s, \"observacion\": \"Taller de refuerzo\"}]}";
        enviar(put(recuperaciones), docente, rec.formatted(e.ana(), "4.0")).andExpect(status().isConflict());
        enviar(put(recuperaciones), docente, rec.formatted(e.bruno(), "4.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estudiantes[1].notaPeriodo").value(2.4))
                .andExpect(jsonPath("$.estudiantes[1].recuperacion").value(4.5))
                .andExpect(jsonPath("$.estudiantes[1].notaDefinitiva").value(3.0))
                .andExpect(jsonPath("$.estudiantes[1].desempeno").value("BASICO"));
        mockMvc.perform(get(consolidado + "?periodoId=" + e.periodo1()).session(director))
                .andExpect(jsonPath("$.estudiantes[1].notas[0].nota").value(3.0))
                .andExpect(jsonPath("$.estudiantes[1].asignaturasEnBajo").value(0));

        // Recuperacion final: solo con la nota del anio en Bajo
        String finalUrl = "/api/notas/cargas/" + e.carga() + "/recuperacion-final";
        enviar(put(finalUrl), docente, rec.formatted(e.bruno(), "3.5")).andExpect(status().isConflict());
        enviar(put(recuperaciones), docente, rec.formatted(e.bruno(), "null")).andExpect(status().isOk());
        mockMvc.perform(get(finalUrl).session(docente))
                .andExpect(jsonPath("$.estudiantes[1].notaAnio").value(2.4))
                .andExpect(jsonPath("$.estudiantes[1].desempeno").value("BAJO"));
        enviar(put(finalUrl), docente, rec.formatted(e.bruno(), "3.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estudiantes[1].recuperacion").value(3.5))
                .andExpect(jsonPath("$.estudiantes[1].notaDefinitiva").value(3.0));
        mockMvc.perform(get(consolidado).session(director))
                .andExpect(jsonPath("$.estudiantes[1].notas[0].nota").value(3.0));
        enviar(put(finalUrl), docente, rec.formatted(e.bruno(), "null")).andExpect(status().isOk());

        // Configuracion: valores por defecto; solo administrador o coordinacion la cambian y los pesos suman 100
        String config = "/api/notas/configuracion/" + e.anio();
        mockMvc.perform(get(config).session(docente))
                .andExpect(jsonPath("$.notaAprobatoria").value(3.0))
                .andExpect(jsonPath("$.limiteSuperior").value(4.6))
                .andExpect(jsonPath("$.pesoSer").value(20));
        String json = """
                {"notaMinima": 1.0, "notaMaxima": 5.0, "notaAprobatoria": 3.0, "limiteAlto": 4.0,
                 "limiteSuperior": 4.6, "topeRecuperacion": 3.0, "pesoSaber": %d, "pesoHacer": 30, "pesoSer": 30}
                """;
        enviar(put(config), docente, json.formatted(40)).andExpect(status().isForbidden());
        enviar(put(config), sesionAdmin, json.formatted(50)).andExpect(status().isConflict());
        enviar(put(config), sesionAdmin, json.formatted(40)).andExpect(status().isOk());

        // Bruno con pesos 40/30/30: 2.0 * 40 + 2.5 * 30 + 3.0 * 30 = 245 / 100 = 2.5 (2.45 redondeado)
        mockMvc.perform(get(consolidado + "?periodoId=" + e.periodo1()).session(director))
                .andExpect(jsonPath("$.estudiantes[1].notas[0].nota").value(2.5));
    }
}
