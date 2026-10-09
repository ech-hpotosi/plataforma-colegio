package co.edu.elencano.plataforma.academico.servicio;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.elencano.plataforma.academico.entidad.Sede;
import co.edu.elencano.plataforma.academico.repositorio.SedeRepository;
import co.edu.elencano.plataforma.academico.web.dto.GuardarSedeDto;
import co.edu.elencano.plataforma.comun.excepcion.RecursoNoEncontradoException;
import co.edu.elencano.plataforma.comun.excepcion.ReglaNegocioException;

@Service
public class SedeService {

    private final SedeRepository sedeRepository;

    public SedeService(SedeRepository sedeRepository) {
        this.sedeRepository = sedeRepository;
    }

    @Transactional(readOnly = true)
    public List<Sede> listar() {
        return sedeRepository.findAllByOrderByPrincipalDescNombreAsc();
    }

    @Transactional(readOnly = true)
    public Sede obtener(Long id) {
        return sedeRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la sede " + id));
    }

    @Transactional
    public Sede crear(GuardarSedeDto datos) {
        Sede sede = new Sede(datos.nombre().trim());
        return guardar(sede, datos);
    }

    @Transactional
    public Sede actualizar(Long id, GuardarSedeDto datos) {
        return guardar(obtener(id), datos);
    }

    /** Valida duplicados y, si la sede queda como principal, se la quita a las demas. */
    private Sede guardar(Sede sede, GuardarSedeDto datos) {
        Long id = sede.getId() == null ? 0L : sede.getId();
        String nombre = datos.nombre().trim();
        String codigoDane = Textos.vacioANulo(datos.codigoDane());
        if (sedeRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ReglaNegocioException("Ya existe una sede llamada " + nombre);
        }
        if (codigoDane != null && sedeRepository.existsByCodigoDaneAndIdNot(codigoDane, id)) {
            throw new ReglaNegocioException("El código DANE " + codigoDane + " ya está registrado en otra sede");
        }
        sede.setNombre(nombre);
        sede.setCodigoDane(codigoDane);
        sede.setDireccion(Textos.vacioANulo(datos.direccion()));
        sede.setPrincipal(datos.principal());
        Sede guardada = sedeRepository.save(sede);
        if (guardada.isPrincipal()) {
            sedeRepository.quitarPrincipalExcepto(guardada.getId());
        }
        return guardada;
    }
}
