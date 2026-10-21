package co.edu.elencano.plataforma.usuarios.web.dto;

import co.edu.elencano.plataforma.usuarios.entidad.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cuenta que crea un padre, madre o acudiente para hacer la matricula en linea. El usuario es su documento. */
public record RegistroAcudienteDto(
        @NotNull(message = "Seleccione el tipo de documento") TipoDocumento tipoDocumento,
        @NotBlank(message = "Ingrese el número de documento")
        @Pattern(regexp = "[0-9A-Za-z]{3,20}", message = "Solo letras y números, entre 3 y 20 caracteres")
        String numeroDocumento,
        @NotBlank(message = "Ingrese los nombres") @Size(max = 100) String nombres,
        @NotBlank(message = "Ingrese los apellidos") @Size(max = 100) String apellidos,
        @NotBlank(message = "Ingrese un teléfono de contacto")
        @Pattern(regexp = "[0-9 +]{7,20}", message = "Teléfono inválido") String telefono,
        @Email(message = "Correo inválido") @Size(max = 150) String correo,
        @NotBlank(message = "Ingrese una contraseña")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String contrasena) {
}
