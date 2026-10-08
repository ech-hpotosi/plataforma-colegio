package co.edu.elencano.plataforma.academico;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import co.edu.elencano.plataforma.usuarios.entidad.Usuario;

class EstructuraAcademicaTest extends PruebaIntegracion {

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

    private long crearSede(String nombre, boolean principal) throws Exception {
        String json = """
                {"nombre": "%s", "principal": %s}
                """.formatted(nombre, principal);
        return leer(enviar(post("/api/sedes"), sesionAdmin, json).andExpect(status().isCreated())).get("id").asLong();
    }

    private String jsonAnio(int anio, String p1, String p2, String p3) {
        return """
                {"anio": %d, "fechaInicio": "%d-01-26", "fechaFin": "%d-11-30", "periodos": [
                  {"fechaInicio": "%d-01-26", "fechaFin": "%d-04-30", "porcentaje": %s},
                  {"fechaInicio": "%d-05-04", "fechaFin": "%d-08-14", "porcentaje": %s},
                  {"fechaInicio": "%d-08-18", "fechaFin": "%d-11-30", "porcentaje": %s}]}
                """.formatted(anio, anio, anio, anio, anio, p1, anio, anio, p2, anio, anio, p3);
    }

    private long crearAnio(int anio) throws Exception {
        return leer(enviar(post("/api/anios"), sesionAdmin, jsonAnio(anio, "30", "30", "40"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PLANEACION"))
                .andExpect(jsonPath("$.periodos", hasSize(3)))).get("id").asLong();
    }

    private long idGrado(String nombre) throws Exception {
        for (JsonNode grado : leer(mockMvc.perform(get("/api/grados").session(sesionAdmin)))) {
            if (grado.get("nombre").asText().equals(nombre)) {
                return grado.get("id").asLong();
            }
        }
        throw new IllegalStateException("No existe el grado " + nombre);
    }

    @Test
    void losGradosVienenCargadosYTransicionEsCualitativo() throws Exception {
        mockMvc.perform(get("/api/grados").session(sesionAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(12)))
                .andExpect(jsonPath("$[0].nombre").value("Transicion"))
                .andExpect(jsonPath("$[0].evaluacionCualitativa").value(true))
                .andExpect(jsonPath("$[11].nombre").value("Undecimo"));
    }

    @Test
    void soloUnaSedeQuedaComoPrincipal() throws Exception {
        long primera = crearSede("Sede Principal Prueba", true);
        crearSede("Sede Otra Principal", true);

        JsonNode sedes = leer(mockMvc.perform(get("/api/sedes").session(sesionAdmin)));
        int principales = 0;
        for (JsonNode sede : sedes) {
            if (sede.get("principal").asBoolean()) {
                principales++;
            }
            if (sede.get("id").asLong() == primera) {
                org.junit.jupiter.api.Assertions.assertFalse(sede.get("principal").asBoolean());
            }
        }
        org.junit.jupiter.api.Assertions.assertEquals(1, principales);

        enviar(post("/api/sedes"), sesionAdmin, """
                {"nombre": "sede principal prueba", "principal": false}
                """).andExpect(status().isConflict());
    }

    @Test
    void periodosQueNoSumanCienSeRechazan() throws Exception {
        enviar(post("/api/anios"), sesionAdmin, jsonAnio(2031, "30", "30", "30"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Los porcentajes de los periodos deben sumar 100. Suman 90"));
    }

    @Test
    void periodosCruzadosSeRechazan() throws Exception {
        String json = jsonAnio(2032, "30", "30", "40").replace("2032-05-04", "2032-04-20");
        enviar(post("/api/anios"), sesionAdmin, json)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El periodo 2 debe empezar despues de que termine el anterior"));
    }

    @Test
    void elEstadoDelAnioSoloAvanzaYSoloUnoEnCurso() throws Exception {
        long anio = crearAnio(2033);
        long otro = crearAnio(2034);

        enviar(put("/api/anios/" + anio + "/estado"), sesionAdmin, "{\"estado\": \"EN_CURSO\"}")
                .andExpect(status().isOk());
        enviar(put("/api/anios/" + anio + "/estado"), sesionAdmin, "{\"estado\": \"MATRICULA\"}")
                .andExpect(status().isConflict());
        enviar(put("/api/anios/" + otro + "/estado"), sesionAdmin, "{\"estado\": \"EN_CURSO\"}")
                .andExpect(status().isConflict());

        enviar(put("/api/anios/" + anio + "/estado"), sesionAdmin, "{\"estado\": \"CERRADO\"}")
                .andExpect(status().isOk());
        enviar(put("/api/anios/" + anio), sesionAdmin, jsonAnio(2033, "30", "30", "40"))
                .andExpect(status().isConflict());
    }

    @Test
    void editarAnioPuedeCambiarLaCantidadDePeriodos() throws Exception {
        long anio = crearAnio(2035);
        String dosPeriodos = """
                {"anio": 2035, "fechaInicio": "2035-01-26", "fechaFin": "2035-11-30", "periodos": [
                  {"fechaInicio": "2035-01-26", "fechaFin": "2035-06-15", "porcentaje": 50},
                  {"fechaInicio": "2035-07-01", "fechaFin": "2035-11-30", "porcentaje": 50}]}
                """;
        enviar(put("/api/anios/" + anio), sesionAdmin, dosPeriodos)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodos", hasSize(2)))
                .andExpect(jsonPath("$.periodos[1].numero").value(2))
                .andExpect(jsonPath("$.periodos[1].porcentaje").value(50));
    }

    @Test
    void flujoCompletoPlanGrupoYCargaAcademica() throws Exception {
        long sede = crearSede("Sede Flujo", false);
        long otraSede = crearSede("Sede Lejana", false);
        long anio = crearAnio(2036);
        long sexto = idGrado("Sexto");

        long area = leer(enviar(post("/api/areas"), sesionAdmin, "{\"nombre\": \"Matematicas Flujo\"}")
                .andExpect(status().isCreated())).get("id").asLong();
        long algebra = leer(enviar(post("/api/asignaturas"), sesionAdmin,
                "{\"areaId\": %d, \"nombre\": \"Aritmetica Flujo\"}".formatted(area))
                .andExpect(status().isCreated())).get("id").asLong();
        long geometria = leer(enviar(post("/api/asignaturas"), sesionAdmin,
                "{\"areaId\": %d, \"nombre\": \"Geometria Flujo\"}".formatted(area))).get("id").asLong();

        String plan = """
                {"asignaturas": [{"asignaturaId": %d, "intensidadHoraria": 4},
                                 {"asignaturaId": %d, "intensidadHoraria": 1}]}
                """.formatted(algebra, geometria);
        enviar(put("/api/plan-estudio?anioId=%d&gradoId=%d".formatted(anio, sexto)), sesionAdmin, plan)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        Usuario docente = crearUsuario("docente.flujo", Rol.DOCENTE);
        Long docenteId = docente.getPersona().getId();
        enviar(put("/api/docentes/" + docenteId), sesionAdmin,
                "{\"especialidad\": \"Matematicas\", \"sedeIds\": [%d]}".formatted(sede))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sedeIds[0]").value(sede));

        String grupoEnOtraSede = """
                {"anioLectivoId": %d, "sedeId": %d, "gradoId": %d, "nombre": "6-01", "jornada": "MANANA",
                 "cupo": 35, "directorId": %d}
                """.formatted(anio, otraSede, sexto, docenteId);
        enviar(post("/api/grupos"), sesionAdmin, grupoEnOtraSede)
                .andExpect(status().isConflict());

        long grupo = leer(enviar(post("/api/grupos"), sesionAdmin, grupoEnOtraSede.replace(
                "\"sedeId\": " + otraSede, "\"sedeId\": " + sede))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.director").value("Nombre Apellido"))).get("id").asLong();

        mockMvc.perform(get("/api/grupos/" + grupo + "/carga").session(sesionAdmin))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].docenteId").value(nullValue()));

        enviar(put("/api/grupos/" + grupo + "/carga"), sesionAdmin,
                "{\"asignaciones\": [{\"asignaturaId\": %d, \"docenteId\": %d}]}".formatted(algebra, docenteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].asignatura").value("Aritmetica Flujo"))
                .andExpect(jsonPath("$[0].docenteId").value(docenteId));

        // Quitar del plan una asignatura que ya tiene docente no se permite
        String planSinAlgebra = """
                {"asignaturas": [{"asignaturaId": %d, "intensidadHoraria": 2}]}
                """.formatted(geometria);
        enviar(put("/api/plan-estudio?anioId=%d&gradoId=%d".formatted(anio, sexto)), sesionAdmin, planSinAlgebra)
                .andExpect(status().isConflict());

        // Una asignatura fuera del plan no se puede asignar
        long fueraDelPlan = leer(enviar(post("/api/asignaturas"), sesionAdmin,
                "{\"areaId\": %d, \"nombre\": \"Estadistica Flujo\"}".formatted(area))).get("id").asLong();
        enviar(put("/api/grupos/" + grupo + "/carga"), sesionAdmin,
                "{\"asignaciones\": [{\"asignaturaId\": %d, \"docenteId\": %d}]}".formatted(fueraDelPlan, docenteId))
                .andExpect(status().isConflict());
    }

    @Test
    void unaPersonaQueNoEsDocenteNoSePuedeAsignar() throws Exception {
        long sede = crearSede("Sede No Docente", false);
        long anio = crearAnio(2037);
        Usuario secretaria = crearUsuario("secretaria.asignada", Rol.SECRETARIA);

        enviar(post("/api/grupos"), sesionAdmin, """
                {"anioLectivoId": %d, "sedeId": %d, "gradoId": %d, "nombre": "1-01", "jornada": "MANANA",
                 "cupo": 30, "directorId": %d}
                """.formatted(anio, sede, idGrado("Primero"), secretaria.getPersona().getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void unDocentePuedeConsultarPeroNoModificar() throws Exception {
        crearUsuario("docente.lector", Rol.DOCENTE);
        MockHttpSession sesionDocente = iniciarSesion("docente.lector", CONTRASENA);

        mockMvc.perform(get("/api/sedes").session(sesionDocente)).andExpect(status().isOk());
        mockMvc.perform(get("/api/docentes").session(sesionDocente)).andExpect(status().isForbidden());
        enviar(post("/api/sedes"), sesionDocente, "{\"nombre\": \"Sede Prohibida\"}")
                .andExpect(status().isForbidden());
    }

    @Test
    void elCoordinadorAcademicoPuedeModificar() throws Exception {
        crearUsuario("coordinador.prueba", Rol.COORDINADOR_ACADEMICO);
        MockHttpSession sesionCoordinador = iniciarSesion("coordinador.prueba", CONTRASENA);

        enviar(post("/api/areas"), sesionCoordinador, "{\"nombre\": \"Area del Coordinador\"}")
                .andExpect(status().isCreated());
    }
}
