package co.edu.elencano.plataforma.usuarios.web.dto;

import java.util.Set;

import co.edu.elencano.plataforma.usuarios.entidad.Rol;
import co.edu.elencano.plataforma.usuarios.entidad.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearUsuarioDto(
        @NotNull(message = "Seleccione el tipo de documento") TipoDocumento tipoDocumento,
        @NotBlank(message = "Ingrese el número de documento")
        @Pattern(regexp = "[0-9A-Za-z]{3,20}", message = "Solo letras y números, entre 3 y 20 caracteres")
        String numeroDocumento,
        @NotBlank(message = "Ingrese los nombres") @Size(max = 100) String nombres,
        @NotBlank(message = "Ingrese los apellidos") @Size(max = 100) String apellidos,
        @Size(max = 20) String telefono,
        @Email(message = "Correo inválido") @Size(max = 150) String correo,
        @NotBlank(message = "Ingrese el nombre de usuario")
        @Pattern(regexp = "[a-z0-9._-]{3,50}", message = "Use minúsculas, números, punto, guion o guion bajo (3 a 50)")
        String nombreUsuario,
        @NotBlank(message = "Ingrese la contraseña")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String contrasena,
        @NotEmpty(message = "Seleccione al menos un rol") Set<Rol> roles) {
}
