package co.edu.elencano.plataforma.notas.servicio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import co.edu.elencano.plataforma.academico.entidad.CargaAcademica;
import co.edu.elencano.plataforma.academico.entidad.Periodo;
import co.edu.elencano.plataforma.academico.repositorio.CargaAcademicaRepository;
import co.edu.elencano.plataforma.comun.excepcion.ReglaNegocioException;
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

    @Transactional
    public InformePeriodoDto informe(Long cargaId, Long periodoId, UsuarioAutenticado usuario) {
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
                }).toList());
    }

    @Transactional
    public InformePeriodoDto guardar(Long cargaId, Long periodoId, GuardarInformeDto datos, UsuarioAutenticado usuario) {
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
