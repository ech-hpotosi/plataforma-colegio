package co.edu.elencano.plataforma.asistencia.repositorio;

import co.edu.elencano.plataforma.asistencia.entidad.EstadoAsistencia;

/** Horas de un estudiante en una clase y periodo con un mismo estado de inasistencia, para el boletin. */
public record FaltasPeriodo(Long matriculaId, Long cargaId, EstadoAsistencia estado, Long horas) {
}
