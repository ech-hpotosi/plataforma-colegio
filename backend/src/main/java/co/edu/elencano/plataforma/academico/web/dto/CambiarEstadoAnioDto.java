package co.edu.elencano.plataforma.academico.web.dto;

import co.edu.elencano.plataforma.academico.entidad.EstadoAnio;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoAnioDto(@NotNull(message = "Seleccione el estado") EstadoAnio estado) {
}
