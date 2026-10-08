package co.edu.elencano.plataforma.estudiantes.web.dto;

import co.edu.elencano.plataforma.estudiantes.entidad.Parentesco;
import co.edu.elencano.plataforma.usuarios.entidad.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Acudiente de un estudiante. Si ya existe una persona con ese documento se reutiliza y se
 * actualizan sus datos de contacto. Al editar un vinculo existente el documento no cambia.
 */
public record GuardarAcudienteDto(
        @NotNull(message = "Seleccione el tipo de documento") TipoDocumento tipoDocumento,
        @NotBlank(message = "Ingrese el numero de documento")
        @Pattern(regexp = "[0-9A-Za-z]{3,20}", message = "Solo letras y numeros, entre 3 y 20 caracteres")
        String numeroDocumento,
        @NotBlank(message = "Ingrese los nombres") @Size(max = 100) String nombres,
        @NotBlank(message = "Ingrese los apellidos") @Size(max = 100) String apellidos,
        @Size(max = 20) String telefono,
        @Email(message = "Correo invalido") @Size(max = 150) String correo,
        @Size(max = 100) String ocupacion,
        @NotNull(message = "Seleccione el parentesco") Parentesco parentesco,
        boolean principal) {
}
