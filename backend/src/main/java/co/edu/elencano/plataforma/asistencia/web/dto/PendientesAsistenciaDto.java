package co.edu.elencano.plataforma.asistencia.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Lo que el usuario tiene pendiente de asistencia, para la pantalla de inicio.
 * Cada lista llega vacia si el usuario no tiene ese tipo de pendiente.
 */
public record PendientesAsistenciaDto(BigDecimal porcentajeMaximo, List<ClaseHoy> clasesHoy,
                                      List<FaltaPorJustificar> faltasPorJustificar,
                                      List<EstudianteEnRiesgo> estudiantesEnRiesgo) {

    /** Clase del docente en un periodo vigente y si ya tiene asistencia de hoy. */
    public record ClaseHoy(Long cargaId, String sede, String grupo, String asignatura, boolean registrada) {
    }

    /** Faltas sin justificar de un estudiante en un dia, todavia dentro del plazo del SIEE. */
    public record FaltaPorJustificar(Long matriculaId, Long grupoId, String estudiante, String grupo, LocalDate fecha,
                                     long horas, LocalDate fechaLimite) {
    }

    /** Estudiante que supera o se acerca al maximo de inasistencia en su asignatura con mas faltas. */
    public record EstudianteEnRiesgo(Long matriculaId, Long grupoId, String estudiante, String grupo, String asignatura,
                                     BigDecimal porcentaje, boolean superaLimite) {
    }
}
