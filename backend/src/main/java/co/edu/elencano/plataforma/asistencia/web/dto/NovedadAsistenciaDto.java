package co.edu.elencano.plataforma.asistencia.web.dto;

import java.time.LocalDate;

import co.edu.elencano.plataforma.asistencia.entidad.EstadoAsistencia;

/** Falta, permiso o retardo de un estudiante en una clase. fechaLimite es el ultimo dia para justificarla. */
public record NovedadAsistenciaDto(Long detalleId, LocalDate fecha, String asignatura, int horas,
                                   EstadoAsistencia estado, String observacion, String justificacion,
                                   LocalDate fechaLimite) {
}
