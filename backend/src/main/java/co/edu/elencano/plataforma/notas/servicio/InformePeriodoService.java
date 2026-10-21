package co.edu.elencano.plataforma.notas.servicio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import co.edu.elencano.plataforma.academico.entidad.CargaAcademica;
import co.edu.elencano.plataforma.academico.entidad.Periodo;
import co.edu.elencano.plataforma.academico.repositorio.CargaAcademicaRepository;
import co.edu.elencano.plataforma.comun.excepcion.RecursoNoEncontradoException;
import co.edu.elencano.plataforma.comun.excepcion.ReglaNegocioException;
import co.edu.elencano.plataforma.matricula.entidad.Matricula;
import co.edu.elencano.plataforma.matricula.repositorio.MatriculaRepository;
import co.edu.elencano.plataforma.notas.entidad.Desempeno;
import co.edu.elencano.plataforma.notas.entidad.DescriptorDesempeno;
import co.edu.elencano.plataforma.notas.entidad.InformeEstudiante;
import co.edu.elencano.plataforma.notas.repositorio.DescriptorDesempenoRepository;
import co.edu.elencano.plataforma.notas.repositorio.InformeEstudianteRepository;
import co.edu.elencano.plataforma.notas.web.dto.ConfiguracionEvaluacionDto;
import co.edu.elencano.plataforma.notas.web.dto.GuardarInformeDto;
import co.edu.elencano.plataforma.notas.web.dto.InformePeriodoDto;
import co.edu.elencano.plataforma.notas.web.dto.PlanillaNotasDto;
import co.edu.elencano.plataforma.usuarios.repositorio.UsuarioRepository;
import co.edu.elencano.plataforma.usuarios.seguridad.UsuarioAutenticado;

/**
 * Informe del periodo de una clase para el boletin (SIEE art. 14): concepto descriptivo de cada desempeno,
 * comportamiento y observacion por estudiante. Quien puede ver y registrar es el mismo de la planilla, y se
 * cambia mientras el periodo este abierto.
 */
@Service
public class InformePeriodoService {

    private final NotasService notasService;
    private final CargaAcademicaRepository cargaRepository;
    private final MatriculaRepository matriculaRepository;
    private final DescriptorDesempenoRepository descriptorRepository;
    private final InformeEstudianteRepository informeRepository;
    private final UsuarioRepository usuarioRepository;

