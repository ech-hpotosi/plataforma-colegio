package co.edu.elencano.plataforma.notas;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import co.edu.elencano.plataforma.academico.entidad.AnioLectivo;
import co.edu.elencano.plataforma.notas.entidad.ConfiguracionEvaluacion;
import co.edu.elencano.plataforma.notas.entidad.Desempeno;
import co.edu.elencano.plataforma.notas.entidad.Dimension;
import co.edu.elencano.plataforma.notas.servicio.CalculoNotas;
import co.edu.elencano.plataforma.notas.servicio.CalculoNotas.NotaPeriodo;
import co.edu.elencano.plataforma.notas.servicio.CalculoNotas.NotaPonderada;

class CalculoNotasTest {

    private final ConfiguracionEvaluacion config = ConfiguracionEvaluacion.porDefecto(new AnioLectivo(2027,
            java.time.LocalDate.of(2027, 1, 25), java.time.LocalDate.of(2027, 11, 30)));

    private static BigDecimal n(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    void laNotaDelPeriodoPonderaLasTresDimensiones() {
        // Saber (3.5 + 4.0) / 2 = 3.8 (3.75 redondeado); Hacer 4.0; Ser 5.0
        NotaPeriodo nota = CalculoNotas.notaPeriodo(Map.of(
                Dimension.SABER, List.of(n("3.5"), n("4.0")),
                Dimension.HACER, List.of(n("4.0")),
                Dimension.SER, List.of(n("5.0"))), config);
        assertThat(nota.promedios().get(Dimension.SABER)).isEqualByComparingTo("3.8");
        // 3.8 * 40 + 4.0 * 40 + 5.0 * 20 = 412 / 100 = 4.1
        assertThat(nota.nota()).isEqualByComparingTo("4.1");
        assertThat(nota.completa()).isTrue();
        assertThat(config.desempenoDe(nota.nota())).isEqualTo(Desempeno.ALTO);
    }

    @Test
    void siFaltaUnaDimensionLaNotaEsParcial() {
        NotaPeriodo nota = CalculoNotas.notaPeriodo(Map.of(
                Dimension.SABER, List.of(n("2.0")),
                Dimension.SER, List.of(n("4.0"))), config);
        // Solo Saber (40) y Ser (20): (2.0 * 40 + 4.0 * 20) / 60 = 2.7 (2.666...)
        assertThat(nota.nota()).isEqualByComparingTo("2.7");
        assertThat(nota.completa()).isFalse();
        assertThat(CalculoNotas.notaPeriodo(Map.of(), config).nota()).isNull();
    }

    @Test
    void losLimitesDeLaEscalaNacional() {
        assertThat(config.desempenoDe(n("2.9"))).isEqualTo(Desempeno.BAJO);
        assertThat(config.desempenoDe(n("3.0"))).isEqualTo(Desempeno.BASICO);
        assertThat(config.desempenoDe(n("3.9"))).isEqualTo(Desempeno.BASICO);
        assertThat(config.desempenoDe(n("4.0"))).isEqualTo(Desempeno.ALTO);
        assertThat(config.desempenoDe(n("4.5"))).isEqualTo(Desempeno.ALTO);
        assertThat(config.desempenoDe(n("4.6"))).isEqualTo(Desempeno.SUPERIOR);
    }

    @Test
    void laNotaDelAnioPonderaLosPeriodos() {
        BigDecimal anual = CalculoNotas.ponderado(List.of(
                new NotaPonderada(n("4.0"), n("30")),
                new NotaPonderada(n("3.0"), n("30")),
                new NotaPonderada(n("2.5"), n("40"))));
        // 120 + 90 + 100 = 310 / 100 = 3.1
        assertThat(anual).isEqualByComparingTo("3.1");
    }
}
