package co.edu.elencano.plataforma.academico.web.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Asignaciones completas de un grupo. Las asignaturas que no aparecen quedan sin docente. */
public record GuardarCargaAcademicaDto(@NotNull List<@Valid @NotNull ItemCargaAcademicaDto> asignaciones) {
}
