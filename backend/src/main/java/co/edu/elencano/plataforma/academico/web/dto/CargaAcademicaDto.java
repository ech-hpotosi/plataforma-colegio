package co.edu.elencano.plataforma.academico.web.dto;

/**
 * Una asignatura del plan de estudios del grupo con el docente asignado.
 * docenteId es nulo si la asignatura aun no tiene docente.
 */
public record CargaAcademicaDto(Long asignaturaId, String asignatura, String area, int intensidadHoraria,
                                Long docenteId, String docente) {
}
