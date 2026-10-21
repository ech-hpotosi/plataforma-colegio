package co.edu.elencano.plataforma.matricula.web.dto;

import java.time.LocalDate;

import co.edu.elencano.plataforma.estudiantes.entidad.Genero;
import co.edu.elencano.plataforma.estudiantes.entidad.Parentesco;
import co.edu.elencano.plataforma.matricula.entidad.TipoSolicitud;
import co.edu.elencano.plataforma.usuarios.entidad.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Lo que el acudiente diligencia en la solicitud de matricula en linea. */
public record DatosSolicitudDto(
        @NotNull(message = "Seleccione el año lectivo") Long anioLectivoId,
        @NotNull(message = "Seleccione la sede") Long sedeId,
        @NotNull(message = "Seleccione el grado") Long gradoId,
        @NotNull(message = "Indique si es estudiante nuevo o antiguo") TipoSolicitud tipo,
        @NotNull(message = "Seleccione su parentesco con el estudiante") Parentesco parentesco,
        @NotNull(message = "Seleccione el tipo de documento") TipoDocumento tipoDocumento,
        @NotBlank(message = "Ingrese el número de documento")
        @Pattern(regexp = "[0-9A-Za-z]{3,20}", message = "Solo letras y números, entre 3 y 20 caracteres")
        String numeroDocumento,
        @NotBlank(message = "Ingrese los nombres") @Size(max = 100) String nombres,
        @NotBlank(message = "Ingrese los apellidos") @Size(max = 100) String apellidos,
        @NotNull(message = "Ingrese la fecha de nacimiento")
        @Past(message = "La fecha de nacimiento debe ser anterior a hoy") LocalDate fechaNacimiento,
        @NotNull(message = "Seleccione el género") Genero genero,
        @NotBlank(message = "Ingrese la dirección o vereda") @Size(max = 200) String direccion,
        @Size(max = 20) String telefono,
        @Email(message = "Correo inválido") @Size(max = 150) String correo,
        @Size(max = 100) String eps,
        @Size(max = 5) String grupoSanguineo,
        @Size(max = 150) String condicionDiscapacidad,
        @Size(max = 500) String condicionesEspeciales,
        @Size(max = 150) String institucionAnterior) {
}
