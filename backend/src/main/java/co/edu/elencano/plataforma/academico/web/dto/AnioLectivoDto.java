package co.edu.elencano.plataforma.academico.web.dto;

import java.time.LocalDate;
import java.util.List;

import co.edu.elencano.plataforma.academico.entidad.AnioLectivo;
import co.edu.elencano.plataforma.academico.entidad.EstadoAnio;

public record AnioLectivoDto(Long id, int anio, LocalDate fechaInicio, LocalDate fechaFin, EstadoAnio estado,
                             List<PeriodoDto> periodos) {

    public static AnioLectivoDto de(AnioLectivo anio) {
        return new AnioLectivoDto(anio.getId(), anio.getAnio(), anio.getFechaInicio(), anio.getFechaFin(),
                anio.getEstado(), anio.getPeriodos().stream().map(PeriodoDto::de).toList());
    }
}
