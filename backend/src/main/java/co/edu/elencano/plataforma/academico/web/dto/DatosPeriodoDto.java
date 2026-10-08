package co.edu.elencano.plataforma.academico.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record DatosPeriodoDto(
        @NotNull(message = "Ingrese la fecha de inicio") LocalDate fechaInicio,
        @NotNull(message = "Ingrese la fecha de fin") LocalDate fechaFin,
        @NotNull(message = "Ingrese el porcentaje")
        @DecimalMin(value = "0.01", message = "El porcentaje debe ser mayor que cero")
        @DecimalMax(value = "100", message = "El porcentaje no puede ser mayor que 100")
        @Digits(integer = 3, fraction = 2, message = "Use maximo dos decimales")
        BigDecimal porcentaje) {
}
