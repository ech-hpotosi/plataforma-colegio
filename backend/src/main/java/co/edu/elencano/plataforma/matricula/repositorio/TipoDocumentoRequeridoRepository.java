package co.edu.elencano.plataforma.matricula.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.elencano.plataforma.matricula.entidad.TipoDocumentoRequerido;

public interface TipoDocumentoRequeridoRepository extends JpaRepository<TipoDocumentoRequerido, Long> {

    List<TipoDocumentoRequerido> findAllByOrderByOrdenAscNombreAsc();

    List<TipoDocumentoRequerido> findByActivoTrueOrderByOrdenAscNombreAsc();

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
