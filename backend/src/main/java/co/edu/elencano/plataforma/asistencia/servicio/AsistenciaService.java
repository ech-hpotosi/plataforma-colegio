package co.edu.elencano.plataforma.asistencia.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import co.edu.elencano.plataforma.academico.entidad.AnioLectivo;
import co.edu.elencano.plataforma.academico.entidad.CargaAcademica;
import co.edu.elencano.plataforma.academico.entidad.Grupo;
import co.edu.elencano.plataforma.academico.entidad.Periodo;
import co.edu.elencano.plataforma.academico.entidad.PlanEstudio;
import co.edu.elencano.plataforma.academico.repositorio.CargaAcademicaRepository;
import co.edu.elencano.plataforma.academico.repositorio.GrupoRepository;
import co.edu.elencano.plataforma.academico.repositorio.PlanEstudioRepository;
import co.edu.elencano.plataforma.academico.servicio.GrupoService;
import co.edu.elencano.plataforma.asistencia.entidad.DetalleAsistencia;
import co.edu.elencano.plataforma.asistencia.entidad.EstadoAsistencia;
import co.edu.elencano.plataforma.asistencia.entidad.RegistroAsistencia;
import co.edu.elencano.plataforma.asistencia.repositorio.ConteoInasistencia;
import co.edu.elencano.plataforma.asistencia.repositorio.DetalleAsistenciaRepository;
import co.edu.elencano.plataforma.asistencia.repositorio.RegistroAsistenciaRepository;
import co.edu.elencano.plataforma.asistencia.web.dto.AsignaturaResumenDto;
import co.edu.elencano.plataforma.asistencia.web.dto.AsistenciaClaseDto;
import co.edu.elencano.plataforma.asistencia.web.dto.CargaDocenteDto;
import co.edu.elencano.plataforma.asistencia.web.dto.EstudianteAsistenciaDto;
import co.edu.elencano.plataforma.asistencia.web.dto.EstudianteResumenDto;
import co.edu.elencano.plataforma.asistencia.web.dto.GrupoAsistenciaDto;
import co.edu.elencano.plataforma.asistencia.web.dto.GuardarAsistenciaDto;
import co.edu.elencano.plataforma.asistencia.web.dto.InasistenciaAsignaturaDto;
import co.edu.elencano.plataforma.asistencia.web.dto.ItemAsistenciaDto;
import co.edu.elencano.plataforma.asistencia.web.dto.NovedadAsistenciaDto;
import co.edu.elencano.plataforma.asistencia.web.dto.PendientesAsistenciaDto;
import co.edu.elencano.plataforma.asistencia.web.dto.PendientesAsistenciaDto.ClaseHoy;
import co.edu.elencano.plataforma.asistencia.web.dto.PendientesAsistenciaDto.EstudianteEnRiesgo;
import co.edu.elencano.plataforma.asistencia.web.dto.PendientesAsistenciaDto.FaltaPorJustificar;
import co.edu.elencano.plataforma.asistencia.web.dto.ResumenGrupoDto;
import co.edu.elencano.plataforma.comun.excepcion.RecursoNoEncontradoException;
import co.edu.elencano.plataforma.comun.excepcion.ReglaNegocioException;
import co.edu.elencano.plataforma.matricula.entidad.Matricula;
import co.edu.elencano.plataforma.matricula.repositorio.MatriculaRepository;
import co.edu.elencano.plataforma.usuarios.entidad.Persona;
import co.edu.elencano.plataforma.usuarios.entidad.Rol;
import co.edu.elencano.plataforma.usuarios.repositorio.UsuarioRepository;
import co.edu.elencano.plataforma.usuarios.seguridad.UsuarioAutenticado;

/**
 * Toma de asistencia por clase, consolidado por grupo y justificacion de faltas segun el SIEE:
 * se pierde una asignatura por inasistencia injustificada mayor al porcentaje maximo de sus horas del anio,
 * y la falta se justifica dentro de los dias habiles siguientes.
 *
 * Quien puede hacer que:
 * - Tomar asistencia: el docente de la carga, el administrador o el coordinador academico.
 * - Ver el consolidado y las faltas de un grupo: directivos, secretaria y el director del grupo.
 * - Justificar: administrador, coordinador, secretaria y director del grupo. Pasado el plazo solo
 *   administrador y coordinador, para casos excepcionales.
 */
