package co.edu.elencano.plataforma.academico.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GuardarAsignaturaDto(
        @NotNull(message = "Seleccione el área") Long areaId,
        @NotBlank(message = "Ingrese el nombre de la asignatura") @Size(max = 120) String nombre) {
}
