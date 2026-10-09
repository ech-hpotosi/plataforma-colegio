package co.edu.elencano.plataforma.notas.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.elencano.plataforma.academico.entidad.CargaAcademica;
import co.edu.elencano.plataforma.academico.entidad.Grupo;
import co.edu.elencano.plataforma.academico.entidad.Jornada;
import co.edu.elencano.plataforma.academico.entidad.Periodo;
import co.edu.elencano.plataforma.academico.repositorio.CargaAcademicaRepository;
import co.edu.elencano.plataforma.academico.repositorio.PlanEstudioRepository;
import co.edu.elencano.plataforma.academico.servicio.GrupoService;
import co.edu.elencano.plataforma.asistencia.entidad.EstadoAsistencia;
import co.edu.elencano.plataforma.asistencia.repositorio.DetalleAsistenciaRepository;
import co.edu.elencano.plataforma.comun.excepcion.RecursoNoEncontradoException;
import co.edu.elencano.plataforma.comun.excepcion.ReglaNegocioException;
import co.edu.elencano.plataforma.matricula.entidad.Matricula;
import co.edu.elencano.plataforma.matricula.repositorio.MatriculaRepository;
import co.edu.elencano.plataforma.notas.entidad.Desempeno;
import co.edu.elencano.plataforma.notas.entidad.InformeEstudiante;
import co.edu.elencano.plataforma.notas.repositorio.DescriptorDesempenoRepository;
import co.edu.elencano.plataforma.notas.repositorio.InformeEstudianteRepository;
import co.edu.elencano.plataforma.notas.web.dto.ConfiguracionEvaluacionDto;
import co.edu.elencano.plataforma.notas.web.dto.ConsolidadoNotasDto;
import co.edu.elencano.plataforma.usuarios.entidad.Rol;
import co.edu.elencano.plataforma.usuarios.entidad.Usuario;
import co.edu.elencano.plataforma.usuarios.repositorio.UsuarioRepository;
import co.edu.elencano.plataforma.usuarios.seguridad.UsuarioAutenticado;

/**
 * Boletin de aprendizaje y convivencia de un periodo (SIEE art. 13 y 14). Reune por estudiante y asignatura la
 * nota definitiva con su desempeno, el concepto descriptivo del docente, las faltas justificadas y sin justificar
 * del periodo y la observacion; y el comportamiento como promedio de lo que valoro cada docente.
 * Lo ven los mismos que el consolidado: directivos, secretaria y el director del grupo.
 */
@Service
public class BoletinService {

    private final NotasService notasService;
    private final GrupoService grupoService;
    private final CargaAcademicaRepository cargaRepository;
    private final PlanEstudioRepository planRepository;
    private final MatriculaRepository matriculaRepository;
    private final DescriptorDesempenoRepository descriptorRepository;
    private final InformeEstudianteRepository informeRepository;
    private final DetalleAsistenciaRepository detalleRepository;
    private final UsuarioRepository usuarioRepository;

