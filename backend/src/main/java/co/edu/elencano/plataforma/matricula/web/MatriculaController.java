package co.edu.elencano.plataforma.matricula.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.elencano.plataforma.matricula.servicio.MatriculaService;
import co.edu.elencano.plataforma.matricula.web.dto.CambiarGrupoDto;
import co.edu.elencano.plataforma.matricula.web.dto.EstudianteDeGrupoDto;
import co.edu.elencano.plataforma.matricula.web.dto.MatriculaDto;
import co.edu.elencano.plataforma.matricula.web.dto.MatricularDto;
import co.edu.elencano.plataforma.matricula.web.dto.RetirarDto;
import co.edu.elencano.plataforma.estudiantes.web.ConsultaEstudiantes;
import co.edu.elencano.plataforma.estudiantes.web.GestionEstudiantes;
import jakarta.validation.Valid;

/** Matriculas de estudiantes y asignacion a grupos. */
@RestController
@ConsultaEstudiantes
public class MatriculaController {

    private final MatriculaService matriculaService;

    public MatriculaController(MatriculaService matriculaService) {
        this.matriculaService = matriculaService;
    }

    @GetMapping("/api/estudiantes/{id}/matriculas")
    public List<MatriculaDto> deEstudiante(@PathVariable Long id) {
        return matriculaService.listarDeEstudiante(id).stream().map(MatriculaDto::de).toList();
    }

    @GetMapping("/api/grupos/{id}/estudiantes")
    public List<EstudianteDeGrupoDto> deGrupo(@PathVariable Long id) {
        return matriculaService.listarDeGrupo(id).stream().map(EstudianteDeGrupoDto::de).toList();
    }

    @PostMapping("/api/matriculas")
    @ResponseStatus(HttpStatus.CREATED)
    @GestionEstudiantes
    public MatriculaDto matricular(@Valid @RequestBody MatricularDto datos) {
        return MatriculaDto.de(matriculaService.matricular(datos.estudianteId(), datos.anioLectivoId(), datos.grupoId()));
    }

    @PutMapping("/api/matriculas/{id}/grupo")
    @GestionEstudiantes
    public MatriculaDto cambiarGrupo(@PathVariable Long id, @RequestBody CambiarGrupoDto datos) {
        return MatriculaDto.de(matriculaService.cambiarGrupo(id, datos.grupoId()));
    }

    @PostMapping("/api/matriculas/{id}/retirar")
    @GestionEstudiantes
    public MatriculaDto retirar(@PathVariable Long id, @Valid @RequestBody RetirarDto datos) {
        return MatriculaDto.de(matriculaService.retirar(id, datos.motivo()));
    }
}
