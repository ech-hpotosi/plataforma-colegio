package co.edu.elencano.plataforma.notas.repositorio;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.elencano.plataforma.notas.entidad.ActividadEvaluativa;

public interface ActividadEvaluativaRepository extends JpaRepository<ActividadEvaluativa, Long> {

    @Query("""
            select a from ActividadEvaluativa a
            where a.carga.id = :cargaId and a.periodo.id = :periodoId
            order by a.fecha nulls last, a.id
            """)
    List<ActividadEvaluativa> listar(@Param("cargaId") Long cargaId, @Param("periodoId") Long periodoId);

    @Query("""
            select new co.edu.elencano.plataforma.notas.repositorio.ConteoActividades(
                   a.carga.id, a.periodo.id, a.dimension, count(a))
            from ActividadEvaluativa a
            where a.carga.id in :cargaIds
            group by a.carga.id, a.periodo.id, a.dimension
            """)
    List<ConteoActividades> contarDeCargas(@Param("cargaIds") Collection<Long> cargaIds);

    @Query("select a from ActividadEvaluativa a where a.carga.grupo.id = :grupoId")
    List<ActividadEvaluativa> listarDeGrupo(@Param("grupoId") Long grupoId);
}
