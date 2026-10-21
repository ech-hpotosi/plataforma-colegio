package co.edu.elencano.plataforma.matricula.web;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import co.edu.elencano.plataforma.comun.web.Pagina;
import co.edu.elencano.plataforma.estudiantes.web.ConsultaEstudiantes;
import co.edu.elencano.plataforma.estudiantes.web.GestionEstudiantes;
import co.edu.elencano.plataforma.matricula.entidad.EstadoSolicitud;
import co.edu.elencano.plataforma.matricula.servicio.SolicitudMatriculaService;
import co.edu.elencano.plataforma.matricula.servicio.SolicitudMatriculaService.ArchivoDocumento;
import co.edu.elencano.plataforma.matricula.servicio.TipoDocumentoRequeridoService;
import co.edu.elencano.plataforma.matricula.web.dto.AprobarSolicitudDto;
import co.edu.elencano.plataforma.matricula.web.dto.ComentarioSolicitudDto;
import co.edu.elencano.plataforma.matricula.web.dto.DatosSolicitudDto;
import co.edu.elencano.plataforma.matricula.web.dto.EnviarSolicitudDto;
import co.edu.elencano.plataforma.matricula.web.dto.GuardarTipoDocumentoDto;
import co.edu.elencano.plataforma.matricula.web.dto.OfertaMatriculaDto;
import co.edu.elencano.plataforma.matricula.web.dto.RevisarDocumentoDto;
import co.edu.elencano.plataforma.matricula.web.dto.SolicitudDto;
import co.edu.elencano.plataforma.matricula.web.dto.SolicitudResumenDto;
import co.edu.elencano.plataforma.matricula.web.dto.TipoDocumentoRequeridoDto;
import co.edu.elencano.plataforma.usuarios.seguridad.UsuarioAutenticado;
import jakarta.validation.Valid;

/**
 * Matricula en linea. El acudiente diligencia y envia sus solicitudes; la secretaria las revisa.
 * El servicio verifica que cada acudiente solo vea y cambie las suyas.
 */
@RestController
public class SolicitudMatriculaController {

    private final SolicitudMatriculaService solicitudService;
    private final TipoDocumentoRequeridoService tipoService;

    public SolicitudMatriculaController(SolicitudMatriculaService solicitudService,
                                        TipoDocumentoRequeridoService tipoService) {
        this.solicitudService = solicitudService;
        this.tipoService = tipoService;
    }

    @GetMapping("/api/matricula-en-linea/oferta")
    public List<OfertaMatriculaDto> oferta() {
        return solicitudService.oferta();
    }

    @GetMapping("/api/tipos-documento-matricula")
    public List<TipoDocumentoRequeridoDto> tiposDocumento(@RequestParam(defaultValue = "true") boolean soloActivos) {
        return tipoService.listar(soloActivos).stream().map(TipoDocumentoRequeridoDto::de).toList();
    }

    @PostMapping("/api/tipos-documento-matricula")
    @ResponseStatus(HttpStatus.CREATED)
    @GestionEstudiantes
    public TipoDocumentoRequeridoDto crearTipo(@Valid @RequestBody GuardarTipoDocumentoDto datos) {
        return TipoDocumentoRequeridoDto.de(tipoService.crear(datos));
    }

    @PutMapping("/api/tipos-documento-matricula/{id}")
    @GestionEstudiantes
    public TipoDocumentoRequeridoDto actualizarTipo(@PathVariable Long id,
                                                    @Valid @RequestBody GuardarTipoDocumentoDto datos) {
        return TipoDocumentoRequeridoDto.de(tipoService.actualizar(id, datos));
    }

    @GetMapping("/api/solicitudes-matricula/mias")
    @PreAuthorize("hasRole('ACUDIENTE')")
    public List<SolicitudResumenDto> mias(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return solicitudService.listarMias(usuario).stream().map(SolicitudResumenDto::de).toList();
    }

