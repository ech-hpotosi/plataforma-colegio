package co.edu.elencano.plataforma.asistencia.web.dto;

import java.math.BigDecimal;

/**
 * Horas de inasistencia de un estudiante en una asignatura. El porcentaje es de las horas sin justificar
 * frente a las horas del anio; superaLimite indica que pasa el maximo del SIEE.
 */
public record InasistenciaAsignaturaDto(Long asignaturaId, long horasSinJustificar, long horasJustificadas,
                                        long horasRetardo, BigDecimal porcentaje, boolean superaLimite) {
}
