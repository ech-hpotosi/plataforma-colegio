package co.edu.elencano.plataforma.asistencia.web.dto;

import co.edu.elencano.plataforma.academico.entidad.CargaAcademica;
import co.edu.elencano.plataforma.academico.entidad.Grupo;

/** Grupo y asignatura en que se puede tomar asistencia. */
public record CargaDocenteDto(Long cargaId, Long grupoId, int anio, String sede, String grupo, String asignatura,
                              String docente) {

    public static CargaDocenteDto de(CargaAcademica c) {
        Grupo g = c.getGrupo();
        return new CargaDocenteDto(c.getId(), g.getId(), g.getAnioLectivo().getAnio(), g.getSede().getNombre(),
                g.getGrado().getNombre() + " " + g.getNombre(), c.getAsignatura().getNombre(),
                c.getDocente().getPersona().getNombreCompleto());
    }
}
