package co.edu.elencano.plataforma.asistencia.web.dto;

import co.edu.elencano.plataforma.asistencia.entidad.EstadoAsistencia;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ItemAsistenciaDto(
        @NotNull(message = "Falta el estudiante") Long matriculaId,
        @NotNull(message = "Indique el estado de asistencia") EstadoAsistencia estado,
        @Size(max = 300, message = "La observacion admite maximo 300 caracteres") String observacion) {
}
