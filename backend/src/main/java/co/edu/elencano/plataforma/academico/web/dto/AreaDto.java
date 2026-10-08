package co.edu.elencano.plataforma.academico.web.dto;

import java.util.List;

import co.edu.elencano.plataforma.academico.entidad.Area;

public record AreaDto(Long id, String nombre, List<AsignaturaDto> asignaturas) {

    public static AreaDto de(Area area) {
        return new AreaDto(area.getId(), area.getNombre(),
                area.getAsignaturas().stream().map(AsignaturaDto::de).toList());
    }
}
