package co.edu.elencano.plataforma.asistencia.web.dto;

import co.edu.elencano.plataforma.asistencia.entidad.EstadoAsistencia;

/** Estudiante en la lista de una clase con el estado guardado (ASISTIO si aun no se ha tomado). */
public record EstudianteAsistenciaDto(Long matriculaId, String nombres, String apellidos, EstadoAsistencia estado,
                                      String observacion, String justificacion) {
}
