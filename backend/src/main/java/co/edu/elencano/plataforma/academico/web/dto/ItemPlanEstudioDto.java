package co.edu.elencano.plataforma.academico.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ItemPlanEstudioDto(
        @NotNull(message = "Seleccione la asignatura") Long asignaturaId,
        @NotNull(message = "Ingrese la intensidad horaria")
        @Min(value = 1, message = "Mínimo 1 hora") @Max(value = 40, message = "Máximo 40 horas")
        Integer intensidadHoraria) {
}
