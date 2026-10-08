package co.edu.elencano.plataforma.academico.web.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Lista completa de asignaturas del plan de un grado en un anio. Reemplaza la anterior. */
public record GuardarPlanEstudioDto(@NotNull List<@Valid @NotNull ItemPlanEstudioDto> asignaturas) {
}
