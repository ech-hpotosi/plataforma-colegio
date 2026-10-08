package co.edu.elencano.plataforma.asistencia.web.dto;

import co.edu.elencano.plataforma.academico.entidad.Grupo;

public record GrupoAsistenciaDto(Long grupoId, int anio, String sede, String grupo) {

    public static GrupoAsistenciaDto de(Grupo g) {
        return new GrupoAsistenciaDto(g.getId(), g.getAnioLectivo().getAnio(), g.getSede().getNombre(),
                g.getGrado().getNombre() + " " + g.getNombre());
    }
}
