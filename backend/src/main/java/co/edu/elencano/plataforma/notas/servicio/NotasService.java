package co.edu.elencano.plataforma.notas.servicio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.elencano.plataforma.academico.entidad.AnioLectivo;
import co.edu.elencano.plataforma.academico.entidad.CargaAcademica;
import co.edu.elencano.plataforma.academico.entidad.Grupo;
import co.edu.elencano.plataforma.academico.entidad.Periodo;
import co.edu.elencano.plataforma.academico.repositorio.AnioLectivoRepository;
import co.edu.elencano.plataforma.academico.repositorio.CargaAcademicaRepository;
import co.edu.elencano.plataforma.academico.servicio.GrupoService;
import co.edu.elencano.plataforma.comun.excepcion.RecursoNoEncontradoException;
import co.edu.elencano.plataforma.comun.excepcion.ReglaNegocioException;
import co.edu.elencano.plataforma.matricula.entidad.Matricula;
import co.edu.elencano.plataforma.matricula.repositorio.MatriculaRepository;
import co.edu.elencano.plataforma.notas.entidad.ActividadEvaluativa;
import co.edu.elencano.plataforma.notas.entidad.ConfiguracionEvaluacion;
import co.edu.elencano.plataforma.notas.entidad.Desempeno;
import co.edu.elencano.plataforma.notas.entidad.Dimension;
import co.edu.elencano.plataforma.notas.entidad.NotaActividad;
import co.edu.elencano.plataforma.notas.repositorio.ActividadEvaluativaRepository;
import co.edu.elencano.plataforma.notas.repositorio.ConfiguracionEvaluacionRepository;
import co.edu.elencano.plataforma.notas.repositorio.NotaActividadRepository;
import co.edu.elencano.plataforma.notas.servicio.CalculoNotas.NotaPeriodo;
import co.edu.elencano.plataforma.notas.servicio.CalculoNotas.NotaPonderada;
import co.edu.elencano.plataforma.notas.web.dto.ActividadDto;
import co.edu.elencano.plataforma.notas.web.dto.ActividadEntradaDto;
import co.edu.elencano.plataforma.notas.web.dto.CargaNotasDto;
import co.edu.elencano.plataforma.notas.web.dto.ConfiguracionEvaluacionDto;
import co.edu.elencano.plataforma.notas.web.dto.ConsolidadoNotasDto;
import co.edu.elencano.plataforma.notas.web.dto.GuardarConfiguracionDto;
import co.edu.elencano.plataforma.notas.web.dto.GuardarNotasDto;
import co.edu.elencano.plataforma.notas.web.dto.PeriodoNotasDto;
import co.edu.elencano.plataforma.notas.web.dto.PlanillaNotasDto;
import co.edu.elencano.plataforma.usuarios.entidad.Persona;
import co.edu.elencano.plataforma.usuarios.entidad.Rol;
import co.edu.elencano.plataforma.usuarios.seguridad.UsuarioAutenticado;

/**
 * Notas por actividad, nota del periodo por dimensiones y consolidado del grupo segun el SIEE.
 *
 * Quien puede hacer que (igual que en asistencia):
 * - Registrar notas y actividades: el docente de la carga, el administrador o el coordinador academico.
 * - Ver el consolidado de un grupo: directivos, secretaria y el director del grupo.
 * - Cambiar la escala y los pesos: administrador y coordinador academico.
 */
@Service
public class NotasService {

    private static final Set<Rol> REGISTRAN_CUALQUIER_CLASE = EnumSet.of(Rol.ADMINISTRADOR, Rol.COORDINADOR_ACADEMICO);
    private static final Set<Rol> VEN_TODOS_LOS_GRUPOS =
            EnumSet.of(Rol.ADMINISTRADOR, Rol.RECTOR, Rol.COORDINADOR_ACADEMICO, Rol.SECRETARIA);

    private final ConfiguracionEvaluacionRepository configuracionRepository;
    private final ActividadEvaluativaRepository actividadRepository;
    private final NotaActividadRepository notaRepository;
    private final CargaAcademicaRepository cargaRepository;
    private final AnioLectivoRepository anioRepository;
    private final GrupoService grupoService;
    private final MatriculaRepository matriculaRepository;

