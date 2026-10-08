package co.edu.elencano.plataforma.academico.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.elencano.plataforma.academico.entidad.Grupo;

public interface GrupoRepository extends JpaRepository<Grupo, Long> {

    /** Grupos de un anio, opcionalmente filtrados por sede, ordenados por sede, grado y nombre. */
    @Query("""
            select g from Grupo g join fetch g.sede join fetch g.grado left join fetch g.director d left join fetch d.persona
            where g.anioLectivo.id = :anioId and (:sedeId is null or g.sede.id = :sedeId)
            order by g.sede.nombre, g.grado.orden, g.nombre
            """)
    List<Grupo> listar(@Param("anioId") Long anioId, @Param("sedeId") Long sedeId);

    boolean existsByAnioLectivoIdAndSedeIdAndGradoIdAndNombreIgnoreCaseAndIdNot(
            Long anioId, Long sedeId, Long gradoId, String nombre, Long id);
}
