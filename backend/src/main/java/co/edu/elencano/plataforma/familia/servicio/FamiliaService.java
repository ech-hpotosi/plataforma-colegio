package co.edu.elencano.plataforma.familia.servicio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.elencano.plataforma.academico.entidad.Grupo;
import co.edu.elencano.plataforma.academico.entidad.Periodo;
import co.edu.elencano.plataforma.asistencia.entidad.EstadoAsistencia;
import co.edu.elencano.plataforma.asistencia.servicio.AsistenciaService;
import co.edu.elencano.plataforma.asistencia.web.dto.NovedadAsistenciaDto;
import co.edu.elencano.plataforma.comun.excepcion.RecursoNoEncontradoException;
import co.edu.elencano.plataforma.comun.excepcion.ReglaNegocioException;
import co.edu.elencano.plataforma.estudiantes.entidad.Estudiante;
import co.edu.elencano.plataforma.estudiantes.entidad.EstudianteAcudiente;
import co.edu.elencano.plataforma.estudiantes.servicio.EstudianteService;
import co.edu.elencano.plataforma.familia.web.dto.EstudianteFamiliaDto;
import co.edu.elencano.plataforma.familia.web.dto.ResumenFamiliaDto;
import co.edu.elencano.plataforma.matricula.entidad.Matricula;
import co.edu.elencano.plataforma.matricula.repositorio.MatriculaRepository;
import co.edu.elencano.plataforma.notas.servicio.BoletinPdf;
import co.edu.elencano.plataforma.notas.servicio.BoletinService;
import co.edu.elencano.plataforma.notas.servicio.DatosBoletin;
import co.edu.elencano.plataforma.notas.servicio.NotasService;
import co.edu.elencano.plataforma.notas.web.dto.ConsolidadoNotasDto;
import co.edu.elencano.plataforma.usuarios.entidad.Rol;
import co.edu.elencano.plataforma.usuarios.seguridad.UsuarioAutenticado;

/**
 * Lo que ve una familia en la plataforma. Un acudiente consulta a los estudiantes en los que tiene el permiso de
 * consulta (se da al aprobar su matricula en linea o lo activa secretaria); un estudiante con cuenta se consulta a si
 * mismo. Cualquier otro estudiante responde como si no existiera, para no revelar quien esta matriculado.
 */
@Service
public class FamiliaService {

    private final EstudianteService estudianteService;
    private final MatriculaRepository matriculaRepository;
    private final NotasService notasService;
    private final BoletinService boletinService;
    private final BoletinPdf boletinPdf;
    private final AsistenciaService asistenciaService;

    public FamiliaService(EstudianteService estudianteService, MatriculaRepository matriculaRepository,
                          NotasService notasService, BoletinService boletinService, BoletinPdf boletinPdf,
                          AsistenciaService asistenciaService) {
        this.estudianteService = estudianteService;
        this.matriculaRepository = matriculaRepository;
        this.notasService = notasService;
        this.boletinService = boletinService;
        this.boletinPdf = boletinPdf;
        this.asistenciaService = asistenciaService;
    }

    /** Boletin listo para descargar, con el nombre del archivo. */
    public record Boletin(String archivo, byte[] contenido) {
    }

    @Transactional(readOnly = true)
    public List<EstudianteFamiliaDto> estudiantes(UsuarioAutenticado usuario) {
        List<EstudianteFamiliaDto> lista = new ArrayList<>();
        propio(usuario).ifPresent(e -> lista.add(dto(e, null)));
        for (EstudianteAcudiente v : estudianteService.vinculosConAcceso(usuario.getPersonaId())) {
            lista.add(dto(v.getEstudiante(), v.getParentesco().name()));
        }
        return lista;
    }

    /** Notas y asistencia del anio de la matricula; sin matriculaId, la matricula activa mas reciente. */
    @Transactional
    public ResumenFamiliaDto resumen(Long estudianteId, Long matriculaId, UsuarioAutenticado usuario) {
        Estudiante estudiante = verificarAcceso(estudianteId, usuario);
        Matricula matricula = matricula(estudiante, matriculaId);
        String nombre = estudiante.getPersona().getNombres() + " " + estudiante.getPersona().getApellidos();
        Grupo grupo = matricula.getGrupo();
        List<NovedadAsistenciaDto> novedades = asistenciaService.novedadesDeMatricula(matricula.getId());
        if (grupo == null) {
            // Matriculado pero todavia sin grupo: no hay asignaturas ni notas
            return new ResumenFamiliaDto(estudiante.getId(), nombre, matricula.getId(),
                    matricula.getAnioLectivo().getAnio(), null, null, null, null, List.of(), List.of(), novedades,
                    false);
        }

        ConsolidadoNotasDto anual = notasService.calcularConsolidado(grupo.getId(), null);
        Map<Long, ConsolidadoNotasDto.Nota> definitivas = notasDe(anual, matricula.getId());
        Map<Long, Map<Long, ConsolidadoNotasDto.Nota>> porPeriodo = new HashMap<>();
        for (Periodo p : grupo.getAnioLectivo().getPeriodos()) {
            porPeriodo.put(p.getId(), notasDe(notasService.calcularConsolidado(grupo.getId(), p.getId()),
                    matricula.getId()));
        }

        // Horas de inasistencia por asignatura: justificadas, sin justificar y retardos
        Map<String, long[]> horas = new HashMap<>();
        for (NovedadAsistenciaDto n : novedades) {
            long[] h = horas.computeIfAbsent(n.asignatura(), k -> new long[3]);
            if (n.estado() == EstadoAsistencia.FALTA) {
                h[1] += n.horas();
            } else if (n.estado() == EstadoAsistencia.RETARDO) {
                h[2] += n.horas();
            } else {
                h[0] += n.horas();
            }
        }

        List<ResumenFamiliaDto.Asignatura> asignaturas = new ArrayList<>();
        for (ConsolidadoNotasDto.Asignatura a : anual.asignaturas()) {
            List<ResumenFamiliaDto.NotaPeriodo> notas = anual.periodos().stream()
                    .map(p -> {
                        ConsolidadoNotasDto.Nota n = porPeriodo.get(p.id()).get(a.cargaId());
                        return new ResumenFamiliaDto.NotaPeriodo(p.id(), n == null ? null : n.nota(),
                                n == null ? null : n.desempeno(), n != null && n.completa());
                    })
                    .toList();
            ConsolidadoNotasDto.Nota definitiva = definitivas.get(a.cargaId());
            long[] h = horas.getOrDefault(a.nombre(), new long[3]);
            asignaturas.add(new ResumenFamiliaDto.Asignatura(a.nombre(), a.docente(), notas,
                    definitiva == null ? null : definitiva.nota(), definitiva == null ? null : definitiva.desempeno(),
                    definitiva != null && definitiva.completa(), h[0], h[1], h[2]));
        }
        return new ResumenFamiliaDto(estudiante.getId(), nombre, matricula.getId(), anual.configuracion().anio(),
                anual.grupo(), grupo.getSede().getNombre(),
                grupo.getDirector() == null ? null : grupo.getDirector().getPersona().getNombreCompleto(),
                anual.configuracion(), anual.periodos(), asignaturas, novedades,
                !grupo.getGrado().isEvaluacionCualitativa());
    }

