package co.edu.elencano.plataforma.academico.servicio;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.elencano.plataforma.academico.entidad.Area;
import co.edu.elencano.plataforma.academico.entidad.Asignatura;
import co.edu.elencano.plataforma.academico.repositorio.AreaRepository;
import co.edu.elencano.plataforma.academico.repositorio.AsignaturaRepository;
import co.edu.elencano.plataforma.academico.web.dto.GuardarAsignaturaDto;
import co.edu.elencano.plataforma.comun.excepcion.RecursoNoEncontradoException;
import co.edu.elencano.plataforma.comun.excepcion.ReglaNegocioException;

/** Areas y asignaturas. Los nombres no se repiten. */
@Service
public class AreaService {

    private final AreaRepository areaRepository;
    private final AsignaturaRepository asignaturaRepository;

    public AreaService(AreaRepository areaRepository, AsignaturaRepository asignaturaRepository) {
        this.areaRepository = areaRepository;
        this.asignaturaRepository = asignaturaRepository;
    }

    @Transactional(readOnly = true)
    public List<Area> listar() {
        return areaRepository.listarConAsignaturas();
    }

    @Transactional
    public Area crear(String nombre) {
        validarNombreArea(nombre.trim(), 0L);
        return areaRepository.save(new Area(nombre.trim()));
    }

    @Transactional
    public Area actualizar(Long id, String nombre) {
        Area area = obtenerArea(id);
        validarNombreArea(nombre.trim(), id);
        area.setNombre(nombre.trim());
        area.getAsignaturas().size();
        return area;
    }

    @Transactional(readOnly = true)
    public Asignatura obtenerAsignatura(Long id) {
        return asignaturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la asignatura " + id));
    }

    @Transactional
    public Asignatura crearAsignatura(GuardarAsignaturaDto datos) {
        validarNombreAsignatura(datos.nombre().trim(), 0L);
        return asignaturaRepository.save(new Asignatura(obtenerArea(datos.areaId()), datos.nombre().trim()));
    }

    @Transactional
    public Asignatura actualizarAsignatura(Long id, GuardarAsignaturaDto datos) {
        Asignatura asignatura = obtenerAsignatura(id);
        validarNombreAsignatura(datos.nombre().trim(), id);
        asignatura.setNombre(datos.nombre().trim());
        asignatura.setArea(obtenerArea(datos.areaId()));
        return asignatura;
    }

    private Area obtenerArea(Long id) {
        return areaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el área " + id));
    }

    private void validarNombreArea(String nombre, Long id) {
        if (areaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ReglaNegocioException("Ya existe un área llamada " + nombre);
        }
    }

    private void validarNombreAsignatura(String nombre, Long id) {
        if (asignaturaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ReglaNegocioException("Ya existe una asignatura llamada " + nombre);
        }
    }
}
