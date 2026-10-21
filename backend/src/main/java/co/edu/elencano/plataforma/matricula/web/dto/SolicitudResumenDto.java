package co.edu.elencano.plataforma.matricula.web.dto;

import java.time.LocalDateTime;

import co.edu.elencano.plataforma.matricula.entidad.EstadoDocumento;
import co.edu.elencano.plataforma.matricula.entidad.EstadoSolicitud;
import co.edu.elencano.plataforma.matricula.entidad.SolicitudMatricula;
import co.edu.elencano.plataforma.matricula.entidad.TipoSolicitud;

/** Fila del listado de solicitudes de la secretaria y del acudiente. */
public record SolicitudResumenDto(Long id, int anio, String estudiante, String numeroDocumento, String sede,
                                  String grado, TipoSolicitud tipo, EstadoSolicitud estado, LocalDateTime fechaEnvio,
                                  String solicitante, long documentosPorRevisar) {

    public static SolicitudResumenDto de(SolicitudMatricula s) {
        long porRevisar = s.getDocumentos().stream().filter(d -> d.getEstado() == EstadoDocumento.PENDIENTE).count();
        return new SolicitudResumenDto(s.getId(), s.getAnioLectivo().getAnio(), s.getNombres() + " " + s.getApellidos(),
                s.getNumeroDocumento(), s.getSede().getNombre(), s.getGrado().getNombre(), s.getTipo(), s.getEstado(),
                s.getFechaEnvio(), s.getSolicitante().getPersona().getNombreCompleto(), porRevisar);
    }
}
