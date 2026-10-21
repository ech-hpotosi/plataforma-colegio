package co.edu.elencano.plataforma.matricula.web;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import co.edu.elencano.plataforma.estudiantes.web.GestionEstudiantes;
import co.edu.elencano.plataforma.matricula.servicio.ImportacionEstudiantesService;
import co.edu.elencano.plataforma.matricula.web.dto.ResultadoImportacionDto;

/** Carga de estudiantes desde Excel. Sin confirmar solo revisa el archivo y muestra lo que haria. */
@RestController
@GestionEstudiantes
public class ImportacionEstudiantesController {

    private static final MediaType EXCEL =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ImportacionEstudiantesService importacionService;

    public ImportacionEstudiantesController(ImportacionEstudiantesService importacionService) {
        this.importacionService = importacionService;
    }

    @PostMapping(value = "/api/importacion-estudiantes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResultadoImportacionDto importar(@RequestParam Long anioId,
                                            @RequestParam(defaultValue = "false") boolean confirmar,
                                            @RequestParam MultipartFile archivo) throws IOException {
        try (InputStream contenido = archivo.getInputStream()) {
            return importacionService.importar(anioId, contenido, confirmar);
        }
    }

    @GetMapping("/api/importacion-estudiantes/plantilla")
    public ResponseEntity<byte[]> plantilla() {
        return ResponseEntity.ok()
                .contentType(EXCEL)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("plantilla-estudiantes.xlsx").build().toString())
                .body(importacionService.plantilla());
    }
}
