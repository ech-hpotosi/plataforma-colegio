package co.edu.elencano.plataforma.asistencia.web.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Justifica todas las faltas de un estudiante en un dia. */
public record JustificarDto(
        @NotNull(message = "Indique la fecha de la falta") LocalDate fecha,
        @NotBlank(message = "Escriba el motivo y el soporte presentado")
        @Size(max = 500, message = "La justificación admite máximo 500 caracteres") String justificacion) {
}
