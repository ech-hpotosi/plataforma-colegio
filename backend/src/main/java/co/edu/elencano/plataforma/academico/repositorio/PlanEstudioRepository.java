package co.edu.elencano.plataforma.academico.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.elencano.plataforma.academico.entidad.PlanEstudio;

public interface PlanEstudioRepository extends JpaRepository<PlanEstudio, Long> {

    @Query("""
            select p from PlanEstudio p join fetch p.asignatura s join fetch s.area
            where p.anioLectivo.id = :anioId and p.grado.id = :gradoId
            order by s.area.nombre, s.nombre
            """)
    List<PlanEstudio> listar(@Param("anioId") Long anioId, @Param("gradoId") Long gradoId);

    boolean existsByAnioLectivoIdAndGradoIdAndAsignaturaId(Long anioId, Long gradoId, Long asignaturaId);
}
