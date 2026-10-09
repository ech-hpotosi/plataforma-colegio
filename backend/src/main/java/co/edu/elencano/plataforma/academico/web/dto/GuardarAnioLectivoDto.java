package co.edu.elencano.plataforma.academico.web.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos de un anio lectivo y sus periodos en orden. Al editar, el anio no cambia. */
public record GuardarAnioLectivoDto(
        @NotNull(message = "Ingrese el año") @Min(value = 2020, message = "Año inválido")
        @Max(value = 2100, message = "Año inválido") Integer anio,
        @NotNull(message = "Ingrese la fecha de inicio") LocalDate fechaInicio,
        @NotNull(message = "Ingrese la fecha de fin") LocalDate fechaFin,
        @NotEmpty(message = "Ingrese al menos un periodo") @Size(max = 6, message = "Máximo 6 periodos")
        List<@Valid @NotNull DatosPeriodoDto> periodos) {
}
