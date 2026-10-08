package co.edu.elencano.plataforma.estudiantes.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.elencano.plataforma.estudiantes.entidad.EstadoEstudiante;
import co.edu.elencano.plataforma.estudiantes.servicio.EstudianteService;
import co.edu.elencano.plataforma.estudiantes.web.dto.AcudienteDto;
import co.edu.elencano.plataforma.estudiantes.web.dto.EstudianteDto;
import co.edu.elencano.plataforma.estudiantes.web.dto.EstudianteResumenDto;
import co.edu.elencano.plataforma.estudiantes.web.dto.GuardarAcudienteDto;
import co.edu.elencano.plataforma.estudiantes.web.dto.GuardarEstudianteDto;
import co.edu.elencano.plataforma.comun.web.Pagina;
import co.edu.elencano.plataforma.usuarios.entidad.TipoDocumento;
import jakarta.validation.Valid;

/** Ficha de estudiantes y sus acudientes. */
@RestController
@ConsultaEstudiantes
public class EstudianteController {

    private final EstudianteService estudianteService;

    public EstudianteController(EstudianteService estudianteService) {
        this.estudianteService = estudianteService;
    }

    @GetMapping("/api/estudiantes")
    public Pagina<EstudianteResumenDto> buscar(@RequestParam(defaultValue = "") String buscar,
                                               @RequestParam(required = false) EstadoEstudiante estado,
                                               @RequestParam(defaultValue = "0") int pagina,
                                               @RequestParam(defaultValue = "20") int tamano) {
        return Pagina.de(estudianteService.buscar(buscar, estado, pagina, tamano), EstudianteResumenDto::de);
    }

    @GetMapping("/api/estudiantes/{id}")
    public EstudianteDto obtener(@PathVariable Long id) {
        return estudianteService.detalle(id);
    }

    @PostMapping("/api/estudiantes")
    @ResponseStatus(HttpStatus.CREATED)
    @GestionEstudiantes
    public EstudianteDto crear(@Valid @RequestBody GuardarEstudianteDto datos) {
        return estudianteService.crear(datos);
    }

    @PutMapping("/api/estudiantes/{id}")
    @GestionEstudiantes
    public EstudianteDto actualizar(@PathVariable Long id, @Valid @RequestBody GuardarEstudianteDto datos) {
        return estudianteService.actualizar(id, datos);
    }

    @PostMapping("/api/estudiantes/{id}/acudientes")
    @GestionEstudiantes
    public EstudianteDto vincularAcudiente(@PathVariable Long id, @Valid @RequestBody GuardarAcudienteDto datos) {
        return estudianteService.vincularAcudiente(id, datos);
    }

    @PutMapping("/api/estudiantes/{id}/acudientes/{acudienteId}")
    @GestionEstudiantes
    public EstudianteDto actualizarAcudiente(@PathVariable Long id, @PathVariable Long acudienteId,
                                             @Valid @RequestBody GuardarAcudienteDto datos) {
        return estudianteService.actualizarAcudiente(id, acudienteId, datos);
    }

    @DeleteMapping("/api/estudiantes/{id}/acudientes/{acudienteId}")
    @GestionEstudiantes
    public EstudianteDto desvincularAcudiente(@PathVariable Long id, @PathVariable Long acudienteId) {
        return estudianteService.desvincularAcudiente(id, acudienteId);
    }

    @GetMapping("/api/personas/por-documento")
    @GestionEstudiantes
    public AcudienteDto buscarPersona(@RequestParam TipoDocumento tipo, @RequestParam String numero) {
        return estudianteService.buscarPersonaPorDocumento(tipo, numero);
    }
}
