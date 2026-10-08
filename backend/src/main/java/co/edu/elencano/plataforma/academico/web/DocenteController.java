package co.edu.elencano.plataforma.academico.web;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.elencano.plataforma.academico.servicio.DocenteService;
import co.edu.elencano.plataforma.academico.web.dto.ActualizarDocenteDto;
import co.edu.elencano.plataforma.academico.web.dto.DocenteDto;
import jakarta.validation.Valid;

/** Docentes del colegio. Solo para funcionarios; estudiantes y acudientes no lo consultan. */
@RestController
@RequestMapping("/api/docentes")
public class DocenteController {

    private final DocenteService docenteService;

    public DocenteController(DocenteService docenteService) {
        this.docenteService = docenteService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECTOR', 'COORDINADOR_ACADEMICO', 'SECRETARIA')")
    public List<DocenteDto> listar() {
        return docenteService.listar();
    }

    @PutMapping("/{id}")
    @GestionAcademica
    public DocenteDto actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarDocenteDto datos) {
        return DocenteDto.de(docenteService.actualizar(id, datos));
    }
}
