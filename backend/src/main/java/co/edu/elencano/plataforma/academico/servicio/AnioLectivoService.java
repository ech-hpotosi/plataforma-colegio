package co.edu.elencano.plataforma.academico.servicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.elencano.plataforma.academico.entidad.AnioLectivo;
import co.edu.elencano.plataforma.academico.entidad.EstadoAnio;
import co.edu.elencano.plataforma.academico.entidad.Periodo;
import co.edu.elencano.plataforma.academico.repositorio.AnioLectivoRepository;
import co.edu.elencano.plataforma.academico.web.dto.DatosPeriodoDto;
import co.edu.elencano.plataforma.academico.web.dto.GuardarAnioLectivoDto;
import co.edu.elencano.plataforma.comun.excepcion.RecursoNoEncontradoException;
import co.edu.elencano.plataforma.comun.excepcion.ReglaNegocioException;

/**
 * Anios lectivos y periodos. Reglas: los porcentajes suman 100, los periodos van en orden,
 * sin cruzarse y dentro del anio; el estado solo avanza y solo un anio puede estar en curso.
 */
@Service
public class AnioLectivoService {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final AnioLectivoRepository anioRepository;

    public AnioLectivoService(AnioLectivoRepository anioRepository) {
        this.anioRepository = anioRepository;
    }

    @Transactional(readOnly = true)
    public List<AnioLectivo> listar() {
        return anioRepository.findAllByOrderByAnioDesc();
    }

    @Transactional(readOnly = true)
    public AnioLectivo obtener(Long id) {
        return anioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el año lectivo " + id));
    }

    /** Obtiene el anio y verifica que no este cerrado, para operaciones que lo modifican. */
    @Transactional(readOnly = true)
    public AnioLectivo obtenerAbierto(Long id) {
        AnioLectivo anio = obtener(id);
        if (anio.estaCerrado()) {
            throw new ReglaNegocioException("El año lectivo " + anio.getAnio() + " está cerrado y no admite cambios");
        }
        return anio;
    }

    @Transactional
    public AnioLectivo crear(GuardarAnioLectivoDto datos) {
        if (anioRepository.existsByAnio(datos.anio())) {
            throw new ReglaNegocioException("El año lectivo " + datos.anio() + " ya existe");
        }
        validarFechas(datos);
        AnioLectivo anio = new AnioLectivo(datos.anio(), datos.fechaInicio(), datos.fechaFin());
        anio.reemplazarPeriodos(crearPeriodos(datos.periodos()));
        return anioRepository.save(anio);
    }

    @Transactional
    public AnioLectivo actualizar(Long id, GuardarAnioLectivoDto datos) {
        AnioLectivo anio = obtenerAbierto(id);
        if (anio.getPeriodos().stream().anyMatch(Periodo::isCerrado)) {
            throw new ReglaNegocioException("No se pueden cambiar las fechas porque ya hay periodos cerrados");
        }
        validarFechas(datos);
        anio.setFechaInicio(datos.fechaInicio());
        anio.setFechaFin(datos.fechaFin());
        anio.reemplazarPeriodos(crearPeriodos(datos.periodos()));
        return anio;
    }

    @Transactional
    public AnioLectivo cambiarEstado(Long id, EstadoAnio nuevo) {
        AnioLectivo anio = obtener(id);
        if (nuevo.ordinal() <= anio.getEstado().ordinal()) {
            throw new ReglaNegocioException("El estado solo puede avanzar: el año está en " + anio.getEstado());
        }
        if (nuevo == EstadoAnio.EN_CURSO && anioRepository.existsByEstadoAndIdNot(EstadoAnio.EN_CURSO, id)) {
            throw new ReglaNegocioException("Ya hay otro año lectivo en curso. Ciérrelo primero");
        }
        anio.setEstado(nuevo);
        return anio;
    }

    private static void validarFechas(GuardarAnioLectivoDto datos) {
        if (!datos.fechaFin().isAfter(datos.fechaInicio())) {
            throw new ReglaNegocioException("La fecha de fin del año debe ser posterior a la de inicio");
        }
        BigDecimal suma = BigDecimal.ZERO;
        LocalDate finAnterior = null;
        for (int i = 0; i < datos.periodos().size(); i++) {
            DatosPeriodoDto periodo = datos.periodos().get(i);
            String nombre = "El periodo " + (i + 1);
            if (!periodo.fechaFin().isAfter(periodo.fechaInicio())) {
                throw new ReglaNegocioException(nombre + " debe terminar después de empezar");
            }
            if (periodo.fechaInicio().isBefore(datos.fechaInicio()) || periodo.fechaFin().isAfter(datos.fechaFin())) {
                throw new ReglaNegocioException(nombre + " debe estar dentro de las fechas del año");
            }
            if (finAnterior != null && !periodo.fechaInicio().isAfter(finAnterior)) {
                throw new ReglaNegocioException(nombre + " debe empezar después de que termine el anterior");
            }
            finAnterior = periodo.fechaFin();
            suma = suma.add(periodo.porcentaje());
        }
        if (suma.compareTo(CIEN) != 0) {
            throw new ReglaNegocioException("Los porcentajes de los periodos deben sumar 100. Suman " + suma.stripTrailingZeros().toPlainString());
        }
    }

    private static List<Periodo> crearPeriodos(List<DatosPeriodoDto> datos) {
        List<Periodo> periodos = new ArrayList<>();
        for (int i = 0; i < datos.size(); i++) {
            DatosPeriodoDto periodo = datos.get(i);
            periodos.add(new Periodo(i + 1, periodo.fechaInicio(), periodo.fechaFin(), periodo.porcentaje()));
        }
        return periodos;
    }
}
