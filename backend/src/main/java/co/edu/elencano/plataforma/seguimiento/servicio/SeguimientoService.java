package co.edu.elencano.plataforma.seguimiento.servicio;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.elencano.plataforma.academico.entidad.AnioLectivo;
import co.edu.elencano.plataforma.academico.entidad.CargaAcademica;
import co.edu.elencano.plataforma.academico.entidad.Grupo;
import co.edu.elencano.plataforma.academico.entidad.Periodo;
import co.edu.elencano.plataforma.academico.repositorio.CargaAcademicaRepository;
import co.edu.elencano.plataforma.asistencia.repositorio.RegistroAsistenciaRepository;
import co.edu.elencano.plataforma.asistencia.repositorio.ResumenRegistros;
import co.edu.elencano.plataforma.matricula.repositorio.MatriculaRepository;
import co.edu.elencano.plataforma.notas.entidad.ConfiguracionEvaluacion;
import co.edu.elencano.plataforma.notas.entidad.Dimension;
import co.edu.elencano.plataforma.notas.repositorio.ActividadEvaluativaRepository;
import co.edu.elencano.plataforma.notas.repositorio.ConfiguracionEvaluacionRepository;
import co.edu.elencano.plataforma.notas.repositorio.NotaActividadRepository;
import co.edu.elencano.plataforma.seguimiento.web.dto.AvanceRegistroDto;
import co.edu.elencano.plataforma.usuarios.entidad.Rol;
import co.edu.elencano.plataforma.usuarios.seguridad.UsuarioAutenticado;

/**
 * Avance del registro de notas y asistencia por clase.
 * - Directivos y secretaria ven todas las clases; el docente solo las suyas.
 * - Solo cuentan los anios abiertos que ya empezaron (algun periodo con inicio hasta hoy).
 * - Sin numero de periodo se toma el periodo en curso; entre periodos, el ultimo que empezo.
 */
@Service
public class SeguimientoService {

    private static final Set<Rol> VEN_TODAS_LAS_CLASES =
            EnumSet.of(Rol.ADMINISTRADOR, Rol.RECTOR, Rol.COORDINADOR_ACADEMICO, Rol.SECRETARIA);

    private final CargaAcademicaRepository cargaRepository;
    private final MatriculaRepository matriculaRepository;
    private final ActividadEvaluativaRepository actividadRepository;
    private final NotaActividadRepository notaRepository;
    private final ConfiguracionEvaluacionRepository configuracionRepository;
    private final RegistroAsistenciaRepository registroRepository;

    public SeguimientoService(CargaAcademicaRepository cargaRepository, MatriculaRepository matriculaRepository,
                              ActividadEvaluativaRepository actividadRepository, NotaActividadRepository notaRepository,
                              ConfiguracionEvaluacionRepository configuracionRepository,
                              RegistroAsistenciaRepository registroRepository) {
        this.cargaRepository = cargaRepository;
        this.matriculaRepository = matriculaRepository;
        this.actividadRepository = actividadRepository;
        this.notaRepository = notaRepository;
        this.configuracionRepository = configuracionRepository;
        this.registroRepository = registroRepository;
    }

