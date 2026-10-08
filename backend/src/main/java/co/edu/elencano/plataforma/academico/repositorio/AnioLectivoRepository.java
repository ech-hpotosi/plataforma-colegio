package co.edu.elencano.plataforma.academico.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.elencano.plataforma.academico.entidad.AnioLectivo;
import co.edu.elencano.plataforma.academico.entidad.EstadoAnio;

public interface AnioLectivoRepository extends JpaRepository<AnioLectivo, Long> {

    List<AnioLectivo> findAllByOrderByAnioDesc();

    boolean existsByAnio(int anio);

    boolean existsByEstadoAndIdNot(EstadoAnio estado, Long id);
}
