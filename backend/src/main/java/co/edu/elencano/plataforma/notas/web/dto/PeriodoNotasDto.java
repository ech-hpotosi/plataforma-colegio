package co.edu.elencano.plataforma.notas.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import co.edu.elencano.plataforma.academico.entidad.Periodo;

public record PeriodoNotasDto(Long id, int numero, LocalDate fechaInicio, LocalDate fechaFin, BigDecimal porcentaje,
                              boolean cerrado) {

    public static PeriodoNotasDto de(Periodo p) {
        return new PeriodoNotasDto(p.getId(), p.getNumero(), p.getFechaInicio(), p.getFechaFin(), p.getPorcentaje(),
                p.isCerrado());
    }
}