    public NotasService(ConfiguracionEvaluacionRepository configuracionRepository,
                        ActividadEvaluativaRepository actividadRepository, NotaActividadRepository notaRepository,
                        CargaAcademicaRepository cargaRepository, AnioLectivoRepository anioRepository,
                        GrupoService grupoService, MatriculaRepository matriculaRepository) {
        this.configuracionRepository = configuracionRepository;
        this.actividadRepository = actividadRepository;
        this.notaRepository = notaRepository;
        this.cargaRepository = cargaRepository;
        this.anioRepository = anioRepository;
        this.grupoService = grupoService;
        this.matriculaRepository = matriculaRepository;
    }

    // ---------- Configuracion ----------

    @Transactional
    public ConfiguracionEvaluacionDto obtenerConfiguracion(Long anioId) {
        AnioLectivo anio = anioRepository.findById(anioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el año lectivo " + anioId));
        return ConfiguracionEvaluacionDto.de(configuracionDe(anio));
    }

    @Transactional
    public ConfiguracionEvaluacionDto guardarConfiguracion(Long anioId, GuardarConfiguracionDto datos) {
        AnioLectivo anio = anioRepository.findById(anioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el año lectivo " + anioId));
        if (anio.estaCerrado()) {
            throw new ReglaNegocioException("El año lectivo " + anio.getAnio() + " está cerrado y no admite cambios");
        }
        boolean ordenada = datos.notaMinima().compareTo(datos.notaAprobatoria()) < 0
                && datos.notaAprobatoria().compareTo(datos.limiteAlto()) < 0
                && datos.limiteAlto().compareTo(datos.limiteSuperior()) < 0
                && datos.limiteSuperior().compareTo(datos.notaMaxima()) <= 0;
        if (!ordenada) {
            throw new ReglaNegocioException("La escala debe ir en orden: nota mínima, nota para aprobar, "
                    + "inicio de Alto, inicio de Superior y nota máxima");
        }
        if (datos.pesoSaber() + datos.pesoHacer() + datos.pesoSer() != 100) {
            throw new ReglaNegocioException("Los pesos de Saber, Hacer y Ser deben sumar 100 %");
        }
        ConfiguracionEvaluacion config = configuracionDe(anio);
        config.actualizar(datos.notaMinima(), datos.notaMaxima(), datos.notaAprobatoria(), datos.limiteAlto(),
                datos.limiteSuperior(), datos.pesoSaber(), datos.pesoHacer(), datos.pesoSer());
        return ConfiguracionEvaluacionDto.de(config);
    }

    /** La configuracion se crea con los valores por defecto la primera vez que se consulta. */
    private ConfiguracionEvaluacion configuracionDe(AnioLectivo anio) {
        return configuracionRepository.findByAnioLectivoId(anio.getId())
                .orElseGet(() -> configuracionRepository.save(ConfiguracionEvaluacion.porDefecto(anio)));
    }

    // ---------- Planilla del docente ----------

    @Transactional(readOnly = true)
    public List<CargaNotasDto> listarCargas(UsuarioAutenticado usuario) {
        List<CargaAcademica> cargas = tieneAlguno(usuario, REGISTRAN_CUALQUIER_CLASE)
                ? cargaRepository.listarAbiertas()
                : cargaRepository.listarAbiertasDeDocente(usuario.getPersonaId());
        return cargas.stream().map(CargaNotasDto::de).toList();
    }

    @Transactional
    public PlanillaNotasDto planilla(Long cargaId, Long periodoId, UsuarioAutenticado usuario) {
        CargaAcademica carga = cargaQueRegistra(cargaId, usuario);
        Periodo periodo = periodoDe(carga, periodoId);
        ConfiguracionEvaluacion config = configuracionDe(carga.getGrupo().getAnioLectivo());
        List<ActividadEvaluativa> actividades = actividadRepository.listar(cargaId, periodoId);
        Map<Long, List<NotaActividad>> porMatricula = notaRepository.listarDeCargaYPeriodo(cargaId, periodoId).stream()
                .collect(Collectors.groupingBy(NotaActividad::getMatriculaId));

        List<PlanillaNotasDto.Fila> filas = matriculaRepository.listarActivasDeGrupo(carga.getGrupo().getId()).stream()
                .map(m -> {
                    Persona p = m.getEstudiante().getPersona();
                    List<NotaActividad> notas = porMatricula.getOrDefault(m.getId(), List.of());
                    Map<Long, BigDecimal> valores = new HashMap<>();
                    notas.forEach(n -> valores.put(n.getActividad().getId(), n.getValor()));
                    NotaPeriodo calculo = CalculoNotas.notaPeriodo(porDimension(notas), config);
                    return new PlanillaNotasDto.Fila(m.getId(), p.getNombres(), p.getApellidos(), valores,
                            calculo.promedios().get(Dimension.SABER), calculo.promedios().get(Dimension.HACER),
                            calculo.promedios().get(Dimension.SER), calculo.nota(),
                            calculo.nota() == null ? null : config.desempenoDe(calculo.nota()), calculo.completa());
                })
                .toList();
        return new PlanillaNotasDto(cargaId, nombreGrupo(carga.getGrupo()), carga.getAsignatura().getNombre(),
                periodoId, periodo.getNumero(), esEditable(periodo), ConfiguracionEvaluacionDto.de(config),
                actividades.stream().map(ActividadDto::de).toList(), filas);
    }

    @Transactional
    public PlanillaNotasDto crearActividad(Long cargaId, Long periodoId, ActividadEntradaDto datos,
                                           UsuarioAutenticado usuario) {
        CargaAcademica carga = cargaQueRegistra(cargaId, usuario);
        Periodo periodo = periodoDe(carga, periodoId);
        verificarEditable(periodo);
        ActividadEvaluativa actividad = new ActividadEvaluativa(carga, periodo);
        actividad.actualizar(datos.dimension(), datos.nombre().trim(), datos.fecha());
        actividadRepository.save(actividad);
        return planilla(cargaId, periodoId, usuario);
    }

    @Transactional
    public PlanillaNotasDto modificarActividad(Long actividadId, ActividadEntradaDto datos, UsuarioAutenticado usuario) {
        ActividadEvaluativa actividad = actividadQueRegistra(actividadId, usuario);
        actividad.actualizar(datos.dimension(), datos.nombre().trim(), datos.fecha());
        return planilla(actividad.getCarga().getId(), actividad.getPeriodo().getId(), usuario);
    }

    /** Borra la actividad con sus notas. */
    @Transactional
    public PlanillaNotasDto eliminarActividad(Long actividadId, UsuarioAutenticado usuario) {
        ActividadEvaluativa actividad = actividadQueRegistra(actividadId, usuario);
        Long cargaId = actividad.getCarga().getId();
        Long periodoId = actividad.getPeriodo().getId();
        notaRepository.deleteAll(notaRepository.listarDeCargaYPeriodo(cargaId, periodoId).stream()
                .filter(n -> n.getActividad().getId().equals(actividadId))
                .toList());
        actividadRepository.delete(actividad);
        actividadRepository.flush();
        return planilla(cargaId, periodoId, usuario);
    }

    /** Guarda las notas enviadas; las que no vengan no cambian y un valor nulo borra la nota. */
    @Transactional
    public PlanillaNotasDto guardarNotas(Long cargaId, Long periodoId, GuardarNotasDto datos,
                                         UsuarioAutenticado usuario) {
        CargaAcademica carga = cargaQueRegistra(cargaId, usuario);
        Periodo periodo = periodoDe(carga, periodoId);
        verificarEditable(periodo);
        ConfiguracionEvaluacion config = configuracionDe(carga.getGrupo().getAnioLectivo());
        Map<Long, ActividadEvaluativa> actividades = actividadRepository.listar(cargaId, periodoId).stream()
                .collect(Collectors.toMap(ActividadEvaluativa::getId, Function.identity()));
        Map<Long, Matricula> delGrupo = matriculaRepository.listarActivasDeGrupo(carga.getGrupo().getId()).stream()
                .collect(Collectors.toMap(Matricula::getId, Function.identity()));
        Map<String, NotaActividad> existentes = notaRepository.listarDeCargaYPeriodo(cargaId, periodoId).stream()
                .collect(Collectors.toMap(n -> clave(n.getActividad().getId(), n.getMatriculaId()), Function.identity()));

        for (GuardarNotasDto.Item item : datos.notas()) {
            ActividadEvaluativa actividad = actividades.get(item.actividadId());
            if (actividad == null) {
                throw new ReglaNegocioException("La actividad " + item.actividadId() + " no es de esta clase y periodo");
            }
            Matricula matricula = delGrupo.get(item.matriculaId());
            if (matricula == null) {
                throw new ReglaNegocioException("La matrícula " + item.matriculaId() + " no está activa en el grupo");
            }
            NotaActividad existente = existentes.get(clave(actividad.getId(), matricula.getId()));
            if (item.valor() == null) {
                if (existente != null) {
                    notaRepository.delete(existente);
                }
                continue;
            }
            if (item.valor().scale() > 1 || !config.estaEnEscala(item.valor())) {
                throw new ReglaNegocioException("La nota de " + matricula.getEstudiante().getPersona().getNombreCompleto()
                        + " debe estar entre " + config.getNotaMinima() + " y " + config.getNotaMaxima()
                        + " con una sola decimal");
            }
            if (existente != null) {
                existente.setValor(item.valor());
            } else {
                notaRepository.save(new NotaActividad(actividad, matricula, item.valor()));
            }
        }
        notaRepository.flush();
        return planilla(cargaId, periodoId, usuario);
    }

    // ---------- Consolidado del grupo ----------

    @Transactional
    public ConsolidadoNotasDto consolidado(Long grupoId, Long periodoId, UsuarioAutenticado usuario) {
        Grupo grupo = grupoService.obtener(grupoId);
        boolean esDirector = grupo.getDirector() != null && grupo.getDirector().getId().equals(usuario.getPersonaId());
        if (!esDirector && !tieneAlguno(usuario, VEN_TODOS_LOS_GRUPOS)) {
            throw new AccessDeniedException("Sin acceso al grupo");
        }
        List<Periodo> periodos = grupo.getAnioLectivo().getPeriodos().stream()
                .sorted(Comparator.comparingInt(Periodo::getNumero))
                .toList();
        if (periodoId != null && periodos.stream().noneMatch(p -> p.getId().equals(periodoId))) {
            throw new RecursoNoEncontradoException("El periodo " + periodoId + " no es del año del grupo");
        }
        ConfiguracionEvaluacion config = configuracionDe(grupo.getAnioLectivo());
        List<CargaAcademica> cargas = cargaRepository.findByGrupoId(grupoId).stream()
                .sorted(Comparator.comparing(c -> c.getAsignatura().getNombre()))
                .toList();

        // matricula -> carga -> periodo -> dimension -> notas
        Map<Long, Map<Long, Map<Long, Map<Dimension, List<BigDecimal>>>>> arbol = new HashMap<>();
        for (NotaActividad n : notaRepository.listarDeGrupo(grupoId)) {
            ActividadEvaluativa a = n.getActividad();
            arbol.computeIfAbsent(n.getMatriculaId(), k -> new HashMap<>())
                    .computeIfAbsent(a.getCarga().getId(), k -> new HashMap<>())
                    .computeIfAbsent(a.getPeriodo().getId(), k -> new EnumMap<>(Dimension.class))
                    .computeIfAbsent(a.getDimension(), k -> new ArrayList<>())
                    .add(n.getValor());
        }

        List<ConsolidadoNotasDto.Estudiante> estudiantes = matriculaRepository.listarActivasDeGrupo(grupoId).stream()
                .map(m -> {
                    Persona p = m.getEstudiante().getPersona();
                    List<ConsolidadoNotasDto.Nota> notas = new ArrayList<>();
                    for (CargaAcademica c : cargas) {
                        Map<Long, Map<Dimension, List<BigDecimal>>> porPeriodo =
                                arbol.getOrDefault(m.getId(), Map.of()).getOrDefault(c.getId(), Map.of());
                        notas.add(notaAsignatura(c.getId(), porPeriodo, periodos, periodoId, config));
                    }
                    int enBajo = (int) notas.stream().filter(n -> n.desempeno() == Desempeno.BAJO).count();
                    return new ConsolidadoNotasDto.Estudiante(m.getId(), p.getNombres(), p.getApellidos(), notas, enBajo);
                })
                .toList();

        return new ConsolidadoNotasDto(grupoId, nombreGrupo(grupo), periodoId,
                periodos.stream().map(PeriodoNotasDto::de).toList(), ConfiguracionEvaluacionDto.de(config),
                cargas.stream().map(c -> new ConsolidadoNotasDto.Asignatura(c.getId(), c.getAsignatura().getNombre(),
                        c.getDocente().getPersona().getNombreCompleto())).toList(),
                estudiantes);
    }

    /** Nota de una asignatura en un periodo, o acumulada del anio si periodoId es nulo. */
    private ConsolidadoNotasDto.Nota notaAsignatura(Long cargaId, Map<Long, Map<Dimension, List<BigDecimal>>> porPeriodo,
                                                    List<Periodo> periodos, Long periodoId,
                                                    ConfiguracionEvaluacion config) {
        BigDecimal nota;
        boolean completa;
        if (periodoId != null) {
            NotaPeriodo calculo = CalculoNotas.notaPeriodo(porPeriodo.getOrDefault(periodoId, Map.of()), config);
            nota = calculo.nota();
            completa = calculo.completa();
        } else {
            List<NotaPonderada> ponderadas = new ArrayList<>();
            completa = true;
            for (Periodo p : periodos) {
                NotaPeriodo calculo = CalculoNotas.notaPeriodo(porPeriodo.getOrDefault(p.getId(), Map.of()), config);
                if (calculo.nota() != null) {
                    ponderadas.add(new NotaPonderada(calculo.nota(), p.getPorcentaje()));
                }
                completa &= calculo.completa();
            }
            nota = CalculoNotas.ponderado(ponderadas);
        }
        return new ConsolidadoNotasDto.Nota(cargaId, nota, nota == null ? null : config.desempenoDe(nota),
                completa && nota != null);
    }

    // ---------- Apoyo ----------

    private static Map<Dimension, List<BigDecimal>> porDimension(List<NotaActividad> notas) {
        Map<Dimension, List<BigDecimal>> mapa = new EnumMap<>(Dimension.class);
        notas.forEach(n -> mapa.computeIfAbsent(n.getActividad().getDimension(), k -> new ArrayList<>()).add(n.getValor()));
        return mapa;
    }

    private CargaAcademica cargaQueRegistra(Long cargaId, UsuarioAutenticado usuario) {
        CargaAcademica carga = cargaRepository.findById(cargaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la carga académica " + cargaId));
        if (!tieneAlguno(usuario, REGISTRAN_CUALQUIER_CLASE)
                && !carga.getDocente().getId().equals(usuario.getPersonaId())) {
            throw new AccessDeniedException("La carga no es del docente");
        }
        if (carga.getGrupo().getGrado().isEvaluacionCualitativa()) {
            throw new ReglaNegocioException(carga.getGrupo().getGrado().getNombre()
                    + " se evalúa de forma cualitativa, sin notas numéricas (SIEE art. 4.3.1)");
        }
        return carga;
    }

    private ActividadEvaluativa actividadQueRegistra(Long actividadId, UsuarioAutenticado usuario) {
        ActividadEvaluativa actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la actividad " + actividadId));
        cargaQueRegistra(actividad.getCarga().getId(), usuario);
        verificarEditable(actividad.getPeriodo());
        return actividad;
    }

    private static Periodo periodoDe(CargaAcademica carga, Long periodoId) {
        return carga.getGrupo().getAnioLectivo().getPeriodos().stream()
                .filter(p -> p.getId().equals(periodoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("El periodo " + periodoId
                        + " no es del año de la clase"));
    }

    private static boolean esEditable(Periodo periodo) {
        return !periodo.isCerrado() && !periodo.getAnioLectivo().estaCerrado();
    }

    private static void verificarEditable(Periodo periodo) {
        if (periodo.getAnioLectivo().estaCerrado()) {
            throw new ReglaNegocioException("El año lectivo " + periodo.getAnioLectivo().getAnio()
                    + " está cerrado y no admite cambios");
        }
        if (periodo.isCerrado()) {
            throw new ReglaNegocioException("El periodo " + periodo.getNumero() + " está cerrado");
        }
    }

    private static String clave(Long actividadId, Long matriculaId) {
        return actividadId + "|" + matriculaId;
    }

    private static boolean tieneAlguno(UsuarioAutenticado usuario, Set<Rol> roles) {
        return usuario.getRoles().stream().anyMatch(roles::contains);
    }

    private static String nombreGrupo(Grupo grupo) {
        return grupo.getGrado().getNombre() + " " + grupo.getNombre();
    }
}
