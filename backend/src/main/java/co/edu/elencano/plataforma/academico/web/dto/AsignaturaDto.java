package co.edu.elencano.plataforma.academico.web.dto;

import co.edu.elencano.plataforma.academico.entidad.Asignatura;

public record AsignaturaDto(Long id, Long areaId, String nombre) {

    public static AsignaturaDto de(Asignatura asignatura) {
        return new AsignaturaDto(asignatura.getId(), asignatura.getArea().getId(), asignatura.getNombre());
    }
}
