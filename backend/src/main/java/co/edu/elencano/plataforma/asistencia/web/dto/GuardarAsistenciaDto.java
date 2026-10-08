package co.edu.elencano.plataforma.asistencia.web.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Asistencia que envia el docente: horas dictadas y estado de cada estudiante. */
public record GuardarAsistenciaDto(
        @NotNull(message = "Ingrese las horas de clase") @Min(value = 1, message = "Minimo 1 hora")
        @Max(value = 10, message = "Maximo 10 horas") Integer horas,
        @NotNull(message = "Falta la lista de estudiantes") List<@Valid ItemAsistenciaDto> estudiantes) {
}