    public InformePeriodoService(NotasService notasService, CargaAcademicaRepository cargaRepository,
                                 MatriculaRepository matriculaRepository,
                                 DescriptorDesempenoRepository descriptorRepository,
                                 InformeEstudianteRepository informeRepository, UsuarioRepository usuarioRepository) {
        this.notasService = notasService;
        this.cargaRepository = cargaRepository;
        this.matriculaRepository = matriculaRepository;
        this.descriptorRepository = descriptorRepository;
        this.informeRepository = informeRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Largo maximo de la observacion en grados con nota; en Transicion el informe descriptivo admite 1500. */
    static final int MAXIMO_OBSERVACION = 500;

    @Transactional
    public InformePeriodoDto informe(Long cargaId, Long periodoId, UsuarioAutenticado usuario) {
        CargaAcademica carga = notasService.cargaDelDocente(cargaId, usuario);
        if (carga.getGrupo().getGrado().isEvaluacionCualitativa()) {
            return informeDescriptivo(carga, periodoDe(carga, periodoId));
        }
        // La planilla verifica el permiso sobre la clase y trae la nota definitiva de cada estudiante
        PlanillaNotasDto planilla = notasService.planilla(cargaId, periodoId, usuario);
        Map<Desempeno, String> descriptores = new EnumMap<>(Desempeno.class);
        descriptorRepository.findByCargaIdAndPeriodoId(cargaId, periodoId)
                .forEach(d -> descriptores.put(d.getDesempeno(), d.getDescripcion()));
        Map<Long, InformeEstudiante> informes = informesPorMatricula(cargaId, periodoId);
        return new InformePeriodoDto(cargaId, planilla.grupo(), planilla.asignatura(), periodoId, planilla.periodo(),
                planilla.editable(), planilla.configuracion(), descriptores,
                planilla.estudiantes().stream().map(f -> {
                    InformeEstudiante i = informes.get(f.matriculaId());
                    return new InformePeriodoDto.Fila(f.matriculaId(), f.nombres(), f.apellidos(), f.notaDefinitiva(),
                            f.desempeno(), f.completa(), i == null ? null : i.getComportamiento(),
                            i == null ? null : i.getObservacion());
                }).toList(), false);
    }

    /** Transicion: por cada estudiante, el informe descriptivo de la dimension en el periodo. */
    private InformePeriodoDto informeDescriptivo(CargaAcademica carga, Periodo periodo) {
        Map<Long, InformeEstudiante> informes = informesPorMatricula(carga.getId(), periodo.getId());
        return new InformePeriodoDto(carga.getId(), carga.getGrupo().getGrado().getNombre() + " "
                + carga.getGrupo().getNombre(), carga.getAsignatura().getNombre(), periodo.getId(), periodo.getNumero(),
                editable(periodo), notasService.obtenerConfiguracion(periodo.getAnioLectivo().getId()), Map.of(),
                matriculaRepository.listarActivasDeGrupo(carga.getGrupo().getId()).stream().map(m -> {
                    InformeEstudiante i = informes.get(m.getId());
                    return new InformePeriodoDto.Fila(m.getId(), m.getEstudiante().getPersona().getNombres(),
                            m.getEstudiante().getPersona().getApellidos(), null, null, false, null,
                            i == null ? null : i.getObservacion());
                }).toList(), true);
    }

    private InformePeriodoDto guardarDescriptivo(CargaAcademica carga, Periodo periodo, GuardarInformeDto datos,
                                                UsuarioAutenticado usuario) {
        if (!editable(periodo)) {
            throw new ReglaNegocioException("El periodo " + periodo.getNumero()
                    + " está cerrado; el informe ya no se puede cambiar");
        }
        Map<Long, Matricula> delGrupo = matriculaRepository.listarActivasDeGrupo(carga.getGrupo().getId()).stream()
                .collect(Collectors.toMap(Matricula::getId, Function.identity()));
        Map<Long, InformeEstudiante> informes = informesPorMatricula(carga.getId(), periodo.getId());
        LocalDateTime ahora = LocalDateTime.now();
        for (GuardarInformeDto.Estudiante item : datos.estudiantes() == null ? List.<GuardarInformeDto.Estudiante>of()
                : datos.estudiantes()) {
            Matricula matricula = delGrupo.get(item.matriculaId());
            if (matricula == null) {
                throw new ReglaNegocioException("La matrícula " + item.matriculaId() + " no está activa en el grupo");
            }
            if (item.comportamiento() != null) {
                throw new ReglaNegocioException(carga.getGrupo().getGrado().getNombre()
                        + " se evalúa de forma cualitativa: el informe no lleva valoración numérica");
            }
            String texto = StringUtils.hasText(item.observacion()) ? item.observacion().trim() : null;
            InformeEstudiante actual = informes.get(item.matriculaId());
            if (texto == null) {
                if (actual != null) {
                    informeRepository.delete(actual);
                }
                continue;
            }
            InformeEstudiante informe = actual != null ? actual : new InformeEstudiante(carga, periodo, matricula);
            informe.registrar(null, texto, usuarioRepository.getReferenceById(usuario.getId()), ahora);
            informeRepository.save(informe);
        }
        informeRepository.flush();
        return informeDescriptivo(carga, periodo);
    }

    /** Observaciones o informes descriptivos de una matricula, por "cargaId-periodoId". Sin verificar acceso. */
    @Transactional(readOnly = true)
    public Map<String, String> observacionesDeMatricula(Long matriculaId) {
        return informeRepository.findByMatriculaId(matriculaId).stream()
                .filter(i -> i.getObservacion() != null)
                .collect(Collectors.toMap(i -> i.getCargaId() + "-" + i.getPeriodoId(), InformeEstudiante::getObservacion));
    }

    private static Periodo periodoDe(CargaAcademica carga, Long periodoId) {
        return carga.getGrupo().getAnioLectivo().getPeriodos().stream()
                .filter(p -> p.getId().equals(periodoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("El periodo " + periodoId + " no es del año de la clase"));
    }

    private static boolean editable(Periodo periodo) {
        return !periodo.isCerrado() && !periodo.getAnioLectivo().estaCerrado();
    }

    @Transactional
    public InformePeriodoDto guardar(Long cargaId, Long periodoId, GuardarInformeDto datos, UsuarioAutenticado usuario) {
        CargaAcademica cargaDocente = notasService.cargaDelDocente(cargaId, usuario);
        if (cargaDocente.getGrupo().getGrado().isEvaluacionCualitativa()) {
            return guardarDescriptivo(cargaDocente, periodoDe(cargaDocente, periodoId), datos, usuario);
        }
        PlanillaNotasDto planilla = notasService.planilla(cargaId, periodoId, usuario);
        if (!planilla.editable()) {
            throw new ReglaNegocioException("El periodo " + planilla.periodo()
                    + " está cerrado; el informe ya no se puede cambiar");
        }
        CargaAcademica carga = cargaRepository.findById(cargaId).orElseThrow();
        Periodo periodo = carga.getGrupo().getAnioLectivo().getPeriodos().stream()
                .filter(p -> p.getId().equals(periodoId))
                .findFirst()
                .orElseThrow();

        if (datos.descriptores() != null) {
            Map<Desempeno, DescriptorDesempeno> existentes = descriptorRepository.findByCargaIdAndPeriodoId(cargaId, periodoId)
                    .stream().collect(Collectors.toMap(DescriptorDesempeno::getDesempeno, Function.identity()));
            datos.descriptores().forEach((desempeno, texto) -> {
                DescriptorDesempeno actual = existentes.get(desempeno);
                if (!StringUtils.hasText(texto)) {
                    if (actual != null) {
                        descriptorRepository.delete(actual);
                    }
                    return;
                }
                DescriptorDesempeno d = actual != null ? actual : new DescriptorDesempeno(carga, periodo, desempeno);
                d.setDescripcion(texto.trim());
                descriptorRepository.save(d);
            });
        }

        if (datos.estudiantes() != null) {
            ConfiguracionEvaluacionDto config = planilla.configuracion();
            Map<Long, PlanillaNotasDto.Fila> delGrupo = planilla.estudiantes().stream()
                    .collect(Collectors.toMap(PlanillaNotasDto.Fila::matriculaId, Function.identity()));
            Map<Long, InformeEstudiante> informes = informesPorMatricula(cargaId, periodoId);
            LocalDateTime ahora = LocalDateTime.now();
            for (GuardarInformeDto.Estudiante item : datos.estudiantes()) {
                PlanillaNotasDto.Fila fila = delGrupo.get(item.matriculaId());
                if (fila == null) {
                    throw new ReglaNegocioException("La matrícula " + item.matriculaId() + " no está activa en el grupo");
                }
                BigDecimal comportamiento = item.comportamiento();
                if (comportamiento != null && (comportamiento.scale() > 1
                        || comportamiento.compareTo(config.notaMinima()) < 0
                        || comportamiento.compareTo(config.notaMaxima()) > 0)) {
                    throw new ReglaNegocioException("El comportamiento de " + fila.nombres() + " " + fila.apellidos()
                            + " debe estar entre " + config.notaMinima() + " y " + config.notaMaxima()
                            + " con una sola decimal");
                }
                String observacion = StringUtils.hasText(item.observacion()) ? item.observacion().trim() : null;
                if (observacion != null && observacion.length() > MAXIMO_OBSERVACION) {
                    throw new ReglaNegocioException("La observación de " + fila.nombres() + " " + fila.apellidos()
                            + " pasa de " + MAXIMO_OBSERVACION + " caracteres");
                }
                InformeEstudiante actual = informes.get(item.matriculaId());
                if (comportamiento == null && observacion == null) {
                    if (actual != null) {
                        informeRepository.delete(actual);
                    }
                    continue;
                }
                InformeEstudiante informe = actual != null ? actual
                        : new InformeEstudiante(carga, periodo, matriculaRepository.getReferenceById(item.matriculaId()));
                informe.registrar(comportamiento, observacion, usuarioRepository.getReferenceById(usuario.getId()), ahora);
                informeRepository.save(informe);
            }
        }
        descriptorRepository.flush();
        informeRepository.flush();
        return informe(cargaId, periodoId, usuario);
    }

    private Map<Long, InformeEstudiante> informesPorMatricula(Long cargaId, Long periodoId) {
        return informeRepository.findByCargaIdAndPeriodoId(cargaId, periodoId).stream()
                .collect(Collectors.toMap(InformeEstudiante::getMatriculaId, Function.identity()));
    }
}