    @Transactional(readOnly = true)
    public AvanceRegistroDto avance(Integer numeroPeriodo, UsuarioAutenticado usuario) {
        LocalDate hoy = LocalDate.now();
        List<CargaAcademica> cargas;
        if (VEN_TODAS_LAS_CLASES.stream().anyMatch(usuario::tieneRol)) {
            cargas = cargaRepository.listarAbiertas();
        } else if (usuario.tieneRol(Rol.DOCENTE) && usuario.getPersonaId() != null) {
            cargas = cargaRepository.listarAbiertasDeDocente(usuario.getPersonaId());
        } else {
            cargas = List.of();
        }

        // Periodo que se revisa en cada clase; las de anios que no han empezado no aparecen
        Map<Long, Periodo> periodoDeCarga = new HashMap<>();
        for (CargaAcademica c : cargas) {
            periodoQueSeRevisa(c.getGrupo().getAnioLectivo(), numeroPeriodo, hoy)
                    .ifPresent(p -> periodoDeCarga.put(c.getId(), p));
        }
        List<CargaAcademica> revisadas = cargas.stream().filter(c -> periodoDeCarga.containsKey(c.getId())).toList();
        if (revisadas.isEmpty()) {
            return new AvanceRegistroDto(hoy, List.of());
        }
        List<Long> cargaIds = revisadas.stream().map(CargaAcademica::getId).toList();

        Map<Long, Long> estudiantesPorGrupo = new HashMap<>();
        matriculaRepository.contarActivasDeGrupos(revisadas.stream().map(c -> c.getGrupo().getId()).distinct().toList())
                .forEach(m -> estudiantesPorGrupo.put(m.grupoId(), m.estudiantes()));
        Map<String, Long> actividades = new HashMap<>();
        actividadRepository.contarDeCargas(cargaIds).forEach(a ->
                actividades.put(clave(a.cargaId(), a.periodoId()) + "-" + a.dimension(), a.actividades()));
        Map<String, Long> notas = new HashMap<>();
        notaRepository.contarDeCargas(cargaIds).forEach(n -> notas.put(clave(n.cargaId(), n.periodoId()), n.notas()));
        Map<String, ResumenRegistros> asistencia = new HashMap<>();
        registroRepository.resumirDeCargas(cargaIds).forEach(r -> asistencia.put(clave(r.cargaId(), r.periodoId()), r));
        Map<Long, ConfiguracionEvaluacion> configuraciones = new HashMap<>();

        List<AvanceRegistroDto.Clase> clases = new ArrayList<>();
        for (CargaAcademica c : revisadas) {
            Periodo p = periodoDeCarga.get(c.getId());
            Grupo g = c.getGrupo();
            AnioLectivo anio = g.getAnioLectivo();
            String clave = clave(c.getId(), p.getId());
            int estudiantes = estudiantesPorGrupo.getOrDefault(g.getId(), 0L).intValue();
            boolean cualitativa = g.getGrado().isEvaluacionCualitativa();
            ConfiguracionEvaluacion config = configuraciones.computeIfAbsent(anio.getId(), id ->
                    configuracionRepository.findByAnioLectivoId(id).orElseGet(() -> ConfiguracionEvaluacion.porDefecto(anio)));

            int totalActividades = 0;
            List<Dimension> sinActividad = new ArrayList<>();
            for (Dimension d : Dimension.values()) {
                long n = actividades.getOrDefault(clave + "-" + d, 0L);
                totalActividades += (int) n;
                if (n == 0 && config.pesoDe(d) > 0 && !cualitativa) {
                    sinActividad.add(d);
                }
            }
            long registradas = notas.getOrDefault(clave, 0L);
            long esperadas = (long) totalActividades * estudiantes;
            int porcentaje = esperadas == 0 ? 0 : (int) Math.min(100, registradas * 100 / esperadas);
            if (porcentaje == 100 && !sinActividad.isEmpty()) {
                porcentaje = 99;
            }

            ResumenRegistros r = asistencia.get(clave);
            LocalDate hasta = hoy.isAfter(p.getFechaFin()) ? p.getFechaFin() : hoy;
            LocalDate desde = r != null ? r.ultimo() : p.getFechaInicio();
            long diasSin = Math.max(0, ChronoUnit.DAYS.between(desde, hasta));

            clases.add(new AvanceRegistroDto.Clase(c.getId(), anio.getAnio(), g.getSede().getNombre(),
                    g.getGrado().getNombre() + " " + g.getNombre(), c.getAsignatura().getNombre(),
                    c.getDocente() == null ? null : c.getDocente().getPersona().getNombreCompleto(),
                    p.getId(), p.getNumero(), p.getFechaInicio(), p.getFechaFin(), p.isCerrado(), estudiantes,
                    cualitativa, totalActividades, sinActividad, registradas, esperadas, porcentaje,
                    r == null ? 0 : r.dias(), r == null ? null : r.ultimo(), diasSin));
        }
        return new AvanceRegistroDto(hoy, clases);
    }

    private static Optional<Periodo> periodoQueSeRevisa(AnioLectivo anio, Integer numero, LocalDate hoy) {
        List<Periodo> empezados = anio.getPeriodos().stream()
                .filter(p -> !p.getFechaInicio().isAfter(hoy))
                .sorted(Comparator.comparingInt(Periodo::getNumero))
                .toList();
        if (empezados.isEmpty()) {
            return Optional.empty();
        }
        if (numero != null) {
            return anio.getPeriodos().stream().filter(p -> p.getNumero() == numero).findFirst();
        }
        return Optional.of(empezados.getLast());
    }

    private static String clave(Long cargaId, Long periodoId) {
        return cargaId + "-" + periodoId;
    }
}
