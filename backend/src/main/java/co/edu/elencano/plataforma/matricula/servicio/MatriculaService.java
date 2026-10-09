package co.edu.elencano.plataforma.matricula.servicio;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.elencano.plataforma.matricula.entidad.EstadoMatricula;
import co.edu.elencano.plataforma.matricula.entidad.Matricula;
import co.edu.elencano.plataforma.matricula.repositorio.MatriculaRepository;
import co.edu.elencano.plataforma.academico.entidad.AnioLectivo;
import co.edu.elencano.plataforma.academico.entidad.Grupo;
import co.edu.elencano.plataforma.academico.servicio.AnioLectivoService;
import co.edu.elencano.plataforma.academico.servicio.GrupoService;
import co.edu.elencano.plataforma.comun.excepcion.RecursoNoEncontradoException;
import co.edu.elencano.plataforma.comun.excepcion.ReglaNegocioException;
import co.edu.elencano.plataforma.estudiantes.entidad.EstadoEstudiante;
import co.edu.elencano.plataforma.estudiantes.entidad.Estudiante;
import co.edu.elencano.plataforma.estudiantes.servicio.EstudianteService;

/**
 * Matricula directa hecha por secretaria y asignacion de grupo. La matricula en linea
 * (solicitud con documentos) llegara a este mismo registro cuando se apruebe.
 */
@Service
public class MatriculaService {

    private final MatriculaRepository matriculaRepository;
    private final EstudianteService estudianteService;
    private final AnioLectivoService anioService;
    private final GrupoService grupoService;

    public MatriculaService(MatriculaRepository matriculaRepository, EstudianteService estudianteService,
                            AnioLectivoService anioService, GrupoService grupoService) {
        this.matriculaRepository = matriculaRepository;
        this.estudianteService = estudianteService;
        this.anioService = anioService;
        this.grupoService = grupoService;
    }

    @Transactional(readOnly = true)
    public List<Matricula> listarDeEstudiante(Long estudianteId) {
        estudianteService.obtener(estudianteId);
        return matriculaRepository.listarDeEstudiante(estudianteId);
    }

    @Transactional(readOnly = true)
    public List<Matricula> listarDeGrupo(Long grupoId) {
        grupoService.obtener(grupoId);
        return matriculaRepository.listarActivasDeGrupo(grupoId);
    }

    /**
     * Matricula al estudiante en el anio. Si ya tuvo una matricula ese anio y se habia retirado,
     * se reactiva la misma. El estudiante queda en estado ACTIVO.
     */
    @Transactional
    public Matricula matricular(Long estudianteId, Long anioId, Long grupoId) {
        Estudiante estudiante = estudianteService.obtener(estudianteId);
        AnioLectivo anio = anioService.obtenerAbierto(anioId);
        Matricula matricula = matriculaRepository.findByEstudianteIdAndAnioLectivoId(estudianteId, anioId)
                .orElse(null);
        if (matricula != null && matricula.estaActiva()) {
            throw new ReglaNegocioException(estudiante.getPersona().getNombreCompleto()
                    + " ya está matriculado en el año " + anio.getAnio());
        }
        if (matricula == null) {
            matricula = new Matricula(estudiante, anio, LocalDate.now());
        } else {
            matricula.reactivar(LocalDate.now());
        }
        matricula.setGrupo(grupoValido(grupoId, anio, null));
        estudiante.setEstado(EstadoEstudiante.ACTIVO);
        return matriculaRepository.save(matricula);
    }

    @Transactional
    public Matricula cambiarGrupo(Long matriculaId, Long grupoId) {
        Matricula matricula = obtenerActiva(matriculaId);
        AnioLectivo anio = anioService.obtenerAbierto(matricula.getAnioLectivo().getId());
        matricula.setGrupo(grupoValido(grupoId, anio, matricula.getGrupo()));
        return matricula;
    }

    @Transactional
    public Matricula retirar(Long matriculaId, String motivo) {
        Matricula matricula = obtenerActiva(matriculaId);
        anioService.obtenerAbierto(matricula.getAnioLectivo().getId());
        matricula.retirar(LocalDate.now(), motivo.trim());
        matricula.getEstudiante().setEstado(EstadoEstudiante.RETIRADO);
        return matricula;
    }

    private Matricula obtenerActiva(Long id) {
        Matricula matricula = matriculaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la matrícula " + id));
        if (!matricula.estaActiva()) {
            throw new ReglaNegocioException("La matrícula está retirada");
        }
        return matricula;
    }

    /** Valida que el grupo sea del anio y tenga cupo. Devuelve nulo si no se indico grupo. */
    private Grupo grupoValido(Long grupoId, AnioLectivo anio, Grupo actual) {
        if (grupoId == null) {
            return null;
        }
        Grupo grupo = grupoService.obtener(grupoId);
        if (!grupo.getAnioLectivo().getId().equals(anio.getId())) {
            throw new ReglaNegocioException("El grupo " + grupo.getNombre() + " no es del año " + anio.getAnio());
        }
        boolean mismoGrupo = actual != null && actual.getId().equals(grupoId);
        if (!mismoGrupo && matriculaRepository.countByGrupoIdAndEstado(grupoId, EstadoMatricula.ACTIVA) >= grupo.getCupo()) {
            throw new ReglaNegocioException("El grupo " + grupo.getGrado().getNombre() + " " + grupo.getNombre()
                    + " ya tiene el cupo completo (" + grupo.getCupo() + ")");
        }
        return grupo;
    }
}
