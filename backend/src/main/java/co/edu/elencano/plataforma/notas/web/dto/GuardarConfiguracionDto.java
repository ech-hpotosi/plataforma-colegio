package co.edu.elencano.plataforma.notas.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GuardarConfiguracionDto(
        @NotNull @DecimalMin("0.0") @DecimalMax("9.9") BigDecimal notaMinima,
        @NotNull @DecimalMin("0.0") @DecimalMax("9.9") BigDecimal notaMaxima,
        @NotNull @DecimalMin("0.0") @DecimalMax("9.9") BigDecimal notaAprobatoria,
        @NotNull @DecimalMin("0.0") @DecimalMax("9.9") BigDecimal limiteAlto,
        @NotNull @DecimalMin("0.0") @DecimalMax("9.9") BigDecimal limiteSuperior,
        @NotNull @DecimalMin("0.0") @DecimalMax("9.9") BigDecimal topeRecuperacion,
        @Min(0) @Max(100) int pesoSaber,
        @Min(0) @Max(100) int pesoHacer,
        @Min(0) @Max(100) int pesoSer) {
}
