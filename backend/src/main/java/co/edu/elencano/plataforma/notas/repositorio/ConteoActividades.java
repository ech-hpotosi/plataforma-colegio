package co.edu.elencano.plataforma.notas.repositorio;

import co.edu.elencano.plataforma.notas.entidad.Dimension;

/** Cantidad de actividades de una carga en un periodo y dimension. */
public record ConteoActividades(Long cargaId, Long periodoId, Dimension dimension, Long actividades) {
}
