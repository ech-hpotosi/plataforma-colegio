package co.edu.elencano.plataforma.notas.web.dto;

import java.math.BigDecimal;
import java.util.List;

import co.edu.elencano.plataforma.notas.entidad.Desempeno;

/**
 * Notas de un grupo: una fila por estudiante y una columna por asignatura.
 * Si periodoId es nulo son las notas acumuladas del anio, ponderadas por el porcentaje de cada periodo.
 */
public record ConsolidadoNotasDto(Long grupoId, String grupo, Long periodoId, List<PeriodoNotasDto> periodos,
                                  ConfiguracionEvaluacionDto configuracion, List<Asignatura> asignaturas,
                                  List<Estudiante> estudiantes) {

    public record Asignatura(Long cargaId, String nombre, String docente) {
    }

    public record Nota(Long cargaId, BigDecimal nota, Desempeno desempeno, boolean completa) {
    }

    public record Estudiante(Long matriculaId, String nombres, String apellidos, List<Nota> notas,
                             int asignaturasEnBajo) {
    }
}
