package co.edu.elencano.plataforma.academico.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.elencano.plataforma.academico.servicio.GradoService;
import co.edu.elencano.plataforma.academico.web.dto.ActualizarGradoDto;
import co.edu.elencano.plataforma.academico.web.dto.GradoDto;

/** Grados escolares. Son fijos; solo se puede cambiar si se evaluan de forma cualitativa. */
@RestController
@RequestMapping("/api/grados")
public class GradoController {

    private final GradoService gradoService;

    public GradoController(GradoService gradoService) {
        this.gradoService = gradoService;
    }

    @GetMapping
    public List<GradoDto> listar() {
        return gradoService.listar().stream().map(GradoDto::de).toList();
    }

    @PutMapping("/{id}")
    @GestionAcademica
    public GradoDto actualizar(@PathVariable Long id, @RequestBody ActualizarGradoDto datos) {
        return GradoDto.de(gradoService.cambiarEvaluacionCualitativa(id, datos.evaluacionCualitativa()));
    }
}
