package co.edu.elencano.plataforma.academico.servicio;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.elencano.plataforma.academico.entidad.AnioLectivo;
import co.edu.elencano.plataforma.academico.entidad.Grado;
import co.edu.elencano.plataforma.academico.entidad.PlanEstudio;
import co.edu.elencano.plataforma.academico.repositorio.CargaAcademicaRepository;
import co.edu.elencano.plataforma.academico.repositorio.PlanEstudioRepository;
import co.edu.elencano.plataforma.academico.web.dto.ItemPlanEstudioDto;
import co.edu.elencano.plataforma.comun.excepcion.ReglaNegocioException;

/** Plan de estudios: asignaturas e intensidad horaria de cada grado en un anio lectivo. */
@Service
public class PlanEstudioService {

    private final PlanEstudioRepository planRepository;
    private final CargaAcademicaRepository cargaRepository;
    private final AnioLectivoService anioService;
    private final GradoService gradoService;
    private final AreaService areaService;

    public PlanEstudioService(PlanEstudioRepository planRepository, CargaAcademicaRepository cargaRepository,
                              AnioLectivoService anioService, GradoService gradoService, AreaService areaService) {
        this.planRepository = planRepository;
        this.cargaRepository = cargaRepository;
        this.anioService = anioService;
        this.gradoService = gradoService;
        this.areaService = areaService;
    }

    @Transactional(readOnly = true)
    public List<PlanEstudio> listar(Long anioId, Long gradoId) {
        return planRepository.listar(anioId, gradoId);
    }

    /**
     * Reemplaza el plan del grado en el anio. No permite quitar una asignatura que ya tiene
     * docente asignado en algun grupo, para no dejar carga academica sin plan.
     */
    @Transactional
    public List<PlanEstudio> guardar(Long anioId, Long gradoId, List<ItemPlanEstudioDto> items) {
        AnioLectivo anio = anioService.obtenerAbierto(anioId);
        Grado grado = gradoService.obtener(gradoId);
        Map<Long, Integer> nuevos = new HashMap<>();
        for (ItemPlanEstudioDto item : items) {
            if (nuevos.put(item.asignaturaId(), item.intensidadHoraria()) != null) {
                throw new ReglaNegocioException("Una asignatura está repetida en el plan");
            }
        }
        for (PlanEstudio actual : planRepository.listar(anioId, gradoId)) {
            Integer intensidad = nuevos.remove(actual.getAsignatura().getId());
            if (intensidad != null) {
                actual.setIntensidadHoraria(intensidad);
            } else if (cargaRepository.asignaturaTieneCarga(anioId, gradoId, actual.getAsignatura().getId())) {
                throw new ReglaNegocioException("No se puede quitar " + actual.getAsignatura().getNombre()
                        + " porque ya tiene docentes asignados en grupos de " + grado.getNombre());
            } else {
                planRepository.delete(actual);
            }
        }
        nuevos.forEach((asignaturaId, intensidad) -> planRepository.save(
                new PlanEstudio(anio, grado, areaService.obtenerAsignatura(asignaturaId), intensidad)));
        planRepository.flush();
        return planRepository.listar(anioId, gradoId);
    }
}
