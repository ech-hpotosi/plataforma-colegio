package co.edu.elencano.plataforma.notas.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import co.edu.elencano.plataforma.notas.entidad.ActividadEvaluativa;
import co.edu.elencano.plataforma.notas.entidad.Dimension;

/**
 * porcentaje es el que puso el docente (nulo si no puso); porcentajeEfectivo es lo que vale dentro de su
 * dimension despues de repartir lo que falta entre las actividades sin porcentaje.
 */
public record ActividadDto(Long id, Dimension dimension, String nombre, LocalDate fecha, Integer porcentaje,
                           BigDecimal porcentajeEfectivo) {

    public static ActividadDto de(ActividadEvaluativa a, BigDecimal porcentajeEfectivo) {
        return new ActividadDto(a.getId(), a.getDimension(), a.getNombre(), a.getFecha(), a.getPorcentaje(),
                porcentajeEfectivo);
    }
}
