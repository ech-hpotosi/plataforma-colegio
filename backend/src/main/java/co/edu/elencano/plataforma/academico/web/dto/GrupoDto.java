package co.edu.elencano.plataforma.academico.web.dto;

import co.edu.elencano.plataforma.academico.entidad.Grupo;
import co.edu.elencano.plataforma.academico.entidad.Jornada;

public record GrupoDto(Long id, Long anioLectivoId, Long sedeId, String sede, Long gradoId, String grado,
                       String nombre, Jornada jornada, int cupo, Long directorId, String director) {

    public static GrupoDto de(Grupo grupo) {
        var director = grupo.getDirector();
        return new GrupoDto(grupo.getId(), grupo.getAnioLectivo().getId(), grupo.getSede().getId(),
                grupo.getSede().getNombre(), grupo.getGrado().getId(), grupo.getGrado().getNombre(),
                grupo.getNombre(), grupo.getJornada(), grupo.getCupo(),
                director == null ? null : director.getId(),
                director == null ? null : director.getPersona().getNombreCompleto());
    }
}
