package co.edu.elencano.plataforma.usuarios.web.dto;

import java.util.Set;

import co.edu.elencano.plataforma.usuarios.entidad.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** Datos editables de un usuario. El documento y el nombre de usuario no cambian. */
public record ActualizarUsuarioDto(
        @NotBlank(message = "Ingrese los nombres") @Size(max = 100) String nombres,
        @NotBlank(message = "Ingrese los apellidos") @Size(max = 100) String apellidos,
        @Size(max = 20) String telefono,
        @Email(message = "Correo invalido") @Size(max = 150) String correo,
        boolean activo,
        @NotEmpty(message = "Seleccione al menos un rol") Set<Rol> roles) {
}
