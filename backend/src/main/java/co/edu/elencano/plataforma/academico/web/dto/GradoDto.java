package co.edu.elencano.plataforma.academico.web.dto;

import co.edu.elencano.plataforma.academico.entidad.Grado;
import co.edu.elencano.plataforma.academico.entidad.Nivel;

public record GradoDto(Long id, String nombre, Nivel nivel, int orden, boolean evaluacionCualitativa) {

    public static GradoDto de(Grado grado) {
        return new GradoDto(grado.getId(), grado.getNombre(), grado.getNivel(), grado.getOrden(),
                grado.isEvaluacionCualitativa());
    }
}
