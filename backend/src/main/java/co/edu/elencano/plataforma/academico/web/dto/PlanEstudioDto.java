package co.edu.elencano.plataforma.academico.web.dto;

import co.edu.elencano.plataforma.academico.entidad.PlanEstudio;

public record PlanEstudioDto(Long id, Long asignaturaId, String asignatura, String area, int intensidadHoraria) {

    public static PlanEstudioDto de(PlanEstudio plan) {
        return new PlanEstudioDto(plan.getId(), plan.getAsignatura().getId(), plan.getAsignatura().getNombre(),
                plan.getAsignatura().getArea().getNombre(), plan.getIntensidadHoraria());
    }
}
