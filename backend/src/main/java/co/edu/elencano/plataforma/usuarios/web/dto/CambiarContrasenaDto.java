package co.edu.elencano.plataforma.usuarios.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarContrasenaDto(
        @NotBlank(message = "Ingrese la contrasena")
        @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres")
        String contrasena) {
}
