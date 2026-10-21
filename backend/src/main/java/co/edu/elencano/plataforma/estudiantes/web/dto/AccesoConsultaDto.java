package co.edu.elencano.plataforma.estudiantes.web.dto;

import jakarta.validation.constraints.NotNull;

/** Permiso del acudiente para ver notas y asistencia del estudiante con su cuenta. */
public record AccesoConsultaDto(@NotNull Boolean acceso) {
}
