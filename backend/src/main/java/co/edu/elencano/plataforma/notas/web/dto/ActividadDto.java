package co.edu.elencano.plataforma.notas.web.dto;

import java.time.LocalDate;

import co.edu.elencano.plataforma.notas.entidad.ActividadEvaluativa;
import co.edu.elencano.plataforma.notas.entidad.Dimension;

public record ActividadDto(Long id, Dimension dimension, String nombre, LocalDate fecha, int peso) {

    public static ActividadDto de(ActividadEvaluativa a) {
        return new ActividadDto(a.getId(), a.getDimension(), a.getNombre(), a.getFecha(), a.getPeso());
    }
}
