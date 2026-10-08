package co.edu.elencano.plataforma.academico.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import co.edu.elencano.plataforma.academico.entidad.Area;

public interface AreaRepository extends JpaRepository<Area, Long> {

    @Query("select distinct a from Area a left join fetch a.asignaturas order by a.nombre")
    List<Area> listarConAsignaturas();

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
