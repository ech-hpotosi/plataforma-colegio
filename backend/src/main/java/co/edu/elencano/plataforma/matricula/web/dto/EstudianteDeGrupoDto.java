package co.edu.elencano.plataforma.matricula.web.dto;

import co.edu.elencano.plataforma.matricula.entidad.Matricula;
import co.edu.elencano.plataforma.usuarios.entidad.Persona;
import co.edu.elencano.plataforma.usuarios.entidad.TipoDocumento;

/** Estudiante en la lista de un grupo. */
public record EstudianteDeGrupoDto(Long matriculaId, Long estudianteId, TipoDocumento tipoDocumento,
                                   String numeroDocumento, String nombres, String apellidos) {

    public static EstudianteDeGrupoDto de(Matricula m) {
        Persona p = m.getEstudiante().getPersona();
        return new EstudianteDeGrupoDto(m.getId(), m.getEstudiante().getId(), p.getTipoDocumento(),
                p.getNumeroDocumento(), p.getNombres(), p.getApellidos());
    }
}
