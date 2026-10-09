package co.edu.elencano.plataforma.notas.web.dto;

import java.util.Comparator;
import java.util.List;

import co.edu.elencano.plataforma.academico.entidad.CargaAcademica;
import co.edu.elencano.plataforma.academico.entidad.Grupo;
import co.edu.elencano.plataforma.academico.entidad.Periodo;

/** Clase (grupo y asignatura) en que se registran notas, con los periodos de su anio. */
public record CargaNotasDto(Long cargaId, Long grupoId, int anio, String sede, String grupo, String asignatura,
                            String docente, boolean cualitativa, List<PeriodoNotasDto> periodos) {

    public static CargaNotasDto de(CargaAcademica c) {
        Grupo g = c.getGrupo();
        List<PeriodoNotasDto> periodos = g.getAnioLectivo().getPeriodos().stream()
                .sorted(Comparator.comparingInt(Periodo::getNumero))
                .map(PeriodoNotasDto::de)
                .toList();
        return new CargaNotasDto(c.getId(), g.getId(), g.getAnioLectivo().getAnio(), g.getSede().getNombre(),
                g.getGrado().getNombre() + " " + g.getNombre(), c.getAsignatura().getNombre(),
                c.getDocente().getPersona().getNombreCompleto(), g.getGrado().isEvaluacionCualitativa(), periodos);
    }
}
