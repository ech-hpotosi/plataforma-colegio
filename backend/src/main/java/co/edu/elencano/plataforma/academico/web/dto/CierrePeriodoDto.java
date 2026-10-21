package co.edu.elencano.plataforma.academico.web.dto;

import jakarta.validation.constraints.NotNull;

public record CierrePeriodoDto(@NotNull Boolean cerrado) {
}
