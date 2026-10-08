package co.edu.elencano.plataforma.academico.web.dto;

import java.util.Set;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActualizarDocenteDto(
        @Size(max = 150) String especialidad,
        @Size(max = 30) String escalafon,
        @NotNull Set<Long> sedeIds) {
}
