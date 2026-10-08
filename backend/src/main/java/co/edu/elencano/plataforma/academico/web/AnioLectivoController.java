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

import co.edu.elencano.plataforma.academico.servicio.AnioLectivoService;
import co.edu.elencano.plataforma.academico.web.dto.AnioLectivoDto;
import co.edu.elencano.plataforma.academico.web.dto.CambiarEstadoAnioDto;
import co.edu.elencano.plataforma.academico.web.dto.GuardarAnioLectivoDto;
import jakarta.validation.Valid;

/** Anios lectivos con sus periodos. */
@RestController
@RequestMapping("/api/anios")
public class AnioLectivoController {

    private final AnioLectivoService anioService;

    public AnioLectivoController(AnioLectivoService anioService) {
        this.anioService = anioService;
    }

    @GetMapping
    public List<AnioLectivoDto> listar() {
        return anioService.listar().stream().map(AnioLectivoDto::de).toList();
    }

    @GetMapping("/{id}")
    public AnioLectivoDto obtener(@PathVariable Long id) {
        return AnioLectivoDto.de(anioService.obtener(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @GestionAcademica
    public AnioLectivoDto crear(@Valid @RequestBody GuardarAnioLectivoDto datos) {
        return AnioLectivoDto.de(anioService.crear(datos));
    }

    @PutMapping("/{id}")
    @GestionAcademica
    public AnioLectivoDto actualizar(@PathVariable Long id, @Valid @RequestBody GuardarAnioLectivoDto datos) {
        return AnioLectivoDto.de(anioService.actualizar(id, datos));
    }

    @PutMapping("/{id}/estado")
    @GestionAcademica
    public AnioLectivoDto cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoAnioDto datos) {
        return AnioLectivoDto.de(anioService.cambiarEstado(id, datos.estado()));
    }
}