    public BoletinService(NotasService notasService, GrupoService grupoService, CargaAcademicaRepository cargaRepository,
                          PlanEstudioRepository planRepository, MatriculaRepository matriculaRepository,
                          DescriptorDesempenoRepository descriptorRepository,
                          InformeEstudianteRepository informeRepository, DetalleAsistenciaRepository detalleRepository,
                          UsuarioRepository usuarioRepository) {
        this.notasService = notasService;
        this.grupoService = grupoService;
        this.cargaRepository = cargaRepository;
        this.planRepository = planRepository;
        this.matriculaRepository = matriculaRepository;
        this.descriptorRepository = descriptorRepository;
        this.informeRepository = informeRepository;
        this.detalleRepository = detalleRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Boletines del grupo en el periodo; con matriculaId, solo el de ese estudiante. */
    @Transactional
    public DatosBoletin datos(Long grupoId, Long periodoId, Long matriculaId, UsuarioAutenticado usuario) {
        // El consolidado verifica el acceso al grupo y trae las notas definitivas del periodo
        ConsolidadoNotasDto consolidado = notasService.consolidado(grupoId, periodoId, usuario);
        Grupo grupo = grupoService.obtener(grupoId);
        if (grupo.getGrado().isEvaluacionCualitativa()) {
            throw new ReglaNegocioException(grupo.getGrado().getNombre()
                    + " se evalúa de forma cualitativa; su boletín descriptivo todavía no está disponible");
        }
        Periodo periodo = grupo.getAnioLectivo().getPeriodos().stream()
                .filter(p -> p.getId().equals(periodoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("El periodo " + periodoId + " no es del año del grupo"));

        Map<Long, CargaAcademica> cargas = new HashMap<>();
        cargaRepository.findByGrupoId(grupoId).forEach(c -> cargas.put(c.getId(), c));
        Map<Long, Integer> intensidades = new HashMap<>();
        planRepository.listar(grupo.getAnioLectivo().getId(), grupo.getGrado().getId())
                .forEach(p -> intensidades.put(p.getAsignatura().getId(), p.getIntensidadHoraria()));
        Map<String, String> conceptos = new HashMap<>();
        descriptorRepository.findByCargaGrupoIdAndPeriodoId(grupoId, periodoId)
                .forEach(d -> conceptos.put(d.getCargaId() + "-" + d.getDesempeno(), d.getDescripcion()));
        Map<String, InformeEstudiante> informes = new HashMap<>();
        informeRepository.findByCargaGrupoIdAndPeriodoId(grupoId, periodoId)
                .forEach(i -> informes.put(i.getCargaId() + "-" + i.getMatriculaId(), i));
        Map<String, long[]> faltas = new HashMap<>();
        detalleRepository.contarDePeriodo(grupoId, periodoId).forEach(f -> {
            long[] horas = faltas.computeIfAbsent(f.cargaId() + "-" + f.matriculaId(), k -> new long[2]);
            if (f.estado() == EstadoAsistencia.FALTA) {
                horas[1] += f.horas();
            } else if (f.estado().esInasistencia()) {
                horas[0] += f.horas();
            }
        });
        Map<Long, Matricula> matriculas = new HashMap<>();
        matriculaRepository.listarActivasDeGrupo(grupoId).forEach(m -> matriculas.put(m.getId(), m));
        ConfiguracionEvaluacionDto config = consolidado.configuracion();

        List<DatosBoletin.Estudiante> estudiantes = new ArrayList<>();
        for (ConsolidadoNotasDto.Estudiante e : consolidado.estudiantes()) {
            if (matriculaId != null && !matriculaId.equals(e.matriculaId())) {
                continue;
            }
            Matricula m = matriculas.get(e.matriculaId());
            List<DatosBoletin.Asignatura> asignaturas = new ArrayList<>();
            List<BigDecimal> comportamientos = new ArrayList<>();
            for (ConsolidadoNotasDto.Nota n : e.notas()) {
                CargaAcademica c = cargas.get(n.cargaId());
                InformeEstudiante informe = informes.get(n.cargaId() + "-" + e.matriculaId());
                long[] horas = faltas.getOrDefault(n.cargaId() + "-" + e.matriculaId(), new long[2]);
                if (informe != null && informe.getComportamiento() != null) {
                    comportamientos.add(informe.getComportamiento());
                }
                asignaturas.add(new DatosBoletin.Asignatura(c.getAsignatura().getArea().getNombre(),
                        c.getAsignatura().getNombre(),
                        c.getDocente() == null ? null : c.getDocente().getPersona().getNombreCompleto(),
                        intensidades.get(c.getAsignatura().getId()), n.nota(), n.desempeno(), n.completa(),
                        n.desempeno() == null ? null : conceptos.get(n.cargaId() + "-" + n.desempeno()),
                        informe == null ? null : informe.getObservacion(), horas[0], horas[1]));
            }
            asignaturas.sort(Comparator.comparing(DatosBoletin.Asignatura::area)
                    .thenComparing(DatosBoletin.Asignatura::nombre));
            BigDecimal comportamiento = comportamientos.isEmpty() ? null
                    : comportamientos.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(comportamientos.size()), 1, RoundingMode.HALF_UP);
            estudiantes.add(new DatosBoletin.Estudiante(e.apellidos() + " " + e.nombres(),
                    m == null ? null : m.getEstudiante().getCodigo(),
                    m == null ? null : m.getEstudiante().getPersona().getTipoDocumento() + " "
                            + m.getEstudiante().getPersona().getNumeroDocumento(),
                    asignaturas, comportamiento, comportamiento == null ? null : desempenoDe(comportamiento, config),
                    comportamientos.size()));
        }
        if (matriculaId != null && estudiantes.isEmpty()) {
            throw new RecursoNoEncontradoException("La matrícula " + matriculaId + " no está activa en el grupo");
        }

        List<Usuario> coordinadores = usuarioRepository.listarActivosConRol(Rol.COORDINADOR_ACADEMICO);
        return new DatosBoletin(grupo.getAnioLectivo().getAnio(), periodo.getNumero(), LocalDate.now(),
                grupo.getSede().getNombre(), consolidado.grupo(), nombreJornada(grupo.getJornada()),
                grupo.getDirector() == null ? null : grupo.getDirector().getPersona().getNombreCompleto(),
                coordinadores.size() == 1 ? coordinadores.getFirst().getPersona().getNombreCompleto() : null,
                new DatosBoletin.Escala(config.notaMinima(), config.notaMaxima(), config.notaAprobatoria(),
                        config.limiteAlto(), config.limiteSuperior()),
                estudiantes);
    }

    private static Desempeno desempenoDe(BigDecimal valor, ConfiguracionEvaluacionDto config) {
        if (valor.compareTo(config.notaAprobatoria()) < 0) {
            return Desempeno.BAJO;
        }
        if (valor.compareTo(config.limiteAlto()) < 0) {
            return Desempeno.BASICO;
        }
        return valor.compareTo(config.limiteSuperior()) < 0 ? Desempeno.ALTO : Desempeno.SUPERIOR;
    }

    private static String nombreJornada(Jornada jornada) {
        return switch (jornada) {
            case MANANA -> "Mañana";
            case TARDE -> "Tarde";
            case UNICA -> "Única";
        };
    }
}
