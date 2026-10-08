package co.edu.elencano.plataforma.academico.web.dto;

import java.util.List;

import co.edu.elencano.plataforma.academico.entidad.Docente;
import co.edu.elencano.plataforma.academico.entidad.Sede;
import co.edu.elencano.plataforma.usuarios.entidad.Persona;

/** Docente para listas y asignaciones. El id es el de la persona. */
public record DocenteDto(Long id, String nombres, String apellidos, String numeroDocumento,
                         String especialidad, String escalafon, List<Long> sedeIds) {

    /** Arma el DTO desde la persona; el docente puede ser nulo si aun no tiene datos academicos. */
    public static DocenteDto de(Persona persona, Docente docente) {
        if (docente == null) {
            return new DocenteDto(persona.getId(), persona.getNombres(), persona.getApellidos(),
                    persona.getNumeroDocumento(), null, null, List.of());
        }
        return new DocenteDto(persona.getId(), persona.getNombres(), persona.getApellidos(),
                persona.getNumeroDocumento(), docente.getEspecialidad(), docente.getEscalafon(),
                docente.getSedes().stream().map(Sede::getId).sorted().toList());
    }

    public static DocenteDto de(Docente docente) {
        return de(docente.getPersona(), docente);
    }
}
