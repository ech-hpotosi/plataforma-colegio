package co.edu.elencano.plataforma.notas.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import co.edu.elencano.plataforma.notas.entidad.Desempeno;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cambios del informe del periodo. descriptores trae solo los desempenos que cambian (texto vacio lo borra);
 * cada estudiante enviado reemplaza su comportamiento y observacion (nulos los borran).
 */
public record GuardarInformeDto(Map<Desempeno, @Size(max = 600) String> descriptores,
                                List<@Valid Estudiante> estudiantes) {

    public record Estudiante(@NotNull Long matriculaId, BigDecimal comportamiento,
                             @Size(max = 500) String observacion) {
    }
}
