package co.edu.elencano.plataforma.notas.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.elencano.plataforma.notas.entidad.Recuperacion;

public interface RecuperacionRepository extends JpaRepository<Recuperacion, Long> {

    /** Recuperaciones de una carga: las de todos los periodos y la final. */
    @Query("select r from Recuperacion r where r.carga.id = :cargaId")
    List<Recuperacion> listarDeCarga(@Param("cargaId") Long cargaId);

    @Query("select r from Recuperacion r where r.carga.grupo.id = :grupoId")
    List<Recuperacion> listarDeGrupo(@Param("grupoId") Long grupoId);
}
