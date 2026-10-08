package co.edu.elencano.plataforma.academico.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuardarAreaDto(@NotBlank(message = "Ingrese el nombre del area") @Size(max = 150) String nombre) {
}
