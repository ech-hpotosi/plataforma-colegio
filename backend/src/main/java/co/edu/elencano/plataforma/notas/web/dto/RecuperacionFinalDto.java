package co.edu.elencano.plataforma.notas.web.dto;

import java.math.BigDecimal;
import java.util.List;

import co.edu.elencano.plataforma.notas.entidad.Desempeno;

/**
 * Recuperacion final de una clase: nota del anio (con las recuperaciones de periodo ya aplicadas),
 * recuperacion final y nota definitiva del anio.
 */
public record RecuperacionFinalDto(Long cargaId, String grupo, String asignatura, boolean editable,
                                   ConfiguracionEvaluacionDto configuracion, List<Fila> estudiantes) {

    public record Fila(Long matriculaId, String nombres, String apellidos, BigDecimal notaAnio, boolean completa,
                       BigDecimal recuperacion, String observacion, BigDecimal notaDefinitiva, Desempeno desempeno) {
    }
}
