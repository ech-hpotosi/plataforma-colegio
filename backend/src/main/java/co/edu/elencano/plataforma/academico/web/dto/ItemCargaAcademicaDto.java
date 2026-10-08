package co.edu.elencano.plataforma.academico.web.dto;

import jakarta.validation.constraints.NotNull;

public record ItemCargaAcademicaDto(
        @NotNull(message = "Seleccione la asignatura") Long asignaturaId,
        @NotNull(message = "Seleccione el docente") Long docenteId) {
}
