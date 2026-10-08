package co.edu.elencano.plataforma.academico.servicio;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.elencano.plataforma.academico.entidad.Grado;
import co.edu.elencano.plataforma.academico.repositorio.GradoRepository;
import co.edu.elencano.plataforma.comun.excepcion.RecursoNoEncontradoException;

@Service
public class GradoService {

    private final GradoRepository gradoRepository;

    public GradoService(GradoRepository gradoRepository) {
        this.gradoRepository = gradoRepository;
    }

    @Transactional(readOnly = true)
    public List<Grado> listar() {
        return gradoRepository.findAllByOrderByOrdenAsc();
    }

    @Transactional(readOnly = true)
    public Grado obtener(Long id) {
        return gradoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el grado " + id));
    }

    /** Cambia si el grado se evalua de forma cualitativa (sin nota numerica), segun el SIEE. */
    @Transactional
    public Grado cambiarEvaluacionCualitativa(Long id, boolean cualitativa) {
        Grado grado = obtener(id);
        grado.setEvaluacionCualitativa(cualitativa);
        return grado;
    }
}
