package co.edu.elencano.plataforma.academico.web.dto;

import co.edu.elencano.plataforma.academico.entidad.Sede;

public record SedeDto(Long id, String codigoDane, String nombre, String direccion, boolean principal) {

    public static SedeDto de(Sede sede) {
        return new SedeDto(sede.getId(), sede.getCodigoDane(), sede.getNombre(), sede.getDireccion(), sede.isPrincipal());
    }
}
