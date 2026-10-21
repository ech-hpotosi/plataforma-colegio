package co.edu.elencano.plataforma.matricula.servicio;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import co.edu.elencano.plataforma.academico.entidad.AnioLectivo;
import co.edu.elencano.plataforma.academico.entidad.EstadoAnio;
import co.edu.elencano.plataforma.academico.repositorio.AnioLectivoRepository;
import co.edu.elencano.plataforma.academico.servicio.AnioLectivoService;
import co.edu.elencano.plataforma.academico.servicio.GradoService;
import co.edu.elencano.plataforma.academico.servicio.SedeService;
import co.edu.elencano.plataforma.comun.archivos.AlmacenamientoArchivos;
import co.edu.elencano.plataforma.comun.excepcion.RecursoNoEncontradoException;
import co.edu.elencano.plataforma.comun.excepcion.ReglaNegocioException;
import co.edu.elencano.plataforma.estudiantes.entidad.Estudiante;
import co.edu.elencano.plataforma.estudiantes.servicio.EstudianteService;
import co.edu.elencano.plataforma.estudiantes.web.dto.GuardarEstudianteDto;
import co.edu.elencano.plataforma.matricula.entidad.DocumentoAdjunto;
import co.edu.elencano.plataforma.matricula.entidad.EstadoDocumento;
import co.edu.elencano.plataforma.matricula.entidad.EstadoSolicitud;
import co.edu.elencano.plataforma.matricula.entidad.Matricula;
import co.edu.elencano.plataforma.matricula.entidad.SolicitudMatricula;
import co.edu.elencano.plataforma.matricula.entidad.TipoDocumentoRequerido;
import co.edu.elencano.plataforma.matricula.repositorio.SolicitudMatriculaRepository;
import co.edu.elencano.plataforma.matricula.repositorio.TipoDocumentoRequeridoRepository;
import co.edu.elencano.plataforma.matricula.web.dto.DatosSolicitudDto;
import co.edu.elencano.plataforma.matricula.web.dto.OfertaMatriculaDto;
import co.edu.elencano.plataforma.matricula.web.dto.RevisarDocumentoDto;
import co.edu.elencano.plataforma.matricula.web.dto.SolicitudDto;
import co.edu.elencano.plataforma.usuarios.entidad.Rol;
import co.edu.elencano.plataforma.usuarios.entidad.Usuario;
import co.edu.elencano.plataforma.usuarios.seguridad.UsuarioAutenticado;
import co.edu.elencano.plataforma.usuarios.servicio.UsuarioService;

/**
 * Matricula en linea. El acudiente crea la solicitud (solo si el anio esta en etapa de MATRICULA), sube los
 * documentos y la envia; la secretaria revisa cada documento y aprueba, devuelve con observaciones o rechaza.
 * Al aprobar se crea o actualiza la ficha del estudiante, el solicitante queda como acudiente con acceso a
 * consulta y se registra la matricula del anio.
 */
@Service
public class SolicitudMatriculaService {

    /** Formatos aceptados, reconocidos por los primeros bytes del archivo y no por su nombre. */
    private enum Formato {
        PDF("application/pdf", "pdf", new byte[] {'%', 'P', 'D', 'F'}),
        PNG("image/png", "png", new byte[] {(byte) 0x89, 'P', 'N', 'G'}),
        JPG("image/jpeg", "jpg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

        private final String mime;
        private final String extension;
        private final byte[] firma;

        Formato(String mime, String extension, byte[] firma) {
            this.mime = mime;
            this.extension = extension;
            this.firma = firma;
        }
    }

    private final SolicitudMatriculaRepository solicitudRepository;
    private final TipoDocumentoRequeridoRepository tipoRepository;
    private final AnioLectivoRepository anioRepository;
    private final AnioLectivoService anioService;
    private final SedeService sedeService;
    private final GradoService gradoService;
    private final UsuarioService usuarioService;
    private final EstudianteService estudianteService;
    private final MatriculaService matriculaService;
    private final AlmacenamientoArchivos almacenamiento;

