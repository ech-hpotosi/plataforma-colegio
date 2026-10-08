package co.edu.elencano.plataforma.asistencia.web.dto;

import java.time.LocalDate;
import java.util.List;

/** Asistencia de una clase. Si aun no se ha tomado, registrada es falso y las horas son 1. */
public record AsistenciaClaseDto(Long cargaId, String grupo, String asignatura, LocalDate fecha, int periodo,
                                 int horas, boolean registrada, List<EstudianteAsistenciaDto> estudiantes) {
}
