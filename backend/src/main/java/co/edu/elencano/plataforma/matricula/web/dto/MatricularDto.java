package co.edu.elencano.plataforma.matricula.web.dto;

import jakarta.validation.constraints.NotNull;

/** Matricula un estudiante en un anio. El grupo es opcional y se puede asignar despues. */
public record MatricularDto(
        @NotNull(message = "Seleccione el estudiante") Long estudianteId,
        @NotNull(message = "Seleccione el anio lectivo") Long anioLectivoId,
        Long grupoId) {
}
