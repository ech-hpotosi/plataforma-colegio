package co.edu.elencano.plataforma.notas.repositorio;

/** Cantidad de notas registradas de una carga en un periodo. */
public record ConteoNotas(Long cargaId, Long periodoId, Long notas) {
}