@Service
public class AsistenciaService {

    private static final Set<Rol> TOMAN_CUALQUIER_CLASE = EnumSet.of(Rol.ADMINISTRADOR, Rol.COORDINADOR_ACADEMICO);
    private static final Set<Rol> VEN_TODOS_LOS_GRUPOS =
            EnumSet.of(Rol.ADMINISTRADOR, Rol.RECTOR, Rol.COORDINADOR_ACADEMICO, Rol.SECRETARIA);
    private static final Set<Rol> JUSTIFICAN = EnumSet.of(Rol.ADMINISTRADOR, Rol.COORDINADOR_ACADEMICO, Rol.SECRETARIA);

    private final RegistroAsistenciaRepository registroRepository;
    private final DetalleAsistenciaRepository detalleRepository;
    private final CargaAcademicaRepository cargaRepository;
    private final GrupoRepository grupoRepository;
    private final GrupoService grupoService;
    private final PlanEstudioRepository planRepository;
    private final MatriculaRepository matriculaRepository;
    private final UsuarioRepository usuarioRepository;
    private final BigDecimal porcentajeMaximo;
    private final int diasParaJustificar;
    private final int semanasLectivas;

    public AsistenciaService(RegistroAsistenciaRepository registroRepository,
                             DetalleAsistenciaRepository detalleRepository,
                             CargaAcademicaRepository cargaRepository, GrupoRepository grupoRepository,
                             GrupoService grupoService, PlanEstudioRepository planRepository,
                             MatriculaRepository matriculaRepository, UsuarioRepository usuarioRepository,
                             @Value("${plataforma.asistencia.porcentaje-maximo-inasistencia:15}") BigDecimal porcentajeMaximo,
                             @Value("${plataforma.asistencia.dias-habiles-justificacion:3}") int diasParaJustificar,
                             @Value("${plataforma.asistencia.semanas-lectivas:40}") int semanasLectivas) {
        this.registroRepository = registroRepository;
        this.detalleRepository = detalleRepository;
        this.cargaRepository = cargaRepository;
        this.grupoRepository = grupoRepository;
        this.grupoService = grupoService;
        this.planRepository = planRepository;
        this.matriculaRepository = matriculaRepository;
        this.usuarioRepository = usuarioRepository;
        this.porcentajeMaximo = porcentajeMaximo;
        this.diasParaJustificar = diasParaJustificar;
        this.semanasLectivas = semanasLectivas;
    }

    /** Cargas en que el usuario puede tomar asistencia: las propias o todas si es administrador o coordinador. */
    @Transactional(readOnly = true)
    public List<CargaDocenteDto> listarCargas(UsuarioAutenticado usuario) {
        List<CargaAcademica> cargas = tieneAlguno(usuario, TOMAN_CUALQUIER_CLASE)
                ? cargaRepository.listarAbiertas()
                : cargaRepository.listarAbiertasDeDocente(usuario.getPersonaId());
        return cargas.stream().map(CargaDocenteDto::de).toList();
    }

    @Transactional(readOnly = true)
    public AsistenciaClaseDto obtenerClase(Long cargaId, LocalDate fecha, UsuarioAutenticado usuario) {
        CargaAcademica carga = cargaQueToma(cargaId, usuario);
        Periodo periodo = periodoDe(carga.getGrupo().getAnioLectivo(), fecha);
        RegistroAsistencia registro = registroRepository.findByCargaIdAndFecha(cargaId, fecha).orElse(null);
        List<EstudianteAsistenciaDto> estudiantes = matriculaRepository.listarActivasDeGrupo(carga.getGrupo().getId())
                .stream()
                .map(m -> {
                    Persona p = m.getEstudiante().getPersona();
                    DetalleAsistencia d = registro == null ? null : registro.detalleDe(m.getId()).orElse(null);
                    return d == null
                            ? new EstudianteAsistenciaDto(m.getId(), p.getNombres(), p.getApellidos(),
                                    EstadoAsistencia.ASISTIO, null, null)
                            : new EstudianteAsistenciaDto(m.getId(), p.getNombres(), p.getApellidos(),
                                    d.getEstado(), d.getObservacion(), d.getJustificacion());
                })
                .toList();
        return new AsistenciaClaseDto(cargaId, nombreGrupo(carga.getGrupo()), carga.getAsignatura().getNombre(),
                fecha, periodo.getNumero(), registro == null ? 1 : registro.getHoras(), registro != null, estudiantes);
    }

