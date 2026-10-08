package co.edu.elencano.plataforma.academico.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.elencano.plataforma.academico.servicio.AreaService;
import co.edu.elencano.plataforma.academico.web.dto.AreaDto;
import co.edu.elencano.plataforma.academico.web.dto.AsignaturaDto;
import co.edu.elencano.plataforma.academico.web.dto.GuardarAreaDto;
import co.edu.elencano.plataforma.academico.web.dto.GuardarAsignaturaDto;
import jakarta.validation.Valid;

/** Areas con sus asignaturas. */
@RestController
public class AreaController {

    private final AreaService areaService;

    public AreaController(AreaService areaService) {
        this.areaService = areaService;
    }

    @GetMapping("/api/areas")
    public List<AreaDto> listar() {
        return areaService.listar().stream().map(AreaDto::de).toList();
    }

    @PostMapping("/api/areas")
    @ResponseStatus(HttpStatus.CREATED)
    @GestionAcademica
    public AreaDto crear(@Valid @RequestBody GuardarAreaDto datos) {
        return AreaDto.de(areaService.crear(datos.nombre()));
    }

    @PutMapping("/api/areas/{id}")
    @GestionAcademica
    public AreaDto actualizar(@PathVariable Long id, @Valid @RequestBody GuardarAreaDto datos) {
        return AreaDto.de(areaService.actualizar(id, datos.nombre()));
    }

    @PostMapping("/api/asignaturas")
    @ResponseStatus(HttpStatus.CREATED)
    @GestionAcademica
    public AsignaturaDto crearAsignatura(@Valid @RequestBody GuardarAsignaturaDto datos) {
        return AsignaturaDto.de(areaService.crearAsignatura(datos));
    }

    @PutMapping("/api/asignaturas/{id}")
    @GestionAcademica
    public AsignaturaDto actualizarAsignatura(@PathVariable Long id, @Valid @RequestBody GuardarAsignaturaDto datos) {
        return AsignaturaDto.de(areaService.actualizarAsignatura(id, datos));
    }
}
