package co.edu.elencano.plataforma.academico.web.dto;

import co.edu.elencano.plataforma.academico.entidad.Jornada;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos de un grupo. Al editar, el anio lectivo no cambia. El director es opcional. */
public record GuardarGrupoDto(
        @NotNull(message = "Seleccione el año lectivo") Long anioLectivoId,
        @NotNull(message = "Seleccione la sede") Long sedeId,
        @NotNull(message = "Seleccione el grado") Long gradoId,
        @NotBlank(message = "Ingrese el nombre del grupo") @Size(max = 20) String nombre,
        @NotNull(message = "Seleccione la jornada") Jornada jornada,
        @NotNull(message = "Ingrese el cupo") @Min(value = 1, message = "Mínimo 1")
        @Max(value = 60, message = "Máximo 60") Integer cupo,
        Long directorId) {
}
