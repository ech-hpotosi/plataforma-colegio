package co.edu.elencano.plataforma.notas.repositorio;

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
}
