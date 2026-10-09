package co.edu.elencano.plataforma.seguimiento.web.dto;

import java.time.LocalDate;
import java.util.List;

import co.edu.elencano.plataforma.notas.entidad.Dimension;

/**
 * Avance del registro de notas y asistencia por clase en un periodo.
 * - porcentajeNotas: notas registradas sobre actividades por estudiantes; no llega a 100 mientras falte crear
 *   actividades en alguna dimension que pesa.
 * - diasSinAsistencia: dias desde la ultima asistencia registrada (o desde el inicio del periodo si no hay),
 *   contados hasta hoy o hasta el cierre del periodo si ya paso.
 */
public record AvanceRegistroDto(LocalDate hoy, List<Clase> clases) {

    public record Clase(Long cargaId, int anio, String sede, String grupo, String asignatura, String docente,
                        Long periodoId, int periodoNumero, LocalDate inicioPeriodo, LocalDate cierrePeriodo,
                        boolean periodoCerrado, int estudiantes, boolean cualitativa, int actividades,
                        List<Dimension> dimensionesSinActividad, long notasRegistradas, long notasEsperadas,
                        int porcentajeNotas, long diasConAsistencia, LocalDate ultimaAsistencia,
                        long diasSinAsistencia) {
    }
}
