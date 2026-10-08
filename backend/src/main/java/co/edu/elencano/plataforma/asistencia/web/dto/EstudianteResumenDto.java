package co.edu.elencano.plataforma.asistencia.web.dto;

import java.util.List;

/** Inasistencias de un estudiante; solo trae las asignaturas en que tiene alguna novedad. */
public record EstudianteResumenDto(Long matriculaId, String nombres, String apellidos,
                                   List<InasistenciaAsignaturaDto> asignaturas, boolean superaLimite) {
}
