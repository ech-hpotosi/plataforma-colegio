package co.edu.elencano.plataforma.academico.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import co.edu.elencano.plataforma.academico.entidad.Periodo;

public record PeriodoDto(Long id, int numero, LocalDate fechaInicio, LocalDate fechaFin, BigDecimal porcentaje,
                         boolean cerrado) {

    public static PeriodoDto de(Periodo periodo) {
        return new PeriodoDto(periodo.getId(), periodo.getNumero(), periodo.getFechaInicio(), periodo.getFechaFin(),
                periodo.getPorcentaje(), periodo.isCerrado());
    }
}