    /** Boletin de un periodo cerrado del anio de la matricula, como el que se entrega impreso. */
    @Transactional
    public Boletin boletin(Long estudianteId, Long matriculaId, Long periodoId, UsuarioAutenticado usuario) {
        Estudiante estudiante = verificarAcceso(estudianteId, usuario);
        Matricula matricula = matricula(estudiante, matriculaId);
        if (matricula.getGrupo() == null) {
            throw new ReglaNegocioException("El estudiante todavía no tiene grupo asignado");
        }
        Periodo periodo = matricula.getAnioLectivo().getPeriodos().stream()
                .filter(p -> p.getId().equals(periodoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("El periodo no es del año de la matrícula"));
        if (!periodo.isCerrado()) {
            throw new ReglaNegocioException("El boletín del periodo " + periodo.getNumero()
                    + " estará disponible cuando el colegio cierre el periodo");
        }
        DatosBoletin datos = boletinService.datos(matricula.getGrupo().getId(), periodoId, matricula.getId());
        String archivo = ("boletin " + estudiante.getPersona().getApellidos() + " periodo " + periodo.getNumero() + " "
                + datos.anio()).replaceAll("[^A-Za-z0-9]+", "-").toLowerCase() + ".pdf";
        return new Boletin(archivo, boletinPdf.generar(datos));
    }

    private Estudiante verificarAcceso(Long estudianteId, UsuarioAutenticado usuario) {
        Optional<Estudiante> propio = propio(usuario).filter(e -> e.getId().equals(estudianteId));
        if (propio.isPresent()) {
            return propio.get();
        }
        return estudianteService.vinculosConAcceso(usuario.getPersonaId()).stream()
                .map(EstudianteAcudiente::getEstudiante)
                .filter(e -> e.getId().equals(estudianteId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el estudiante " + estudianteId));
    }

    /** El estudiante con cuenta propia se consulta a si mismo. */
    private Optional<Estudiante> propio(UsuarioAutenticado usuario) {
        if (!usuario.getRoles().contains(Rol.ESTUDIANTE)) {
            return Optional.empty();
        }
        return estudianteService.buscarPorId(usuario.getPersonaId());
    }

    private Matricula matricula(Estudiante estudiante, Long matriculaId) {
        return activas(estudiante).stream()
                .filter(m -> matriculaId == null || m.getId().equals(matriculaId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(matriculaId == null
                        ? "El estudiante no tiene una matrícula activa"
                        : "No existe la matrícula " + matriculaId));
    }

    private List<Matricula> activas(Estudiante estudiante) {
        return matriculaRepository.listarDeEstudiante(estudiante.getId()).stream()
                .filter(Matricula::estaActiva)
                .sorted(Comparator.comparingInt((Matricula m) -> m.getAnioLectivo().getAnio()).reversed())
                .toList();
    }

    private EstudianteFamiliaDto dto(Estudiante e, String parentesco) {
        List<EstudianteFamiliaDto.Matricula> matriculas = activas(e).stream()
                .map(m -> new EstudianteFamiliaDto.Matricula(m.getId(), m.getAnioLectivo().getAnio(),
                        m.getGrupo() == null ? null : m.getGrupo().getGrado().getNombre() + " " + m.getGrupo().getNombre(),
                        m.getGrupo() == null ? null : m.getGrupo().getSede().getNombre()))
                .toList();
        return new EstudianteFamiliaDto(e.getId(), e.getPersona().getNombres(), e.getPersona().getApellidos(),
                parentesco, matriculas);
    }

    private static Map<Long, ConsolidadoNotasDto.Nota> notasDe(ConsolidadoNotasDto consolidado, Long matriculaId) {
        Map<Long, ConsolidadoNotasDto.Nota> notas = new HashMap<>();
        consolidado.estudiantes().stream()
                .filter(e -> e.matriculaId().equals(matriculaId))
                .findFirst()
                .ifPresent(e -> e.notas().forEach(n -> notas.put(n.cargaId(), n)));
        return notas;
    }
}
