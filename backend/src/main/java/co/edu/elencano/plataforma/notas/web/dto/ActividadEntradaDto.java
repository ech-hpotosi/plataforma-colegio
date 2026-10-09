package co.edu.elencano.plataforma.notas.web.dto;

import java.time.LocalDate;

import co.edu.elencano.plataforma.notas.entidad.Dimension;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** peso es opcional: si no viene, la actividad pesa 1 (normal). */
public record ActividadEntradaDto(@NotNull Dimension dimension, @NotBlank @Size(max = 120) String nombre,
                                  LocalDate fecha, @Min(1) @Max(5) Integer peso) {

    public int pesoOUno() {
        return peso == null ? 1 : peso;
    }
}
