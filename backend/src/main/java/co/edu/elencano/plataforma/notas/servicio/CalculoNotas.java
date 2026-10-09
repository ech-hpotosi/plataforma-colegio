package co.edu.elencano.plataforma.notas.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import co.edu.elencano.plataforma.notas.entidad.ConfiguracionEvaluacion;
import co.edu.elencano.plataforma.notas.entidad.Dimension;

/**
 * Calculos de notas, sin acceso a base de datos:
 * - Promedio de cada dimension: promedio simple de las notas que tiene el estudiante en sus actividades.
 *   Una actividad sin nota no cuenta; si el docente quiere que pese, registra la nota minima.
 * - Nota del periodo: promedio ponderado de las dimensiones con nota, con los pesos de la configuracion.
 *   Si falta alguna dimension con peso, la nota queda como parcial (completa = false).
 * - Nota del anio: promedio ponderado de los periodos con nota, segun el porcentaje de cada periodo.
 * Todo se redondea a una decimal, mitad hacia arriba (2.95 queda 3.0).
 */
public final class CalculoNotas {

    private CalculoNotas() {
    }

    public record NotaPeriodo(Map<Dimension, BigDecimal> promedios, BigDecimal nota, boolean completa) {
    }

    public record NotaPonderada(BigDecimal valor, BigDecimal peso) {
    }

    public static BigDecimal promedio(Collection<BigDecimal> valores) {
        if (valores.isEmpty()) {
            return null;
        }
        BigDecimal suma = valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return suma.divide(BigDecimal.valueOf(valores.size()), 1, RoundingMode.HALF_UP);
    }

    public static NotaPeriodo notaPeriodo(Map<Dimension, ? extends Collection<BigDecimal>> notasPorDimension,
                                          ConfiguracionEvaluacion config) {
        Map<Dimension, BigDecimal> promedios = new EnumMap<>(Dimension.class);
        boolean completa = true;
        for (Dimension d : Dimension.values()) {
            Collection<BigDecimal> notas = notasPorDimension.get(d);
            BigDecimal p = notas == null ? null : promedio(notas);
            if (p != null) {
                promedios.put(d, p);
            } else if (config.pesoDe(d) > 0) {
                completa = false;
            }
        }
        List<NotaPonderada> ponderadas = promedios.entrySet().stream()
                .filter(e -> config.pesoDe(e.getKey()) > 0)
                .map(e -> new NotaPonderada(e.getValue(), BigDecimal.valueOf(config.pesoDe(e.getKey()))))
                .toList();
        return new NotaPeriodo(promedios, ponderado(ponderadas), completa && !ponderadas.isEmpty());
    }

    /** Promedio ponderado; null si no hay notas. */
    public static BigDecimal ponderado(List<NotaPonderada> notas) {
        BigDecimal pesos = notas.stream().map(NotaPonderada::peso).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (notas.isEmpty() || pesos.signum() == 0) {
            return null;
        }
        BigDecimal suma = notas.stream().map(n -> n.valor().multiply(n.peso())).reduce(BigDecimal.ZERO, BigDecimal::add);
        return suma.divide(pesos, 1, RoundingMode.HALF_UP);
    }
}
