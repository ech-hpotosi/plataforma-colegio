package co.edu.elencano.plataforma.notas.web.dto;

import java.time.LocalDate;

import co.edu.elencano.plataforma.notas.entidad.Dimension;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** porcentaje es opcional: sin el, la actividad comparte en partes iguales lo que falte para 100 %. */
public record ActividadEntradaDto(@NotNull Dimension dimension, @NotBlank @Size(max = 120) String nombre,
                                  LocalDate fecha, @Min(1) @Max(100) Integer porcentaje) {
}
