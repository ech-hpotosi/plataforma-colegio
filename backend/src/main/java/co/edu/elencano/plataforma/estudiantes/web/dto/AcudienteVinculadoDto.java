package co.edu.elencano.plataforma.estudiantes.web.dto;

import co.edu.elencano.plataforma.estudiantes.entidad.EstudianteAcudiente;
import co.edu.elencano.plataforma.estudiantes.entidad.Parentesco;
import co.edu.elencano.plataforma.usuarios.entidad.Persona;
import co.edu.elencano.plataforma.usuarios.entidad.TipoDocumento;

/** Acudiente de un estudiante con el parentesco. El id es el de la persona del acudiente. */
public record AcudienteVinculadoDto(Long id, TipoDocumento tipoDocumento, String numeroDocumento, String nombres,
                                    String apellidos, String telefono, String correo, String ocupacion,
                                    Parentesco parentesco, boolean principal) {

    public static AcudienteVinculadoDto de(EstudianteAcudiente vinculo) {
        Persona persona = vinculo.getAcudiente().getPersona();
        return new AcudienteVinculadoDto(persona.getId(), persona.getTipoDocumento(), persona.getNumeroDocumento(),
                persona.getNombres(), persona.getApellidos(), persona.getTelefono(), persona.getCorreo(),
                vinculo.getAcudiente().getOcupacion(), vinculo.getParentesco(), vinculo.isPrincipal());
    }
}
