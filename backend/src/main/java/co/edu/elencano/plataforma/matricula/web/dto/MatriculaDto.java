package co.edu.elencano.plataforma.matricula.web.dto;

import java.time.LocalDate;

import co.edu.elencano.plataforma.matricula.entidad.EstadoMatricula;
import co.edu.elencano.plataforma.matricula.entidad.Matricula;

/** Matricula de un estudiante en un anio, con el grupo si ya tiene. */
public record MatriculaDto(Long id, Long estudianteId, Long anioLectivoId, int anio, Long grupoId, String grupo,
                           String sede, LocalDate fechaMatricula, EstadoMatricula estado, LocalDate fechaRetiro,
                           String motivoRetiro) {

    public static MatriculaDto de(Matricula m) {
        var grupo = m.getGrupo();
        return new MatriculaDto(m.getId(), m.getEstudiante().getId(), m.getAnioLectivo().getId(),
                m.getAnioLectivo().getAnio(), grupo == null ? null : grupo.getId(),
                grupo == null ? null : grupo.getGrado().getNombre() + " " + grupo.getNombre(),
                grupo == null ? null : grupo.getSede().getNombre(), m.getFechaMatricula(), m.getEstado(),
                m.getFechaRetiro(), m.getMotivoRetiro());
    }
}