    /** Guarda o corrige la asistencia de una clase. Los estudiantes que no vengan en la lista no cambian. */
    @Transactional
    public AsistenciaClaseDto guardarClase(Long cargaId, LocalDate fecha, GuardarAsistenciaDto datos,
                                           UsuarioAutenticado usuario) {
        CargaAcademica carga = cargaQueToma(cargaId, usuario);
        AnioLectivo anio = carga.getGrupo().getAnioLectivo();
        if (anio.estaCerrado()) {
            throw new ReglaNegocioException("El año lectivo " + anio.getAnio() + " está cerrado y no admite cambios");
        }
        if (fecha.isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("No se puede tomar asistencia de una fecha futura");
        }
        Periodo periodo = periodoDe(anio, fecha);
        if (periodo.isCerrado()) {
            throw new ReglaNegocioException("El periodo " + periodo.getNumero() + " está cerrado");
        }
        Map<Long, Matricula> delGrupo = matriculaRepository.listarActivasDeGrupo(carga.getGrupo().getId()).stream()
                .collect(Collectors.toMap(Matricula::getId, Function.identity()));

        RegistroAsistencia registro = registroRepository.findByCargaIdAndFecha(cargaId, fecha)
                .orElseGet(() -> new RegistroAsistencia(carga, periodo, fecha));
        registro.registrar(datos.horas(), usuarioRepository.getReferenceById(usuario.getId()), LocalDateTime.now());
        for (ItemAsistenciaDto item : datos.estudiantes()) {
            Matricula matricula = delGrupo.get(item.matriculaId());
            if (matricula == null) {
                throw new ReglaNegocioException("La matrícula " + item.matriculaId() + " no está activa en el grupo");
            }
            DetalleAsistencia detalle = registro.detalleDe(matricula.getId())
                    .orElseGet(() -> registro.agregarDetalle(matricula));
            if (item.estado() == EstadoAsistencia.FALTA_JUSTIFICADA
                    && detalle.getEstado() != EstadoAsistencia.FALTA_JUSTIFICADA) {
                throw new ReglaNegocioException("La falta de " + matricula.getEstudiante().getPersona().getNombreCompleto()
                        + " se marca como FALTA; la justificación la registra el director de grupo o secretaria");
            }
            detalle.marcar(item.estado(), vacioANulo(item.observacion()));
        }
        registroRepository.save(registro);
        return obtenerClase(cargaId, fecha, usuario);
    }

    /** Grupos cuyo consolidado puede ver el usuario: todos para directivos y secretaria, los que dirige para un docente. */
    @Transactional(readOnly = true)
    public List<GrupoAsistenciaDto> listarGrupos(UsuarioAutenticado usuario) {
        Long directorId = tieneAlguno(usuario, VEN_TODOS_LOS_GRUPOS) ? null : usuario.getPersonaId();
        return grupoRepository.listarAbiertos(directorId).stream().map(GrupoAsistenciaDto::de).toList();
    }

