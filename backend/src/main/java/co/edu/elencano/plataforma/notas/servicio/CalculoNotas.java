package co.edu.elencano.plataforma.notas.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import co.edu.elencano.plataforma.notas.entidad.ConfiguracionEvaluacion;
import co.edu.elencano.plataforma.notas.entidad.Dimension;

/**
 * Calculos de notas, sin acceso a base de datos:
 * - Promedio de cada dimension: promedio de las notas que tiene el estudiante, ponderado por el porcentaje de
 *   cada actividad dentro de la dimension. Las actividades sin porcentaje se reparten en partes iguales lo que
 *   falte para 100 %. Una actividad sin nota no cuenta; si el docente quiere que pese, registra la nota minima.
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

    public static NotaPeriodo notaPeriodo(Map<Dimension, ? extends List<NotaPonderada>> notasPorDimension,
                                          ConfiguracionEvaluacion config) {
        Map<Dimension, BigDecimal> promedios = new EnumMap<>(Dimension.class);
        boolean completa = true;
        for (Dimension d : Dimension.values()) {
            List<NotaPonderada> notas = notasPorDimension.get(d);
            BigDecimal p = notas == null ? null : ponderado(notas);
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

    /**
     * Porcentaje efectivo de cada actividad de una dimension, en el mismo orden: el que puso el docente o,
     * si no puso, una parte igual de lo que falta para 100 % (nunca negativa).
     */
    public static List<BigDecimal> porcentajesEfectivos(List<Integer> porcentajes) {
        int fijos = porcentajes.stream().filter(p -> p != null).mapToInt(Integer::intValue).sum();
        long sinPorcentaje = porcentajes.stream().filter(p -> p == null).count();
        BigDecimal parte = sinPorcentaje == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(Math.max(0, 100 - fijos)).divide(BigDecimal.valueOf(sinPorcentaje), 2, RoundingMode.HALF_UP);
        List<BigDecimal> efectivos = new ArrayList<>();
        porcentajes.forEach(p -> efectivos.add(p == null ? parte : BigDecimal.valueOf(p)));
        return efectivos;
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

    /**
     * Nota despues de la recuperacion: la mayor entre la calculada y la recuperacion, sin que la
     * recuperacion deje mas que el tope. Sin nota calculada no hay definitiva.
     */
    public static BigDecimal definitiva(BigDecimal calculada, BigDecimal recuperacion, BigDecimal tope) {
        if (calculada == null || recuperacion == null) {
            return calculada;
        }
        BigDecimal conTope = recuperacion.min(tope);
        return calculada.max(conTope);
    }
}
