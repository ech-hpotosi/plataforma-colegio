package co.edu.elencano.plataforma.matricula;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.elencano.plataforma.PruebaIntegracion;
import co.edu.elencano.plataforma.usuarios.entidad.Rol;

class ImportacionEstudiantesTest extends PruebaIntegracion {

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession sesionAdmin;

    @BeforeEach
    void iniciarSesionComoAdmin() throws Exception {
        sesionAdmin = iniciarSesion("admin", "clave-de-prueba");
    }

    private JsonNode leer(ResultActions resultado) throws Exception {
        return objectMapper.readTree(resultado.andReturn().getResponse().getContentAsString());
    }

    private long crear(String url, String json) throws Exception {
        return leer(mockMvc.perform(post(url).with(tokenCsrf()).session(sesionAdmin)
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    private long crearAnio(int anio) throws Exception {
        return crear("/api/anios", """
                {"anio": %d, "fechaInicio": "%d-01-20", "fechaFin": "%d-11-30",
                 "periodos": [{"fechaInicio": "%d-01-20", "fechaFin": "%d-11-30", "porcentaje": 100}]}
                """.formatted(anio, anio, anio, anio, anio));
    }

    /**
     * Arma un archivo como los que exporta otro sistema: una fila de titulo antes del encabezado, nombres
     * y apellidos partidos, la fecha como fecha de Excel y el documento como numero.
     */
    private static byte[] excel(String sede, List<Object[]> filas) throws Exception {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Hoja1");
            hoja.createRow(0).createCell(0).setCellValue("Listado de estudiantes " + sede);
            String[] titulos = {"TIPO DOC", "Documento", "Primer nombre", "Segundo nombre", "Primer apellido",
                    "Segundo apellido", "Fecha de nacimiento", "Sexo", "Sede", "Grado", "Grupo",
                    "Documento del acudiente", "Nombres del acudiente", "Apellidos del acudiente", "Parentesco"};
            Row encabezado = hoja.createRow(1);
            for (int i = 0; i < titulos.length; i++) {
                encabezado.createCell(i).setCellValue(titulos[i]);
            }
            CellStyle estiloFecha = libro.createCellStyle();
            estiloFecha.setDataFormat(libro.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy"));
            for (int f = 0; f < filas.size(); f++) {
                Row fila = hoja.createRow(f + 2);
                Object[] valores = filas.get(f);
                for (int i = 0; i < valores.length; i++) {
                    Object v = valores[i];
                    if (v instanceof Number n) {
                        fila.createCell(i).setCellValue(n.doubleValue());
                    } else if (v instanceof LocalDate d) {
                        fila.createCell(i).setCellValue(d);
                        fila.getCell(i).setCellStyle(estiloFecha);
                    } else if (v != null) {
                        fila.createCell(i).setCellValue(v.toString());
                    }
                }
            }
            libro.write(salida);
            return salida.toByteArray();
        }
    }

    private ResultActions importar(long anioId, boolean confirmar, byte[] archivo, MockHttpSession sesion)
            throws Exception {
        return mockMvc.perform(multipart("/api/importacion-estudiantes")
                .file(new MockMultipartFile("archivo", "estudiantes.xlsx", "application/octet-stream", archivo))
                .param("anioId", String.valueOf(anioId))
                .param("confirmar", String.valueOf(confirmar))
                .with(tokenCsrf()).session(sesion));
    }

    private JsonNode buscarEstudiantes(String texto) throws Exception {
        return leer(mockMvc.perform(get("/api/estudiantes").param("buscar", texto).session(sesionAdmin)))
                .get("contenido");
    }

    @Test
    void revisaElArchivoSinGuardarYAlConfirmarCreaEstudiantesMatriculasYAcudientes() throws Exception {
        String nombreSede = "Sede Importación 2066";
        long sede = crear("/api/sedes", "{\"nombre\": \"" + nombreSede + "\"}");
        long anio = crearAnio(2066);
        long sexto = leer(mockMvc.perform(get("/api/grados").session(sesionAdmin))).get(6).get("id").asLong();
        long grupo = crear("/api/grupos", """
                {"anioLectivoId": %d, "sedeId": %d, "gradoId": %d, "nombre": "6-01", "jornada": "MANANA", "cupo": 30}
                """.formatted(anio, sede, sexto));

        byte[] archivo = excel(nombreSede, List.of(
                new Object[] {"TI", 500001, "Luna", "Sofía", "Jojoa", "Chasoy", LocalDate.of(2014, 4, 2), "F",
                        "sede importacion 2066", "6", "6-01", "27500001", "Rosa Elena", "Chasoy", "mamá"},
                new Object[] {"Tarjeta de identidad", "500.002", "Mateo", null, "Botina", null, "15/08/2013", "M",
                        nombreSede, "Sexto", null},
                new Object[] {"TI", 500003, "Ana", null, "Matabanchoy", null, "2013-01-10", "F", nombreSede,
                        "Doceavo", null},
                new Object[] {"TI", 500001, "Luna", null, "Repetida", null, "2014-04-02", "F", nombreSede, "6", null}));

        importar(anio, false, archivo, sesionAdmin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anio").value(2066))
                .andExpect(jsonPath("$.aplicado").value(false))
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.creados").value(2))
                .andExpect(jsonPath("$.conErrores").value(2))
                .andExpect(jsonPath("$.filas[0].fila").value(3))
                .andExpect(jsonPath("$.filas[0].estudiante").value("Luna Sofía Jojoa Chasoy"))
                .andExpect(jsonPath("$.filas[0].grupo").value("6-01"))
                .andExpect(jsonPath("$.filas[0].accion").value("CREAR"))
                .andExpect(jsonPath("$.filas[1].documento").value("500002"))
                .andExpect(jsonPath("$.filas[1].avisos[0]").value(containsString("Sin grupo")))
                .andExpect(jsonPath("$.filas[2].accion").value("ERROR"))
                .andExpect(jsonPath("$.filas[2].errores[0]").value("No existe el grado Doceavo"))
                .andExpect(jsonPath("$.filas[3].errores[0]").value(containsString("repetido")));
        assertThat(buscarEstudiantes("500001")).isEmpty();

        importar(anio, true, archivo, sesionAdmin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aplicado").value(true))
                .andExpect(jsonPath("$.creados").value(2))
                .andExpect(jsonPath("$.conErrores").value(2));

        mockMvc.perform(get("/api/grupos/" + grupo + "/estudiantes").session(sesionAdmin))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].numeroDocumento").value("500001"));
        long luna = buscarEstudiantes("500001").get(0).get("id").asLong();
        mockMvc.perform(get("/api/estudiantes/" + luna).session(sesionAdmin))
                .andExpect(jsonPath("$.fechaNacimiento").value("2014-04-02"))
                .andExpect(jsonPath("$.genero").value("FEMENINO"))
                .andExpect(jsonPath("$.acudientes", hasSize(1)))
                .andExpect(jsonPath("$.acudientes[0].parentesco").value("MADRE"))
                .andExpect(jsonPath("$.acudientes[0].accesoConsulta").value(false));
        long mateo = buscarEstudiantes("500002").get(0).get("id").asLong();
        mockMvc.perform(get("/api/estudiantes/" + mateo + "/matriculas").session(sesionAdmin))
                .andExpect(jsonPath("$", hasSize(1)));

        // Volver a cargar el mismo archivo actualiza sin duplicar fichas, matriculas ni acudientes
        importar(anio, true, archivo, sesionAdmin)
                .andExpect(jsonPath("$.creados").value(0))
                .andExpect(jsonPath("$.actualizados").value(2))
                .andExpect(jsonPath("$.filas[0].accion").value("ACTUALIZAR"));
        assertThat(buscarEstudiantes("500001")).hasSize(1);
        mockMvc.perform(get("/api/estudiantes/" + luna).session(sesionAdmin))
                .andExpect(jsonPath("$.acudientes", hasSize(1)));
        mockMvc.perform(get("/api/grupos/" + grupo + "/estudiantes").session(sesionAdmin))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void validaElArchivoYSoloLoUsaQuienGestionaEstudiantes() throws Exception {
        long anio = crearAnio(2067);

        importar(anio, false, "no es un excel".getBytes(StandardCharsets.UTF_8), sesionAdmin)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(containsString("No se pudo leer el archivo")));
        importar(anio, false, excel("Sin filas", List.of()), sesionAdmin)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(containsString("no tiene estudiantes")));

        mockMvc.perform(get("/api/importacion-estudiantes/plantilla").session(sesionAdmin))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("plantilla-estudiantes.xlsx")));

        crearUsuario("docente.importacion", Rol.DOCENTE);
        MockHttpSession docente = iniciarSesion("docente.importacion", CONTRASENA);
        importar(anio, false, excel("x", List.of()), docente).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/importacion-estudiantes/plantilla").session(docente))
                .andExpect(status().isForbidden());
    }

    @Test
    void laPlantillaDescargadaSePuedeVolverACargar() throws Exception {
        long anio = crearAnio(2068);
        byte[] plantilla = mockMvc.perform(get("/api/importacion-estudiantes/plantilla").session(sesionAdmin))
                .andReturn().getResponse().getContentAsByteArray();

        importar(anio, false, plantilla, sesionAdmin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.conErrores").value(0))
                .andExpect(jsonPath("$.filas[0].estudiante").value("Luna Sofía Jojoa Chasoy"));
    }
}
