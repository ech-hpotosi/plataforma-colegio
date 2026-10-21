package co.edu.elencano.plataforma.matricula.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import co.edu.elencano.plataforma.estudiantes.entidad.Genero;
import co.edu.elencano.plataforma.estudiantes.entidad.Parentesco;
import co.edu.elencano.plataforma.matricula.entidad.DocumentoAdjunto;
import co.edu.elencano.plataforma.matricula.entidad.EstadoDocumento;
import co.edu.elencano.plataforma.matricula.entidad.EstadoSolicitud;
import co.edu.elencano.plataforma.matricula.entidad.SolicitudMatricula;
import co.edu.elencano.plataforma.matricula.entidad.TipoDocumentoRequerido;
import co.edu.elencano.plataforma.matricula.entidad.TipoSolicitud;
import co.edu.elencano.plataforma.usuarios.entidad.Persona;
import co.edu.elencano.plataforma.usuarios.entidad.TipoDocumento;

/**
 * Solicitud completa. En documentos aparece cada documento que se pide hoy (con o sin archivo) y los
 * archivos de documentos que ya no se piden, para que la secretaria los siga viendo.
 */
public record SolicitudDto(Long id, Long anioLectivoId, int anio, Long sedeId, String sede, Long gradoId,
                           String grado, TipoSolicitud tipo, Parentesco parentesco, EstadoSolicitud estado,
                           boolean editable, TipoDocumento tipoDocumento, String numeroDocumento, String nombres,
                           String apellidos, LocalDate fechaNacimiento, Genero genero, String direccion,
                           String telefono, String correo, String eps, String grupoSanguineo,
                           String condicionDiscapacidad, String condicionesEspeciales, String institucionAnterior,
                           boolean autorizaDatos, LocalDateTime fechaCreacion, LocalDateTime fechaEnvio,
                           String observacion, Solicitante solicitante, Long estudianteId, String grupo,
                           List<Documento> documentos, List<Revision> revisiones) {

    public record Solicitante(String nombre, TipoDocumento tipoDocumento, String numeroDocumento, String telefono,
                              String correo) {
    }

    /** Documento pedido. Los datos del archivo son nulos si todavia no se ha subido. */
    public record Documento(Long tipoDocumentoId, String nombre, String descripcion, boolean obligatorio,
                            Long id, String nombreArchivo, String tipoMime, Long tamanoBytes,
                            LocalDateTime fechaCarga, EstadoDocumento estado, String observacion) {
    }

    public record Revision(LocalDateTime fecha, String usuario, EstadoSolicitud estadoAnterior,
                           EstadoSolicitud estadoNuevo, String comentario) {
    }

    public static SolicitudDto de(SolicitudMatricula s, List<TipoDocumentoRequerido> tiposActivos) {
        List<Documento> documentos = new ArrayList<>();
        for (TipoDocumentoRequerido tipo : tiposActivos) {
            documentos.add(documento(tipo, s.documentoDe(tipo.getId())));
        }
        s.getDocumentos().stream()
                .filter(d -> !d.getTipoDocumento().isActivo())
                .sorted(Comparator.comparingInt(d -> d.getTipoDocumento().getOrden()))
                .forEach(d -> documentos.add(documento(d.getTipoDocumento(), d)));
        List<Revision> revisiones = s.getRevisiones().stream()
                .map(r -> new Revision(r.getFecha(), r.getUsuario().getPersona().getNombreCompleto(),
                        r.getEstadoAnterior(), r.getEstadoNuevo(), r.getComentario()))
                .toList();
        Persona p = s.getSolicitante().getPersona();
        Solicitante solicitante = new Solicitante(p.getNombreCompleto(), p.getTipoDocumento(), p.getNumeroDocumento(),
                p.getTelefono(), p.getCorreo());
        String grupo = s.getMatricula() == null || s.getMatricula().getGrupo() == null ? null
                : s.getMatricula().getGrupo().getGrado().getNombre() + " " + s.getMatricula().getGrupo().getNombre();
        return new SolicitudDto(s.getId(), s.getAnioLectivo().getId(), s.getAnioLectivo().getAnio(),
                s.getSede().getId(), s.getSede().getNombre(), s.getGrado().getId(), s.getGrado().getNombre(),
                s.getTipo(), s.getParentesco(), s.getEstado(), s.getEstado().editablePorAcudiente(),
                s.getTipoDocumento(), s.getNumeroDocumento(), s.getNombres(), s.getApellidos(),
                s.getFechaNacimiento(), s.getGenero(), s.getDireccion(), s.getTelefono(), s.getCorreo(), s.getEps(),
                s.getGrupoSanguineo(), s.getCondicionDiscapacidad(), s.getCondicionesEspeciales(),
                s.getInstitucionAnterior(), s.isAutorizaDatos(), s.getFechaCreacion(), s.getFechaEnvio(),
                s.getObservacion(), solicitante, s.getEstudiante() == null ? null : s.getEstudiante().getId(), grupo,
                documentos, revisiones);
    }

    private static Documento documento(TipoDocumentoRequerido tipo, DocumentoAdjunto d) {
        if (d == null) {
            return new Documento(tipo.getId(), tipo.getNombre(), tipo.getDescripcion(), tipo.isObligatorio(),
                    null, null, null, null, null, null, null);
        }
        return new Documento(tipo.getId(), tipo.getNombre(), tipo.getDescripcion(), tipo.isObligatorio(),
                d.getId(), d.getNombreOriginal(), d.getTipoMime(), d.getTamanoBytes(), d.getFechaCarga(),
                d.getEstado(), d.getObservacion());
    }
}
