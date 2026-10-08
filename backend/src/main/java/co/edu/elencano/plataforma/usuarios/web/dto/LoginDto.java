package co.edu.elencano.plataforma.usuarios.web.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginDto(
        @NotBlank(message = "Ingrese el usuario") String nombreUsuario,
        @NotBlank(message = "Ingrese la contrasena") String contrasena) {
}
