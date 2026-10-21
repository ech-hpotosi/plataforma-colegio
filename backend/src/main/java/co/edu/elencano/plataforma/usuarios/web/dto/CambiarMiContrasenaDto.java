package co.edu.elencano.plataforma.usuarios.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cambio de contrasena que hace cada usuario sobre su propia cuenta. */
public record CambiarMiContrasenaDto(
        @NotBlank(message = "Ingrese la contraseña actual")
        String actual,
        @NotBlank(message = "Ingrese la nueva contraseña")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String nueva) {
}
