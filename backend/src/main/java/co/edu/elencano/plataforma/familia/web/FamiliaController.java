package co.edu.elencano.plataforma.familia.web;

import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.elencano.plataforma.familia.servicio.FamiliaService;
import co.edu.elencano.plataforma.familia.servicio.FamiliaService.Boletin;
import co.edu.elencano.plataforma.familia.web.dto.EstudianteFamiliaDto;
import co.edu.elencano.plataforma.familia.web.dto.ResumenFamiliaDto;
import co.edu.elencano.plataforma.usuarios.seguridad.UsuarioAutenticado;

/** Consulta de notas, asistencia y boletines para acudientes y estudiantes. El servicio verifica cada estudiante. */
@RestController
@PreAuthorize("hasAnyRole('ACUDIENTE', 'ESTUDIANTE')")
public class FamiliaController {

    private final FamiliaService familiaService;

    public FamiliaController(FamiliaService familiaService) {
        this.familiaService = familiaService;
    }

    @GetMapping("/api/familia/estudiantes")
    public List<EstudianteFamiliaDto> estudiantes(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return familiaService.estudiantes(usuario);
    }

    @GetMapping("/api/familia/estudiantes/{id}/resumen")
    public ResumenFamiliaDto resumen(@PathVariable Long id, @RequestParam(required = false) Long matriculaId,
                                     @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return familiaService.resumen(id, matriculaId, usuario);
    }

    @GetMapping(value = "/api/familia/estudiantes/{id}/boletin", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> boletin(@PathVariable Long id, @RequestParam Long matriculaId,
                                          @RequestParam Long periodoId,
                                          @AuthenticationPrincipal UsuarioAutenticado usuario) {
        Boletin boletin = familiaService.boletin(id, matriculaId, periodoId, usuario);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(boletin.archivo()).build().toString())
                .body(boletin.contenido());
    }
}