    /** Horas de inasistencia del anio por estudiante y asignatura, con alerta si supera el maximo del SIEE. */
    @Transactional(readOnly = true)
    public ResumenGrupoDto resumenGrupo(Long grupoId, UsuarioAutenticado usuario) {
        Grupo grupo = grupoService.obtener(grupoId);
        verificarAccesoGrupo(grupo, usuario, VEN_TODOS_LOS_GRUPOS);
        List<PlanEstudio> plan = planRepository.listar(grupo.getAnioLectivo().getId(), grupo.getGrado().getId());
        Map<Long, Integer> horasAnuales = new HashMap<>();
        List<AsignaturaResumenDto> asignaturas = new ArrayList<>();
        for (PlanEstudio p : plan) {
            int horas = p.getIntensidadHoraria() * semanasLectivas;
            horasAnuales.put(p.getAsignatura().getId(), horas);
            asignaturas.add(new AsignaturaResumenDto(p.getAsignatura().getId(), p.getAsignatura().getNombre(), horas));
        }

        Map<Long, Map<Long, long[]>> conteos = new HashMap<>();
        for (ConteoInasistencia c : detalleRepository.contarDeGrupo(grupoId)) {
            long[] horas = conteos.computeIfAbsent(c.matriculaId(), k -> new HashMap<>())
                    .computeIfAbsent(c.asignaturaId(), k -> new long[3]);
            int posicion = switch (c.estado()) {
                case FALTA -> 0;
                case FALTA_JUSTIFICADA, PERMISO -> 1;
                default -> 2;
            };
            horas[posicion] += c.horas();
        }

        List<EstudianteResumenDto> estudiantes = matriculaRepository.listarActivasDeGrupo(grupoId).stream()
                .map(m -> {
                    Persona p = m.getEstudiante().getPersona();
                    List<InasistenciaAsignaturaDto> detalle = new ArrayList<>();
                    for (AsignaturaResumenDto a : asignaturas) {
                        long[] horas = conteos.getOrDefault(m.getId(), Map.of()).get(a.asignaturaId());
                        if (horas != null) {
                            BigDecimal porcentaje = porcentaje(horas[0], horasAnuales.get(a.asignaturaId()));
                            detalle.add(new InasistenciaAsignaturaDto(a.asignaturaId(), horas[0], horas[1], horas[2],
                                    porcentaje, porcentaje.compareTo(porcentajeMaximo) > 0));
                        }
                    }
                    boolean supera = detalle.stream().anyMatch(InasistenciaAsignaturaDto::superaLimite);
                    return new EstudianteResumenDto(m.getId(), p.getNombres(), p.getApellidos(), detalle, supera);
                })
                .toList();
        return new ResumenGrupoDto(grupoId, nombreGrupo(grupo), semanasLectivas, porcentajeMaximo, asignaturas,
                estudiantes);
    }

    /**
     * Pendientes para la pantalla de inicio:
     * - Clases propias del docente en periodo vigente, con la marca de si ya tienen asistencia de hoy.
     * - Faltas sin justificar que siguen en plazo, para quien puede justificarlas.
     * - Estudiantes que superan el maximo de inasistencia o ya pasaron dos tercios de el.
     * No hay horario, por eso se listan todas las clases del docente y no solo las del dia.
     */
    @Transactional(readOnly = true)
    public PendientesAsistenciaDto pendientes(UsuarioAutenticado usuario) {
        LocalDate hoy = LocalDate.now();
        return new PendientesAsistenciaDto(porcentajeMaximo, clasesHoy(usuario, hoy),
                faltasPorJustificar(usuario, hoy), estudiantesEnRiesgo(usuario));
    }

    private List<ClaseHoy> clasesHoy(UsuarioAutenticado usuario, LocalDate hoy) {
        boolean finDeSemana = hoy.getDayOfWeek() == DayOfWeek.SATURDAY || hoy.getDayOfWeek() == DayOfWeek.SUNDAY;
        if (finDeSemana || usuario.getPersonaId() == null || !usuario.tieneRol(Rol.DOCENTE)) {
            return List.of();
        }
        List<CargaAcademica> cargas = cargaRepository.listarAbiertasDeDocente(usuario.getPersonaId()).stream()
                .filter(c -> c.getGrupo().getAnioLectivo().getPeriodos().stream()
                        .anyMatch(p -> !p.isCerrado() && !hoy.isBefore(p.getFechaInicio())
                                && !hoy.isAfter(p.getFechaFin())))
                .toList();
        if (cargas.isEmpty()) {
            return List.of();
        }
        Set<Long> registradas = Set.copyOf(registroRepository.listarCargasRegistradas(
                cargas.stream().map(CargaAcademica::getId).toList(), hoy));
        return cargas.stream()
                .map(c -> new ClaseHoy(c.getId(), c.getGrupo().getSede().getNombre(), nombreGrupo(c.getGrupo()),
                        c.getAsignatura().getNombre(), registradas.contains(c.getId())))
                .toList();
    }

