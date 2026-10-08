package co.edu.elencano.plataforma.academico.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.elencano.plataforma.academico.servicio.SedeService;
import co.edu.elencano.plataforma.academico.web.dto.GuardarSedeDto;
import co.edu.elencano.plataforma.academico.web.dto.SedeDto;
import jakarta.validation.Valid;

/** Sedes del colegio. Consultar: cualquier usuario autenticado. Modificar: gestion academica. */
@RestController
@RequestMapping("/api/sedes")
public class SedeController {

    private final SedeService sedeService;

    public SedeController(SedeService sedeService) {
        this.sedeService = sedeService;
    }

    @GetMapping
    public List<SedeDto> listar() {
        return sedeService.listar().stream().map(SedeDto::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @GestionAcademica
    public SedeDto crear(@Valid @RequestBody GuardarSedeDto datos) {
        return SedeDto.de(sedeService.crear(datos));
    }

    @PutMapping("/{id}")
    @GestionAcademica
    public SedeDto actualizar(@PathVariable Long id, @Valid @RequestBody GuardarSedeDto datos) {
        return SedeDto.de(sedeService.actualizar(id, datos));
    }
}
