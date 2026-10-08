package co.edu.elencano.plataforma.academico.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.elencano.plataforma.academico.servicio.PlanEstudioService;
import co.edu.elencano.plataforma.academico.web.dto.GuardarPlanEstudioDto;
import co.edu.elencano.plataforma.academico.web.dto.PlanEstudioDto;
import jakarta.validation.Valid;

/** Plan de estudios de un grado en un anio lectivo. */
@RestController
@RequestMapping("/api/plan-estudio")
public class PlanEstudioController {

    private final PlanEstudioService planService;

    public PlanEstudioController(PlanEstudioService planService) {
        this.planService = planService;
    }

    @GetMapping
    public List<PlanEstudioDto> listar(@RequestParam Long anioId, @RequestParam Long gradoId) {
        return planService.listar(anioId, gradoId).stream().map(PlanEstudioDto::de).toList();
    }

    @PutMapping
    @GestionAcademica
    public List<PlanEstudioDto> guardar(@RequestParam Long anioId, @RequestParam Long gradoId,
                                        @Valid @RequestBody GuardarPlanEstudioDto datos) {
        return planService.guardar(anioId, gradoId, datos.asignaturas()).stream().map(PlanEstudioDto::de).toList();
    }
}