    public SolicitudMatriculaService(SolicitudMatriculaRepository solicitudRepository,
                                     TipoDocumentoRequeridoRepository tipoRepository,
                                     AnioLectivoRepository anioRepository, AnioLectivoService anioService,
                                     SedeService sedeService, GradoService gradoService, UsuarioService usuarioService,
                                     EstudianteService estudianteService, MatriculaService matriculaService,
                                     AlmacenamientoArchivos almacenamiento) {
        this.solicitudRepository = solicitudRepository;
        this.tipoRepository = tipoRepository;
        this.anioRepository = anioRepository;
        this.anioService = anioService;
        this.sedeService = sedeService;
        this.gradoService = gradoService;
        this.usuarioService = usuarioService;
        this.estudianteService = estudianteService;
        this.matriculaService = matriculaService;
        this.almacenamiento = almacenamiento;
    }

    /** Anios con la matricula en linea abierta (estado MATRICULA), con sedes y grados para elegir. */
    @Transactional(readOnly = true)
    public List<OfertaMatriculaDto> oferta() {
        List<OfertaMatriculaDto.Opcion> sedes = sedeService.listar().stream()
                .map(s -> new OfertaMatriculaDto.Opcion(s.getId(), s.getNombre())).toList();
        List<OfertaMatriculaDto.Opcion> grados = gradoService.listar().stream()
                .map(g -> new OfertaMatriculaDto.Opcion(g.getId(), g.getNombre())).toList();
        return anioRepository.findAllByOrderByAnioDesc().stream()
                .filter(a -> a.getEstado() == EstadoAnio.MATRICULA)
                .map(a -> new OfertaMatriculaDto(a.getId(), a.getAnio(), sedes, grados))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SolicitudMatricula> listarMias(UsuarioAutenticado usuario) {
        List<SolicitudMatricula> solicitudes = solicitudRepository.listarDeSolicitante(usuario.getId());
        solicitudes.forEach(s -> s.getDocumentos().size());
        return solicitudes;
    }

    @Transactional(readOnly = true)
    public Page<SolicitudMatricula> buscar(Long anioId, EstadoSolicitud estado, String texto, int pagina, int tamano) {
        String filtro = texto == null ? "" : texto.trim().toLowerCase();
        PageRequest paginacion = PageRequest.of(Math.max(pagina, 0), Math.clamp(tamano, 1, 100),
                Sort.by(Sort.Order.asc("fechaEnvio"), Sort.Order.asc("id")));
        Page<SolicitudMatricula> resultado = solicitudRepository.buscar(anioId, estado, filtro, paginacion);
        resultado.forEach(s -> s.getDocumentos().size());
        return resultado;
    }

    @Transactional(readOnly = true)
    public SolicitudDto detalle(Long id, UsuarioAutenticado usuario) {
        return dto(obtenerVisible(id, usuario));
    }

    @Transactional
    public SolicitudDto crear(DatosSolicitudDto datos, UsuarioAutenticado usuario) {
        AnioLectivo anio = anioService.obtener(datos.anioLectivoId());
        if (anio.getEstado() != EstadoAnio.MATRICULA) {
            throw new ReglaNegocioException("La matrícula en línea del año " + anio.getAnio() + " no está abierta");
        }
        Usuario solicitante = usuarioService.obtener(usuario.getId());
        SolicitudMatricula solicitud = new SolicitudMatricula(anio, solicitante, LocalDateTime.now());
        aplicar(solicitud, datos);
        return dto(solicitudRepository.save(solicitud));
    }

    @Transactional
    public SolicitudDto actualizar(Long id, DatosSolicitudDto datos, UsuarioAutenticado usuario) {
        SolicitudMatricula solicitud = obtenerEditable(id, usuario);
        if (!solicitud.getAnioLectivo().getId().equals(datos.anioLectivoId())) {
            throw new ReglaNegocioException("No se puede cambiar el año de la solicitud");
        }
        aplicar(solicitud, datos);
        return dto(solicitud);
    }

    /** Sube o reemplaza el archivo de un tipo de documento. Solo PDF, JPG o PNG. */
    @Transactional
    public SolicitudDto subirDocumento(Long id, Long tipoId, MultipartFile archivo, UsuarioAutenticado usuario) {
        SolicitudMatricula solicitud = obtenerEditable(id, usuario);
        TipoDocumentoRequerido tipo = tipoRepository.findById(tipoId)
                .filter(TipoDocumentoRequerido::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ese documento ya no se pide en la matrícula"));
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("Seleccione un archivo");
        }
        Formato formato = formatoDe(archivo);
        DocumentoAdjunto documento = solicitud.documentoPara(tipo);
        String anterior = documento.getRutaAlmacenamiento();
        String ruta;
        try (InputStream contenido = archivo.getInputStream()) {
            ruta = almacenamiento.guardar("solicitudes/" + solicitud.getId(), formato.extension, contenido);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        documento.reemplazarArchivo(nombreSeguro(archivo.getOriginalFilename(), formato), ruta, formato.mime,
                archivo.getSize(), LocalDateTime.now());
        if (anterior != null) {
            eliminarAlConfirmar(anterior);
        }
        return dto(solicitud);
    }

    @Transactional
    public SolicitudDto quitarDocumento(Long id, Long tipoId, UsuarioAutenticado usuario) {
        SolicitudMatricula solicitud = obtenerEditable(id, usuario);
        DocumentoAdjunto documento = solicitud.documentoDe(tipoId);
        if (documento != null) {
            String ruta = documento.getRutaAlmacenamiento();
            solicitud.quitarDocumento(documento);
            eliminarAlConfirmar(ruta);
        }
        return dto(solicitud);
    }

    /** Archivo de un documento, para el acudiente que hizo la solicitud o para la secretaria. */
    @Transactional(readOnly = true)
    public ArchivoDocumento archivo(Long id, Long documentoId, UsuarioAutenticado usuario) {
        SolicitudMatricula solicitud = obtenerVisible(id, usuario);
        DocumentoAdjunto documento = solicitud.getDocumentos().stream()
                .filter(d -> d.getId().equals(documentoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el documento " + documentoId));
        return new ArchivoDocumento(documento.getNombreOriginal(), documento.getTipoMime(),
                almacenamiento.leer(documento.getRutaAlmacenamiento()));
    }

    public record ArchivoDocumento(String nombre, String tipoMime, Resource contenido) {
    }

    /** El acudiente envia la solicitud a revision. Deben estar todos los documentos obligatorios. */
    @Transactional
    public SolicitudDto enviar(Long id, UsuarioAutenticado usuario) {
        SolicitudMatricula solicitud = obtenerEditable(id, usuario);
        if (solicitud.getAnioLectivo().getEstado() != EstadoAnio.MATRICULA) {
            throw new ReglaNegocioException("La matrícula en línea del año " + solicitud.getAnioLectivo().getAnio()
                    + " ya está cerrada. Comuníquese con la secretaría");
        }
        List<String> faltantes = tipoRepository.findByActivoTrueOrderByOrdenAscNombreAsc().stream()
                .filter(TipoDocumentoRequerido::isObligatorio)
                .filter(t -> solicitud.documentoDe(t.getId()) == null)
                .map(TipoDocumentoRequerido::getNombre)
                .toList();
        if (!faltantes.isEmpty()) {
            throw new ReglaNegocioException("Faltan documentos: " + String.join(", ", faltantes));
        }
        boolean hayRechazados = solicitud.getDocumentos().stream()
                .anyMatch(d -> d.getEstado() == EstadoDocumento.RECHAZADO);
        if (hayRechazados) {
            throw new ReglaNegocioException("Reemplace los documentos que la secretaría marcó como rechazados");
        }
        LocalDateTime ahora = LocalDateTime.now();
        solicitud.autorizarDatos(ahora);
        solicitud.cambiarEstado(EstadoSolicitud.ENVIADA, solicitud.getSolicitante(), ahora, null);
        return dto(solicitud);
    }

    @Transactional
    public SolicitudDto revisarDocumento(Long id, Long documentoId, RevisarDocumentoDto datos,
                                         UsuarioAutenticado usuario) {
        SolicitudMatricula solicitud = obtenerEnRevision(id);
        if (datos.estado() == EstadoDocumento.PENDIENTE) {
            throw new ReglaNegocioException("Indique si el documento se aprueba o se rechaza");
        }
        String observacion = StringUtils.hasText(datos.observacion()) ? datos.observacion().trim() : null;
        if (datos.estado() == EstadoDocumento.RECHAZADO && observacion == null) {
            throw new ReglaNegocioException("Escriba por qué se rechaza el documento, para que el acudiente lo corrija");
        }
        DocumentoAdjunto documento = solicitud.getDocumentos().stream()
                .filter(d -> d.getId().equals(documentoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el documento " + documentoId));
        documento.revisar(datos.estado(), observacion);
        return dto(solicitud);
    }

    /** Devuelve la solicitud al acudiente para que corrija datos o documentos. */
    @Transactional
    public SolicitudDto devolver(Long id, String comentario, UsuarioAutenticado usuario) {
        SolicitudMatricula solicitud = obtenerEnRevision(id);
        solicitud.cambiarEstado(EstadoSolicitud.CON_OBSERVACIONES, usuarioService.obtener(usuario.getId()),
                LocalDateTime.now(), comentario.trim());
        return dto(solicitud);
    }

    @Transactional
    public SolicitudDto rechazar(Long id, String comentario, UsuarioAutenticado usuario) {
        SolicitudMatricula solicitud = obtenerEnRevision(id);
        solicitud.cambiarEstado(EstadoSolicitud.RECHAZADA, usuarioService.obtener(usuario.getId()),
                LocalDateTime.now(), comentario.trim());
        return dto(solicitud);
    }

    /**
     * Aprueba la solicitud: crea la ficha del estudiante (o actualiza la del antiguo, buscado por numero de
     * documento), vincula al solicitante como acudiente con acceso a consulta y lo matricula en el anio.
     * Los documentos que seguian pendientes quedan aprobados.
     */
    @Transactional
    public SolicitudDto aprobar(Long id, Long grupoId, UsuarioAutenticado usuario) {
        SolicitudMatricula solicitud = obtenerEnRevision(id);
        if (solicitud.getDocumentos().stream().anyMatch(d -> d.getEstado() == EstadoDocumento.RECHAZADO)) {
            throw new ReglaNegocioException("Hay documentos rechazados. Devuelva la solicitud para que los corrijan");
        }
        Estudiante estudiante = estudianteService.buscarPorNumeroDocumento(solicitud.getNumeroDocumento())
                .map(existente -> {
                    estudianteService.actualizar(existente.getId(), fichaDe(solicitud, existente));
                    return existente;
                })
                .orElseGet(() -> estudianteService.obtener(estudianteService.crear(fichaDe(solicitud, null)).id()));
        estudianteService.vincularSolicitante(estudiante, solicitud.getSolicitante().getPersona(),
                solicitud.getParentesco());
        Matricula matricula = matriculaService.matricular(estudiante.getId(), solicitud.getAnioLectivo().getId(),
                grupoId);
        solicitud.getDocumentos().stream()
                .filter(d -> d.getEstado() == EstadoDocumento.PENDIENTE)
                .forEach(d -> d.revisar(EstadoDocumento.APROBADO, null));
        solicitud.registrarAprobacion(estudiante, matricula);
        solicitud.cambiarEstado(EstadoSolicitud.APROBADA, usuarioService.obtener(usuario.getId()),
                LocalDateTime.now(), null);
        return dto(solicitud);
    }

    private static GuardarEstudianteDto fichaDe(SolicitudMatricula s, Estudiante existente) {
        String codigo = existente == null ? null : existente.getCodigo();
        boolean tienePiar = existente != null && existente.isTienePiar();
        return new GuardarEstudianteDto(s.getTipoDocumento(), s.getNumeroDocumento(), s.getNombres(),
                s.getApellidos(), s.getTelefono(), s.getCorreo(), codigo, s.getFechaNacimiento(), s.getGenero(),
                s.getDireccion(), s.getEps(), s.getGrupoSanguineo(), s.getCondicionDiscapacidad(), tienePiar,
                s.getCondicionesEspeciales());
    }

    private void aplicar(SolicitudMatricula solicitud, DatosSolicitudDto datos) {
        String documento = datos.numeroDocumento().trim();
        if (documento.equals(solicitud.getSolicitante().getPersona().getNumeroDocumento())) {
            throw new ReglaNegocioException("El documento del estudiante no puede ser el mismo del acudiente");
        }
        Long excluir = solicitud.getId() == null ? 0L : solicitud.getId();
        if (solicitudRepository.existeVigente(solicitud.getAnioLectivo().getId(), documento, excluir)) {
            throw new ReglaNegocioException("Ya hay una solicitud de matrícula " + solicitud.getAnioLectivo().getAnio()
                    + " para el estudiante con documento " + documento);
        }
        solicitud.setSede(sedeService.obtener(datos.sedeId()));
        solicitud.setGrado(gradoService.obtener(datos.gradoId()));
        solicitud.setTipo(datos.tipo());
        solicitud.setParentesco(datos.parentesco());
        solicitud.setTipoDocumento(datos.tipoDocumento());
        solicitud.setNumeroDocumento(documento);
        solicitud.setNombres(datos.nombres().trim());
        solicitud.setApellidos(datos.apellidos().trim());
        solicitud.setFechaNacimiento(datos.fechaNacimiento());
        solicitud.setGenero(datos.genero());
        solicitud.setDireccion(vacioANulo(datos.direccion()));
        solicitud.setTelefono(vacioANulo(datos.telefono()));
        solicitud.setCorreo(vacioANulo(datos.correo()));
        solicitud.setEps(vacioANulo(datos.eps()));
        solicitud.setGrupoSanguineo(vacioANulo(datos.grupoSanguineo()));
        solicitud.setCondicionDiscapacidad(vacioANulo(datos.condicionDiscapacidad()));
        solicitud.setCondicionesEspeciales(vacioANulo(datos.condicionesEspeciales()));
        solicitud.setInstitucionAnterior(vacioANulo(datos.institucionAnterior()));
    }

    private SolicitudDto dto(SolicitudMatricula solicitud) {
        return SolicitudDto.de(solicitud, tipoRepository.findByActivoTrueOrderByOrdenAscNombreAsc());
    }

    private SolicitudMatricula obtener(Long id) {
        return solicitudRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la solicitud " + id));
    }

    /** La ve quien la hizo, la secretaria y los directivos. A otros se les responde como si no existiera. */
    private SolicitudMatricula obtenerVisible(Long id, UsuarioAutenticado usuario) {
        SolicitudMatricula solicitud = obtener(id);
        boolean propia = solicitud.getSolicitante().getId().equals(usuario.getId());
        boolean revisora = usuario.tieneRol(Rol.ADMINISTRADOR) || usuario.tieneRol(Rol.SECRETARIA)
                || usuario.tieneRol(Rol.RECTOR) || usuario.tieneRol(Rol.COORDINADOR_ACADEMICO);
        boolean borradorAjeno = !propia && solicitud.getEstado() == EstadoSolicitud.BORRADOR;
        if ((!propia && !revisora) || borradorAjeno) {
            throw new RecursoNoEncontradoException("No existe la solicitud " + id);
        }
        return solicitud;
    }

    private SolicitudMatricula obtenerEditable(Long id, UsuarioAutenticado usuario) {
        SolicitudMatricula solicitud = obtener(id);
        if (!solicitud.getSolicitante().getId().equals(usuario.getId())) {
            throw new AccessDeniedException("La solicitud es de otro usuario");
        }
        if (!solicitud.getEstado().editablePorAcudiente()) {
            throw new ReglaNegocioException("La solicitud ya fue enviada y no se puede modificar");
        }
        return solicitud;
    }

    private SolicitudMatricula obtenerEnRevision(Long id) {
        SolicitudMatricula solicitud = obtener(id);
        if (solicitud.getEstado() != EstadoSolicitud.ENVIADA) {
            throw new ReglaNegocioException("Solo se revisan solicitudes enviadas por el acudiente");
        }
        return solicitud;
    }

    /** Borra el archivo solo si la transaccion termina bien, para no perderlo si algo falla despues. */
    private void eliminarAlConfirmar(String ruta) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                almacenamiento.eliminar(ruta);
            }
        });
    }

    private static Formato formatoDe(MultipartFile archivo) {
        byte[] inicio;
        try (InputStream entrada = archivo.getInputStream()) {
            inicio = entrada.readNBytes(4);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        for (Formato formato : Formato.values()) {
            if (inicio.length >= formato.firma.length
                    && Arrays.equals(Arrays.copyOf(inicio, formato.firma.length), formato.firma)) {
                return formato;
            }
        }
        throw new ReglaNegocioException("El archivo debe ser PDF, JPG o PNG");
    }

    /** Conserva el nombre para mostrarlo, sin rutas ni caracteres raros, y con la extension real. */
    private static String nombreSeguro(String original, Formato formato) {
        String nombre = original == null ? "" : original.replaceAll("^.*[/\\\\]", "");
        nombre = nombre.replaceAll("[^\\p{L}\\p{N} ._()-]", "").trim();
        int punto = nombre.lastIndexOf('.');
        String base = punto > 0 ? nombre.substring(0, punto) : nombre;
        if (base.isBlank()) {
            base = "documento";
        }
        if (base.length() > 150) {
            base = base.substring(0, 150);
        }
        return base + "." + formato.extension;
    }

    private static String vacioANulo(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }
}
