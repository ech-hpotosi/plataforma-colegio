package co.edu.elencano.plataforma.familia.web.dto;

import java.math.BigDecimal;
import java.util.List;

import co.edu.elencano.plataforma.asistencia.web.dto.NovedadAsistenciaDto;
import co.edu.elencano.plataforma.notas.entidad.Desempeno;
import co.edu.elencano.plataforma.notas.web.dto.ConfiguracionEvaluacionDto;
import co.edu.elencano.plataforma.notas.web.dto.PeriodoNotasDto;

/**
 * Notas y asistencia de un estudiante en un anio. Las notas de un periodo abierto son parciales: cambian a
 * medida que los docentes registran actividades. Los boletines se pueden descargar de los periodos cerrados.
 * En Transicion (cualitativo) cada asignatura es una dimension con informes descriptivos en lugar de notas.
 */
public record ResumenFamiliaDto(Long estudianteId, String estudiante, Long matriculaId, int anio, String grupo,
                                String sede, String director, ConfiguracionEvaluacionDto escala,
                                List<PeriodoNotasDto> periodos, List<Asignatura> asignaturas,
                                List<NovedadAsistenciaDto> novedades, boolean boletinDisponible,
                                boolean cualitativo) {

    /** Faltas en horas de clase; los retardos se cuentan aparte porque no son inasistencia. */
    public record Asignatura(String nombre, String docente, List<NotaPeriodo> periodos, BigDecimal definitiva,
                             Desempeno desempeno, boolean completa, long faltasJustificadas,
                             long faltasSinJustificar, long retardos) {
    }

    /** En Transicion no hay nota: informe es el texto descriptivo del docente, visible cuando el periodo cierra. */
    public record NotaPeriodo(Long periodoId, BigDecimal nota, Desempeno desempeno, boolean completa, String informe) {
    }
}
