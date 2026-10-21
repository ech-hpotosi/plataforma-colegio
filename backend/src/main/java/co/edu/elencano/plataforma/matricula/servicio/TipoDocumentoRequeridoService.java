package co.edu.elencano.plataforma.matricula.servicio;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import co.edu.elencano.plataforma.comun.excepcion.RecursoNoEncontradoException;
import co.edu.elencano.plataforma.comun.excepcion.ReglaNegocioException;
import co.edu.elencano.plataforma.matricula.entidad.TipoDocumentoRequerido;
import co.edu.elencano.plataforma.matricula.repositorio.TipoDocumentoRequeridoRepository;
import co.edu.elencano.plataforma.matricula.web.dto.GuardarTipoDocumentoDto;

/**
 * Documentos que se piden en la matricula en linea. No se borran: se desactivan, porque puede haber
 * solicitudes que ya tienen ese documento.
 */
@Service
public class TipoDocumentoRequeridoService {

    private final TipoDocumentoRequeridoRepository repository;

    public TipoDocumentoRequeridoService(TipoDocumentoRequeridoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TipoDocumentoRequerido> listar(boolean soloActivos) {
        return soloActivos ? repository.findByActivoTrueOrderByOrdenAscNombreAsc()
                : repository.findAllByOrderByOrdenAscNombreAsc();
    }

    @Transactional
    public TipoDocumentoRequerido crear(GuardarTipoDocumentoDto datos) {
        TipoDocumentoRequerido tipo = new TipoDocumentoRequerido(datos.nombre().trim());
        aplicar(tipo, datos);
        return repository.save(tipo);
    }

    @Transactional
    public TipoDocumentoRequerido actualizar(Long id, GuardarTipoDocumentoDto datos) {
        TipoDocumentoRequerido tipo = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el documento " + id));
        aplicar(tipo, datos);
        return tipo;
    }

    private void aplicar(TipoDocumentoRequerido tipo, GuardarTipoDocumentoDto datos) {
        String nombre = datos.nombre().trim();
        if (repository.existsByNombreIgnoreCaseAndIdNot(nombre, tipo.getId() == null ? 0L : tipo.getId())) {
            throw new ReglaNegocioException("Ya existe el documento " + nombre);
        }
        tipo.setNombre(nombre);
        tipo.setDescripcion(StringUtils.hasText(datos.descripcion()) ? datos.descripcion().trim() : null);
        tipo.setObligatorio(datos.obligatorio());
        tipo.setActivo(datos.activo());
        tipo.setOrden(datos.orden());
    }
}
