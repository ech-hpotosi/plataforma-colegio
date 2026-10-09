package co.edu.elencano.plataforma.notas.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import co.edu.elencano.plataforma.notas.entidad.Desempeno;

/**
 * Lo que una clase aporta al boletin del periodo: el concepto descriptivo de cada desempeno y, por estudiante,
 * su nota definitiva con el desempeno, la valoracion del comportamiento y la observacion.
 */
public record InformePeriodoDto(Long cargaId, String grupo, String asignatura, Long periodoId, int periodo,
                                boolean editable, ConfiguracionEvaluacionDto configuracion,
                                Map<Desempeno, String> descriptores, List<Fila> estudiantes) {

    public record Fila(Long matriculaId, String nombres, String apellidos, BigDecimal notaDefinitiva,
                       Desempeno desempeno, boolean completa, BigDecimal comportamiento, String observacion) {
    }
}
