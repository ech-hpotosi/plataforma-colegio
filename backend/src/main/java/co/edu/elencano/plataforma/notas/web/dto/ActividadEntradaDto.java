package co.edu.elencano.plataforma.notas.web.dto;

import java.time.LocalDate;

import co.edu.elencano.plataforma.notas.entidad.Dimension;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActividadEntradaDto(@NotNull Dimension dimension, @NotBlank @Size(max = 120) String nombre,
                                  LocalDate fecha) {
}
