package co.edu.elencano.plataforma.asistencia.repositorio;

import co.edu.elencano.plataforma.asistencia.entidad.EstadoAsistencia;

/** Horas acumuladas de un estudiante en una asignatura con un mismo estado de asistencia. */
public record ConteoInasistencia(Long matriculaId, Long asignaturaId, EstadoAsistencia estado, Long horas) {
}
