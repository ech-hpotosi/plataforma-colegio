package co.edu.elencano.plataforma.academico.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.elencano.plataforma.academico.servicio.CargaAcademicaService;
import co.edu.elencano.plataforma.academico.servicio.GrupoService;
import co.edu.elencano.plataforma.academico.web.dto.CargaAcademicaDto;
import co.edu.elencano.plataforma.academico.web.dto.GrupoDto;
import co.edu.elencano.plataforma.academico.web.dto.GuardarCargaAcademicaDto;
import co.edu.elencano.plataforma.academico.web.dto.GuardarGrupoDto;
import jakarta.validation.Valid;

/** Grupos de un anio lectivo y su carga academica. */
@RestController
@RequestMapping("/api/grupos")
public class GrupoController {

    private final GrupoService grupoService;
    private final CargaAcademicaService cargaService;

    public GrupoController(GrupoService grupoService, CargaAcademicaService cargaService) {
        this.grupoService = grupoService;
        this.cargaService = cargaService;
    }

    @GetMapping
    public List<GrupoDto> listar(@RequestParam Long anioId, @RequestParam(required = false) Long sedeId) {
        return grupoService.listar(anioId, sedeId).stream().map(GrupoDto::de).toList();
    }

    @GetMapping("/{id}")
    public GrupoDto obtener(@PathVariable Long id) {
        return GrupoDto.de(grupoService.obtener(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @GestionAcademica
    public GrupoDto crear(@Valid @RequestBody GuardarGrupoDto datos) {
        return GrupoDto.de(grupoService.crear(datos));
    }

    @PutMapping("/{id}")
    @GestionAcademica
    public GrupoDto actualizar(@PathVariable Long id, @Valid @RequestBody GuardarGrupoDto datos) {
        return GrupoDto.de(grupoService.actualizar(id, datos));
    }

    @GetMapping("/{id}/carga")
    public List<CargaAcademicaDto> carga(@PathVariable Long id) {
        return cargaService.listar(id);
    }

    @PutMapping("/{id}/carga")
    @GestionAcademica
    public List<CargaAcademicaDto> guardarCarga(@PathVariable Long id,
                                                @Valid @RequestBody GuardarCargaAcademicaDto datos) {
        return cargaService.guardar(id, datos.asignaciones());
    }
}
