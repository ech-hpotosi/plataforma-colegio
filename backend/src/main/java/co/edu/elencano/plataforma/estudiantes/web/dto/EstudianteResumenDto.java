package co.edu.elencano.plataforma.estudiantes.web.dto;

import co.edu.elencano.plataforma.estudiantes.entidad.EstadoEstudiante;
import co.edu.elencano.plataforma.estudiantes.entidad.Estudiante;
import co.edu.elencano.plataforma.usuarios.entidad.Persona;
import co.edu.elencano.plataforma.usuarios.entidad.TipoDocumento;

public record EstudianteResumenDto(Long id, TipoDocumento tipoDocumento, String numeroDocumento, String nombres,
                                   String apellidos, String codigo, EstadoEstudiante estado) {

    public static EstudianteResumenDto de(Estudiante estudiante) {
        Persona persona = estudiante.getPersona();
        return new EstudianteResumenDto(estudiante.getId(), persona.getTipoDocumento(), persona.getNumeroDocumento(),
                persona.getNombres(), persona.getApellidos(), estudiante.getCodigo(), estudiante.getEstado());
    }
}
