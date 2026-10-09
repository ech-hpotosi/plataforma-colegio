package co.edu.elencano.plataforma.notas.web.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Notas a guardar en la planilla. Un valor nulo borra la nota de esa actividad. */
public record GuardarNotasDto(@NotNull List<@Valid Item> notas) {

    public record Item(@NotNull Long actividadId, @NotNull Long matriculaId, BigDecimal valor) {
    }
}
