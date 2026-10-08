package co.edu.elencano.plataforma.asistencia.web.dto;

/** Asignatura del plan del grupo con las horas del anio (intensidad semanal por semanas lectivas). */
public record AsignaturaResumenDto(Long asignaturaId, String nombre, int horasAnuales) {
}