    private List<FaltaPorJustificar> faltasPorJustificar(UsuarioAutenticado usuario, LocalDate hoy) {
        Long directorId = tieneAlguno(usuario, JUSTIFICAN) ? null : usuario.getPersonaId();
        if (directorId == null && !tieneAlguno(usuario, JUSTIFICAN)) {
            return List.of();
        }
        // Primer dia cuyas faltas todavia se pueden justificar hoy
        LocalDate desde = hoy;
        while (!fechaLimite(desde.minusDays(1)).isBefore(hoy)) {
            desde = desde.minusDays(1);
        }
        Map<String, FaltaPorJustificar> porDia = new LinkedHashMap<>();
        for (DetalleAsistencia d : detalleRepository.listarFaltasRecientes(desde, directorId)) {
            Matricula m = d.getMatricula();
            LocalDate fecha = d.getRegistro().getFecha();
            porDia.merge(m.getId() + "|" + fecha,
                    new FaltaPorJustificar(m.getId(), m.getGrupo().getId(),
                            m.getEstudiante().getPersona().getNombreCompleto(), nombreGrupo(m.getGrupo()), fecha,
                            d.getRegistro().getHoras(), fechaLimite(fecha)),
                    (a, b) -> new FaltaPorJustificar(a.matriculaId(), a.grupoId(), a.estudiante(), a.grupo(),
                            a.fecha(), a.horas() + b.horas(), a.fechaLimite()));
        }
        return List.copyOf(porDia.values());
    }

    private List<EstudianteEnRiesgo> estudiantesEnRiesgo(UsuarioAutenticado usuario) {
        BigDecimal umbral = porcentajeMaximo.multiply(BigDecimal.valueOf(2))
                .divide(BigDecimal.valueOf(3), 1, RoundingMode.HALF_UP);
        Long directorId = tieneAlguno(usuario, VEN_TODOS_LOS_GRUPOS) ? null : usuario.getPersonaId();
        if (directorId == null && !tieneAlguno(usuario, VEN_TODOS_LOS_GRUPOS)) {
            return List.of();
        }
        List<EstudianteEnRiesgo> enRiesgo = new ArrayList<>();
        for (Grupo grupo : grupoRepository.listarAbiertos(directorId)) {
            ResumenGrupoDto resumen = resumenGrupo(grupo.getId(), usuario);
            Map<Long, String> nombres = resumen.asignaturas().stream()
                    .collect(Collectors.toMap(AsignaturaResumenDto::asignaturaId, AsignaturaResumenDto::nombre));
            for (EstudianteResumenDto e : resumen.estudiantes()) {
                e.asignaturas().stream()
                        .max(Comparator.comparing(InasistenciaAsignaturaDto::porcentaje))
                        .filter(a -> a.porcentaje().compareTo(umbral) >= 0)
                        .ifPresent(a -> enRiesgo.add(new EstudianteEnRiesgo(e.matriculaId(), grupo.getId(),
                                e.nombres() + " " + e.apellidos(), resumen.grupo(), nombres.get(a.asignaturaId()),
                                a.porcentaje(), a.superaLimite())));
            }
        }
        enRiesgo.sort(Comparator.comparing(EstudianteEnRiesgo::porcentaje).reversed());
        return enRiesgo;
    }

    @Transactional(readOnly = true)
    public List<NovedadAsistenciaDto> novedades(Long matriculaId, UsuarioAutenticado usuario) {
        Matricula matricula = obtenerMatricula(matriculaId);
        verificarAccesoMatricula(matricula, usuario, VEN_TODOS_LOS_GRUPOS);
        return detalleRepository.listarNovedadesDeMatricula(matriculaId).stream()
                .map(d -> new NovedadAsistenciaDto(d.getId(), d.getRegistro().getFecha(),
                        d.getRegistro().getCarga().getAsignatura().getNombre(), d.getRegistro().getHoras(),
                        d.getEstado(), d.getObservacion(), d.getJustificacion(),
                        fechaLimite(d.getRegistro().getFecha())))
                .toList();
    }

