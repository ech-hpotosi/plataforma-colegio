package co.edu.elencano.plataforma.notas.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import co.edu.elencano.plataforma.notas.entidad.Desempeno;

/**
 * Planilla de una clase en un periodo: actividades como columnas y una fila por estudiante.
 * notas va por id de actividad; saber, hacer y ser son los promedios de cada dimension.
 * notaPeriodo es la calculada; notaDefinitiva aplica la recuperacion, y el desempeno es el de la definitiva.
 */
public record PlanillaNotasDto(Long cargaId, String grupo, String asignatura, Long periodoId, int periodo,
                               boolean editable, boolean admiteRecuperacion, ConfiguracionEvaluacionDto configuracion,
                               List<ActividadDto> actividades, List<Fila> estudiantes) {

    public record Fila(Long matriculaId, String nombres, String apellidos, Map<Long, BigDecimal> notas,
                       BigDecimal saber, BigDecimal hacer, BigDecimal ser, BigDecimal notaPeriodo,
                       BigDecimal recuperacion, String observacionRecuperacion, BigDecimal notaDefinitiva,
                       Desempeno desempeno, boolean completa) {
    }
}
