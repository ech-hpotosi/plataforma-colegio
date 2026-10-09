package co.edu.elencano.plataforma.notas.web.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Notas de recuperacion a guardar. Una nota nula borra la recuperacion del estudiante. */
public record GuardarRecuperacionesDto(@NotNull List<@Valid Item> recuperaciones) {

    public record Item(@NotNull Long matriculaId, BigDecimal nota, @Size(max = 300) String observacion) {
    }
}
