package co.edu.elencano.plataforma.academico.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GuardarSedeDto(
        @Pattern(regexp = "[0-9]{0,20}", message = "El codigo DANE solo lleva numeros") String codigoDane,
        @NotBlank(message = "Ingrese el nombre de la sede") @Size(max = 120) String nombre,
        @Size(max = 200) String direccion,
        boolean principal) {
}
