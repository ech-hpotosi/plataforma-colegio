package co.edu.elencano.plataforma.matricula.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuardarTipoDocumentoDto(
        @NotBlank(message = "Ingrese el nombre del documento") @Size(max = 100) String nombre,
        @Size(max = 300) String descripcion,
        boolean obligatorio,
        boolean activo,
        @Min(0) @Max(999) int orden) {
}
