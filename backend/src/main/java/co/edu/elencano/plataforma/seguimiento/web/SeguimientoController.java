package co.edu.elencano.plataforma.seguimiento.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.elencano.plataforma.seguimiento.servicio.SeguimientoService;
import co.edu.elencano.plataforma.seguimiento.web.dto.AvanceRegistroDto;
import co.edu.elencano.plataforma.usuarios.seguridad.UsuarioAutenticado;

/** Avance del registro de notas y asistencia. El servicio decide que clases ve cada usuario. */
@RestController
@RequestMapping("/api/seguimiento")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECTOR', 'COORDINADOR_ACADEMICO', 'SECRETARIA', 'DOCENTE')")
public class SeguimientoController {

    private final SeguimientoService seguimientoService;

    public SeguimientoController(SeguimientoService seguimientoService) {
        this.seguimientoService = seguimientoService;
    }

    /** periodo es el numero del periodo (1, 2, 3); sin el, el periodo en curso. */
    @GetMapping("/avance")
    public AvanceRegistroDto avance(@RequestParam(required = false) Integer periodo,
                                    @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return seguimientoService.avance(periodo, usuario);
    }
}
