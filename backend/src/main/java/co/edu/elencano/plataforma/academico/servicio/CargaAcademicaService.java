package co.edu.elencano.plataforma.academico.servicio;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.elencano.plataforma.academico.entidad.CargaAcademica;
import co.edu.elencano.plataforma.academico.entidad.Docente;
import co.edu.elencano.plataforma.academico.entidad.Grupo;
import co.edu.elencano.plataforma.academico.entidad.PlanEstudio;
import co.edu.elencano.plataforma.academico.repositorio.CargaAcademicaRepository;
import co.edu.elencano.plataforma.academico.repositorio.PlanEstudioRepository;
import co.edu.elencano.plataforma.academico.web.dto.CargaAcademicaDto;
import co.edu.elencano.plataforma.academico.web.dto.ItemCargaAcademicaDto;
import co.edu.elencano.plataforma.comun.excepcion.ReglaNegocioException;

/** Carga academica: que docente dicta cada asignatura del plan de estudios en un grupo. */
@Service
public class CargaAcademicaService {

    private final CargaAcademicaRepository cargaRepository;
    private final PlanEstudioRepository planRepository;
    private final GrupoService grupoService;
    private final AnioLectivoService anioService;
    private final DocenteService docenteService;

    public CargaAcademicaService(CargaAcademicaRepository cargaRepository, PlanEstudioRepository planRepository,
                                 GrupoService grupoService, AnioLectivoService anioService,
                                 DocenteService docenteService) {
        this.cargaRepository = cargaRepository;
        this.planRepository = planRepository;
        this.grupoService = grupoService;
        this.anioService = anioService;
        this.docenteService = docenteService;
    }

    /** Lista todas las asignaturas del plan del grupo, con su docente si ya lo tiene. */
    @Transactional(readOnly = true)
    public List<CargaAcademicaDto> listar(Long grupoId) {
        Grupo grupo = grupoService.obtener(grupoId);
        Map<Long, CargaAcademica> cargas = cargaRepository.findByGrupoId(grupoId).stream()
                .collect(Collectors.toMap(c -> c.getAsignatura().getId(), Function.identity()));
        return planDelGrupo(grupo).stream().map(plan -> {
            CargaAcademica carga = cargas.get(plan.getAsignatura().getId());
            Docente docente = carga == null ? null : carga.getDocente();
            return new CargaAcademicaDto(plan.getAsignatura().getId(), plan.getAsignatura().getNombre(),
                    plan.getAsignatura().getArea().getNombre(), plan.getIntensidadHoraria(),
                    docente == null ? null : docente.getId(),
                    docente == null ? null : docente.getPersona().getNombreCompleto());
        }).toList();
    }

    /**
     * Reemplaza la carga del grupo. Cada asignatura debe estar en el plan de estudios del grado
     * y el docente debe trabajar en la sede del grupo.
     */
    @Transactional
    public List<CargaAcademicaDto> guardar(Long grupoId, List<ItemCargaAcademicaDto> items) {
        Grupo grupo = grupoService.obtener(grupoId);
        anioService.obtenerAbierto(grupo.getAnioLectivo().getId());
        Map<Long, PlanEstudio> plan = planDelGrupo(grupo).stream()
                .collect(Collectors.toMap(p -> p.getAsignatura().getId(), Function.identity()));
        Map<Long, Docente> nuevas = new HashMap<>();
        for (ItemCargaAcademicaDto item : items) {
            PlanEstudio enPlan = plan.get(item.asignaturaId());
            if (enPlan == null) {
                throw new ReglaNegocioException("La asignatura " + item.asignaturaId()
                        + " no está en el plan de estudios de " + grupo.getGrado().getNombre());
            }
            Docente docente = docenteService.obtenerParaAsignar(item.docenteId());
            if (!docente.trabajaEn(grupo.getSede())) {
                throw new ReglaNegocioException(docente.getPersona().getNombreCompleto()
                        + " no trabaja en la sede " + grupo.getSede().getNombre());
            }
            if (nuevas.put(item.asignaturaId(), docente) != null) {
                throw new ReglaNegocioException(enPlan.getAsignatura().getNombre() + " está repetida");
            }
        }
        for (CargaAcademica actual : cargaRepository.findByGrupoId(grupoId)) {
            Docente docente = nuevas.remove(actual.getAsignatura().getId());
            if (docente == null) {
                cargaRepository.delete(actual);
            } else {
                actual.setDocente(docente);
            }
        }
        nuevas.forEach((asignaturaId, docente) -> cargaRepository.save(
                new CargaAcademica(grupo, plan.get(asignaturaId).getAsignatura(), docente)));
        cargaRepository.flush();
        return listar(grupoId);
    }

    private List<PlanEstudio> planDelGrupo(Grupo grupo) {
        return planRepository.listar(grupo.getAnioLectivo().getId(), grupo.getGrado().getId());
    }
}