    /** Justifica todas las faltas del estudiante en esa fecha (todas las clases del dia). */
    @Transactional
    public List<NovedadAsistenciaDto> justificar(Long matriculaId, LocalDate fecha, String justificacion,
                                                 UsuarioAutenticado usuario) {
        Matricula matricula = obtenerMatricula(matriculaId);
        verificarAccesoMatricula(matricula, usuario, JUSTIFICAN);
        if (matricula.getAnioLectivo().estaCerrado()) {
            throw new ReglaNegocioException("El año lectivo está cerrado y no admite cambios");
        }
        List<DetalleAsistencia> faltas = detalleRepository.listarFaltasSinJustificar(matriculaId, fecha);
        if (faltas.isEmpty()) {
            throw new ReglaNegocioException("El estudiante no tiene faltas sin justificar el " + fecha);
        }
        LocalDate limite = fechaLimite(fecha);
        if (LocalDate.now().isAfter(limite) && !tieneAlguno(usuario, TOMAN_CUALQUIER_CLASE)) {
            throw new ReglaNegocioException("El plazo para justificar venció el " + limite
                    + ". Solo coordinación puede justificarla");
        }
        if (faltas.stream().anyMatch(d -> d.getRegistro().getPeriodo().isCerrado())) {
            throw new ReglaNegocioException("El periodo de esa fecha está cerrado");
        }
        LocalDateTime ahora = LocalDateTime.now();
        faltas.forEach(d -> d.justificar(justificacion.trim(), usuarioRepository.getReferenceById(usuario.getId()), ahora));
        return novedades(matriculaId, usuario);
    }

    /** Ultimo dia para justificar: la fecha mas los dias habiles del SIEE, sin contar sabados ni domingos. */
    LocalDate fechaLimite(LocalDate fecha) {
        LocalDate limite = fecha;
        int dias = 0;
        while (dias < diasParaJustificar) {
            limite = limite.plusDays(1);
            if (limite.getDayOfWeek() != DayOfWeek.SATURDAY && limite.getDayOfWeek() != DayOfWeek.SUNDAY) {
                dias++;
            }
        }
        return limite;
    }

    private CargaAcademica cargaQueToma(Long cargaId, UsuarioAutenticado usuario) {
        CargaAcademica carga = cargaRepository.findById(cargaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la carga académica " + cargaId));
        if (!tieneAlguno(usuario, TOMAN_CUALQUIER_CLASE)
                && !carga.getDocente().getId().equals(usuario.getPersonaId())) {
            throw new AccessDeniedException("La carga no es del docente");
        }
        return carga;
    }

    private Periodo periodoDe(AnioLectivo anio, LocalDate fecha) {
        return anio.getPeriodos().stream()
                .filter(p -> !fecha.isBefore(p.getFechaInicio()) && !fecha.isAfter(p.getFechaFin()))
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException("La fecha " + fecha
                        + " no está dentro de ningún periodo del año " + anio.getAnio()));
    }

    private Matricula obtenerMatricula(Long id) {
        return matriculaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la matrícula " + id));
    }

    private void verificarAccesoMatricula(Matricula matricula, UsuarioAutenticado usuario, Set<Rol> roles) {
        if (matricula.getGrupo() == null) {
            if (!tieneAlguno(usuario, roles)) {
                throw new AccessDeniedException("Sin acceso a la matrícula");
            }
            return;
        }
        verificarAccesoGrupo(matricula.getGrupo(), usuario, roles);
    }

    private void verificarAccesoGrupo(Grupo grupo, UsuarioAutenticado usuario, Set<Rol> roles) {
        boolean esDirector = grupo.getDirector() != null && grupo.getDirector().getId().equals(usuario.getPersonaId());
        if (!esDirector && !tieneAlguno(usuario, roles)) {
            throw new AccessDeniedException("Sin acceso al grupo");
        }
    }

    private static boolean tieneAlguno(UsuarioAutenticado usuario, Set<Rol> roles) {
        return usuario.getRoles().stream().anyMatch(roles::contains);
    }

    private BigDecimal porcentaje(long horas, Integer horasAnuales) {
        if (horasAnuales == null || horasAnuales == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(horas * 100L).divide(BigDecimal.valueOf(horasAnuales), 1, RoundingMode.HALF_UP);
    }

    private static String nombreGrupo(Grupo grupo) {
        return grupo.getGrado().getNombre() + " " + grupo.getNombre();
    }

    private static String vacioANulo(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }
}
