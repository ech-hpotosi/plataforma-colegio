package co.edu.elencano.plataforma.estudiantes.web.dto;

import co.edu.elencano.plataforma.usuarios.entidad.Persona;
import co.edu.elencano.plataforma.usuarios.entidad.TipoDocumento;

/** Persona encontrada por documento, para llenar el formulario de acudiente sin volver a digitar. */
public record AcudienteDto(Long id, TipoDocumento tipoDocumento, String numeroDocumento, String nombres,
                           String apellidos, String telefono, String correo, String ocupacion) {

    public static AcudienteDto de(Persona persona, String ocupacion) {
        return new AcudienteDto(persona.getId(), persona.getTipoDocumento(), persona.getNumeroDocumento(),
                persona.getNombres(), persona.getApellidos(), persona.getTelefono(), persona.getCorreo(), ocupacion);
    }
}
