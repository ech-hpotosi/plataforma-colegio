package co.edu.elencano.plataforma.notas.web;

import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.elencano.plataforma.notas.servicio.BoletinPdf;
import co.edu.elencano.plataforma.notas.servicio.BoletinService;
import co.edu.elencano.plataforma.notas.servicio.DatosBoletin;
import co.edu.elencano.plataforma.notas.servicio.InformePeriodoService;
import co.edu.elencano.plataforma.notas.servicio.NotasService;
import co.edu.elencano.plataforma.notas.web.dto.ActividadEntradaDto;
import co.edu.elencano.plataforma.notas.web.dto.CargaNotasDto;
import co.edu.elencano.plataforma.notas.web.dto.ConfiguracionEvaluacionDto;
import co.edu.elencano.plataforma.notas.web.dto.ConsolidadoNotasDto;
import co.edu.elencano.plataforma.notas.web.dto.GuardarConfiguracionDto;
import co.edu.elencano.plataforma.notas.web.dto.GuardarInformeDto;
import co.edu.elencano.plataforma.notas.web.dto.GuardarNotasDto;
import co.edu.elencano.plataforma.notas.web.dto.GuardarRecuperacionesDto;
import co.edu.elencano.plataforma.notas.web.dto.InformePeriodoDto;
import co.edu.elencano.plataforma.notas.web.dto.PlanillaNotasDto;
import co.edu.elencano.plataforma.notas.web.dto.RecuperacionFinalDto;
import co.edu.elencano.plataforma.usuarios.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;

/**
 * Notas segun el SIEE: configuracion de la escala, planilla del docente por clase y periodo,
 * y consolidado por grupo. Aqui solo se filtra por rol; el servicio verifica la carga o el grupo.
 */
@RestController
@RequestMapping("/api/notas")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECTOR', 'COORDINADOR_ACADEMICO', 'SECRETARIA', 'DOCENTE')")
public class NotasController {

    private final NotasService notasService;
    private final InformePeriodoService informeService;
    private final BoletinService boletinService;
    private final BoletinPdf boletinPdf;

    public NotasController(NotasService notasService, InformePeriodoService informeService,
                           BoletinService boletinService, BoletinPdf boletinPdf) {
        this.notasService = notasService;
        this.informeService = informeService;
        this.boletinService = boletinService;
        this.boletinPdf = boletinPdf;
    }

    @GetMapping("/configuracion/{anioId}")
    public ConfiguracionEvaluacionDto configuracion(@PathVariable Long anioId) {
        return notasService.obtenerConfiguracion(anioId);
    }

    @PutMapping("/configuracion/{anioId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR_ACADEMICO')")
    public ConfiguracionEvaluacionDto guardarConfiguracion(@PathVariable Long anioId,
                                                           @Valid @RequestBody GuardarConfiguracionDto datos) {
        return notasService.guardarConfiguracion(anioId, datos);
    }

    @GetMapping("/cargas")
    public List<CargaNotasDto> cargas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return notasService.listarCargas(usuario);
    }

    @GetMapping("/cargas/{cargaId}/periodos/{periodoId}")
    public PlanillaNotasDto planilla(@PathVariable Long cargaId, @PathVariable Long periodoId,
                                     @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return notasService.planilla(cargaId, periodoId, usuario);
    }

    @PutMapping("/cargas/{cargaId}/periodos/{periodoId}")
    public PlanillaNotasDto guardarNotas(@PathVariable Long cargaId, @PathVariable Long periodoId,
                                         @Valid @RequestBody GuardarNotasDto datos,
                                         @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return notasService.guardarNotas(cargaId, periodoId, datos, usuario);
    }

    @GetMapping("/cargas/{cargaId}/periodos/{periodoId}/informe")
    public InformePeriodoDto informe(@PathVariable Long cargaId, @PathVariable Long periodoId,
                                     @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return informeService.informe(cargaId, periodoId, usuario);
    }

    @PutMapping("/cargas/{cargaId}/periodos/{periodoId}/informe")
    public InformePeriodoDto guardarInforme(@PathVariable Long cargaId, @PathVariable Long periodoId,
                                            @Valid @RequestBody GuardarInformeDto datos,
                                            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return informeService.guardar(cargaId, periodoId, datos, usuario);
    }

    @PostMapping("/cargas/{cargaId}/periodos/{periodoId}/actividades")
    public PlanillaNotasDto crearActividad(@PathVariable Long cargaId, @PathVariable Long periodoId,
                                           @Valid @RequestBody ActividadEntradaDto datos,
                                           @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return notasService.crearActividad(cargaId, periodoId, datos, usuario);
    }

    @PutMapping("/actividades/{id}")
    public PlanillaNotasDto modificarActividad(@PathVariable Long id, @Valid @RequestBody ActividadEntradaDto datos,
                                               @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return notasService.modificarActividad(id, datos, usuario);
    }

    @DeleteMapping("/actividades/{id}")
    public PlanillaNotasDto eliminarActividad(@PathVariable Long id,
                                              @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return notasService.eliminarActividad(id, usuario);
    }

    @PutMapping("/cargas/{cargaId}/periodos/{periodoId}/recuperaciones")
    public PlanillaNotasDto guardarRecuperaciones(@PathVariable Long cargaId, @PathVariable Long periodoId,
                                                  @Valid @RequestBody GuardarRecuperacionesDto datos,
                                                  @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return notasService.guardarRecuperaciones(cargaId, periodoId, datos, usuario);
    }

    @GetMapping("/cargas/{cargaId}/recuperacion-final")
    public RecuperacionFinalDto recuperacionFinal(@PathVariable Long cargaId,
                                                  @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return notasService.recuperacionFinal(cargaId, usuario);
    }

    @PutMapping("/cargas/{cargaId}/recuperacion-final")
    public RecuperacionFinalDto guardarRecuperacionFinal(@PathVariable Long cargaId,
                                                         @Valid @RequestBody GuardarRecuperacionesDto datos,
                                                         @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return notasService.guardarRecuperacionFinal(cargaId, datos, usuario);
    }

    @GetMapping("/grupos/{grupoId}/consolidado")
    public ConsolidadoNotasDto consolidado(@PathVariable Long grupoId, @RequestParam(required = false) Long periodoId,
                                           @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return notasService.consolidado(grupoId, periodoId, usuario);
    }

    /** Boletines del grupo en el periodo en un PDF, un estudiante por pagina; con matriculaId, solo ese. */
    @GetMapping(value = "/grupos/{grupoId}/periodos/{periodoId}/boletines", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> boletines(@PathVariable Long grupoId, @PathVariable Long periodoId,
                                            @RequestParam(required = false) Long matriculaId,
                                            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        DatosBoletin datos = boletinService.datos(grupoId, periodoId, matriculaId, usuario);
        String archivo = ("boletin " + datos.grupo() + " periodo " + datos.periodo() + " " + datos.anio())
                .replaceAll("[^A-Za-z0-9]+", "-").toLowerCase() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(archivo).build().toString())
                .body(boletinPdf.generar(datos));
    }
}
