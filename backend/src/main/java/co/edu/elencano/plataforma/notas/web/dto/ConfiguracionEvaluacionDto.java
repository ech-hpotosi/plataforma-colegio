package co.edu.elencano.plataforma.notas.web.dto;

import java.math.BigDecimal;

import co.edu.elencano.plataforma.notas.entidad.ConfiguracionEvaluacion;

/** Escala y pesos del anio. editable es falso cuando el anio esta cerrado. */
public record ConfiguracionEvaluacionDto(Long anioLectivoId, int anio, BigDecimal notaMinima, BigDecimal notaMaxima,
                                         BigDecimal notaAprobatoria, BigDecimal limiteAlto, BigDecimal limiteSuperior,
                                         BigDecimal topeRecuperacion,
                                         int pesoSaber, int pesoHacer, int pesoSer, boolean editable) {

    public static ConfiguracionEvaluacionDto de(ConfiguracionEvaluacion c) {
        return new ConfiguracionEvaluacionDto(c.getAnioLectivo().getId(), c.getAnioLectivo().getAnio(),
                c.getNotaMinima(), c.getNotaMaxima(), c.getNotaAprobatoria(), c.getLimiteAlto(),
                c.getLimiteSuperior(), c.getTopeRecuperacion(), c.getPesoSaber(), c.getPesoHacer(), c.getPesoSer(),
                !c.getAnioLectivo().estaCerrado());
    }
}