    @GetMapping("/api/solicitudes-matricula")
    @ConsultaEstudiantes
    public Pagina<SolicitudResumenDto> buscar(@RequestParam Long anioId,
                                              @RequestParam(required = false) EstadoSolicitud estado,
                                              @RequestParam(required = false) String buscar,
                                              @RequestParam(defaultValue = "0") int pagina,
                                              @RequestParam(defaultValue = "25") int tamano) {
        return Pagina.de(solicitudService.buscar(anioId, estado, buscar, pagina, tamano), SolicitudResumenDto::de);
    }

    @GetMapping("/api/solicitudes-matricula/{id}")
    public SolicitudDto detalle(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return solicitudService.detalle(id, usuario);
    }

    @PostMapping("/api/solicitudes-matricula")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ACUDIENTE')")
    public SolicitudDto crear(@Valid @RequestBody DatosSolicitudDto datos,
                              @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return solicitudService.crear(datos, usuario);
    }

    @PutMapping("/api/solicitudes-matricula/{id}")
    @PreAuthorize("hasRole('ACUDIENTE')")
    public SolicitudDto actualizar(@PathVariable Long id, @Valid @RequestBody DatosSolicitudDto datos,
                                   @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return solicitudService.actualizar(id, datos, usuario);
    }

    @PostMapping(value = "/api/solicitudes-matricula/{id}/documentos/{tipoId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ACUDIENTE')")
    public SolicitudDto subirDocumento(@PathVariable Long id, @PathVariable Long tipoId,
                                       @RequestParam("archivo") MultipartFile archivo,
                                       @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return solicitudService.subirDocumento(id, tipoId, archivo, usuario);
    }

    @DeleteMapping("/api/solicitudes-matricula/{id}/documentos/{tipoId}")
    @PreAuthorize("hasRole('ACUDIENTE')")
    public SolicitudDto quitarDocumento(@PathVariable Long id, @PathVariable Long tipoId,
                                        @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return solicitudService.quitarDocumento(id, tipoId, usuario);
    }

    /** Muestra el archivo en el navegador. nosniff evita que se interprete como otro tipo de contenido. */
    @GetMapping("/api/solicitudes-matricula/{id}/archivos/{documentoId}")
    public ResponseEntity<Resource> archivo(@PathVariable Long id, @PathVariable Long documentoId,
                                            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        ArchivoDocumento archivo = solicitudService.archivo(id, documentoId, usuario);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.tipoMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(archivo.nombre(), StandardCharsets.UTF_8)
                                .build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(archivo.contenido());
    }

    @PostMapping("/api/solicitudes-matricula/{id}/enviar")
    @PreAuthorize("hasRole('ACUDIENTE')")
    public SolicitudDto enviar(@PathVariable Long id, @Valid @RequestBody EnviarSolicitudDto datos,
                               @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return solicitudService.enviar(id, usuario);
    }

    @PutMapping("/api/solicitudes-matricula/{id}/archivos/{documentoId}/revision")
    @GestionEstudiantes
    public SolicitudDto revisarDocumento(@PathVariable Long id, @PathVariable Long documentoId,
                                         @Valid @RequestBody RevisarDocumentoDto datos,
                                         @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return solicitudService.revisarDocumento(id, documentoId, datos, usuario);
    }

    @PostMapping("/api/solicitudes-matricula/{id}/devolver")
    @GestionEstudiantes
    public SolicitudDto devolver(@PathVariable Long id, @Valid @RequestBody ComentarioSolicitudDto datos,
                                 @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return solicitudService.devolver(id, datos.comentario(), usuario);
    }

    @PostMapping("/api/solicitudes-matricula/{id}/rechazar")
    @GestionEstudiantes
    public SolicitudDto rechazar(@PathVariable Long id, @Valid @RequestBody ComentarioSolicitudDto datos,
                                 @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return solicitudService.rechazar(id, datos.comentario(), usuario);
    }

    @PostMapping("/api/solicitudes-matricula/{id}/aprobar")
    @GestionEstudiantes
    public SolicitudDto aprobar(@PathVariable Long id, @RequestBody AprobarSolicitudDto datos,
                                @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return solicitudService.aprobar(id, datos.grupoId(), usuario);
    }
}
