package co.edu.elencano.plataforma.matricula.web.dto;

import co.edu.elencano.plataforma.matricula.entidad.TipoDocumentoRequerido;

public record TipoDocumentoRequeridoDto(Long id, String nombre, String descripcion, boolean obligatorio,
                                        boolean activo, int orden) {

    public static TipoDocumentoRequeridoDto de(TipoDocumentoRequerido t) {
        return new TipoDocumentoRequeridoDto(t.getId(), t.getNombre(), t.getDescripcion(), t.isObligatorio(),
                t.isActivo(), t.getOrden());
    }
}
