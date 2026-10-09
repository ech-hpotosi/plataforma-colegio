package co.edu.elencano.plataforma.asistencia.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.elencano.plataforma.asistencia.servicio.AsistenciaService;
import co.edu.elencano.plataforma.asistencia.web.dto.AsistenciaClaseDto;
import co.edu.elencano.plataforma.asistencia.web.dto.CargaDocenteDto;
import co.edu.elencano.plataforma.asistencia.web.dto.GrupoAsistenciaDto;
import co.edu.elencano.plataforma.asistencia.web.dto.GuardarAsistenciaDto;
import co.edu.elencano.plataforma.asistencia.web.dto.JustificarDto;
import co.edu.elencano.plataforma.asistencia.web.dto.NovedadAsistenciaDto;
import co.edu.elencano.plataforma.asistencia.web.dto.PendientesAsistenciaDto;
import co.edu.elencano.plataforma.asistencia.web.dto.ResumenGrupoDto;
import co.edu.elencano.plataforma.usuarios.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;

/**
 * Asistencia por clase, consolidado por grupo y justificacion de faltas.
 * Aqui solo se filtra por rol; el servicio verifica que la carga sea del docente o que sea director del grupo.
 */
@RestController
@RequestMapping("/api/asistencia")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECTOR', 'COORDINADOR_ACADEMICO', 'SECRETARIA', 'DOCENTE')")
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    public AsistenciaController(AsistenciaService asistenciaService) {
        this.asistenciaService = asistenciaService;
    }

    @GetMapping("/pendientes")
    public PendientesAsistenciaDto pendientes(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return asistenciaService.pendientes(usuario);
    }

    @GetMapping("/cargas")
    public List<CargaDocenteDto> cargas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return asistenciaService.listarCargas(usuario);
    }

    @GetMapping("/cargas/{id}")
    public AsistenciaClaseDto clase(@PathVariable Long id,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                    @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return asistenciaService.obtenerClase(id, fecha, usuario);
    }

    @PutMapping("/cargas/{id}")
    public AsistenciaClaseDto guardar(@PathVariable Long id,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                      @Valid @RequestBody GuardarAsistenciaDto datos,
                                      @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return asistenciaService.guardarClase(id, fecha, datos, usuario);
    }

    @GetMapping("/grupos")
    public List<GrupoAsistenciaDto> grupos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return asistenciaService.listarGrupos(usuario);
    }

    @GetMapping("/grupos/{id}/resumen")
    public ResumenGrupoDto resumen(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return asistenciaService.resumenGrupo(id, usuario);
    }

    @GetMapping("/matriculas/{id}/novedades")
    public List<NovedadAsistenciaDto> novedades(@PathVariable Long id,
                                                @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return asistenciaService.novedades(id, usuario);
    }

    @PostMapping("/matriculas/{id}/justificacion")
    public List<NovedadAsistenciaDto> justificar(@PathVariable Long id, @Valid @RequestBody JustificarDto datos,
                                                 @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return asistenciaService.justificar(id, datos.fecha(), datos.justificacion(), usuario);
    }
}
