package co.edu.elencano.plataforma.estudiantes.web.dto;

import java.time.LocalDate;

import co.edu.elencano.plataforma.estudiantes.entidad.Genero;
import co.edu.elencano.plataforma.usuarios.entidad.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos de la ficha del estudiante, para crearla o actualizarla. */
public record GuardarEstudianteDto(
        @NotNull(message = "Seleccione el tipo de documento") TipoDocumento tipoDocumento,
        @NotBlank(message = "Ingrese el numero de documento")
        @Pattern(regexp = "[0-9A-Za-z]{3,20}", message = "Solo letras y numeros, entre 3 y 20 caracteres")
        String numeroDocumento,
        @NotBlank(message = "Ingrese los nombres") @Size(max = 100) String nombres,
        @NotBlank(message = "Ingrese los apellidos") @Size(max = 100) String apellidos,
        @Size(max = 20) String telefono,
        @Email(message = "Correo invalido") @Size(max = 150) String correo,
        @Size(max = 20) String codigo,
        @NotNull(message = "Ingrese la fecha de nacimiento")
        @Past(message = "La fecha de nacimiento debe ser anterior a hoy") LocalDate fechaNacimiento,
        @NotNull(message = "Seleccione el genero") Genero genero,
        @Size(max = 200) String direccion,
        @Size(max = 100) String eps,
        @Size(max = 5) String grupoSanguineo,
        @Size(max = 150) String condicionDiscapacidad,
        boolean tienePiar,
        @Size(max = 500) String condicionesEspeciales) {
}
