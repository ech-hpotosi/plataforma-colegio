package co.edu.elencano.plataforma.asistencia.web.dto;

import java.math.BigDecimal;
import java.util.List;

/** Consolidado de inasistencias del anio para un grupo. */
public record ResumenGrupoDto(Long grupoId, String grupo, int semanasLectivas, BigDecimal porcentajeMaximo,
                              List<AsignaturaResumenDto> asignaturas, List<EstudianteResumenDto> estudiantes) {
}
