package co.edu.elencano.plataforma.asistencia.repositorio;

import java.time.LocalDate;

/** Dias con asistencia registrada de una carga en un periodo y el ultimo de ellos. */
public record ResumenRegistros(Long cargaId, Long periodoId, Long dias, LocalDate ultimo) {
}
