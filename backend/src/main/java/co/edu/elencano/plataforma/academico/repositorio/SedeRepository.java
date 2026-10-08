package co.edu.elencano.plataforma.academico.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.elencano.plataforma.academico.entidad.Sede;

public interface SedeRepository extends JpaRepository<Sede, Long> {

    List<Sede> findAllByOrderByPrincipalDescNombreAsc();

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    boolean existsByCodigoDaneAndIdNot(String codigoDane, Long id);

    /** Quita la marca de principal a todas las sedes menos a la indicada. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Sede s set s.principal = false where s.principal = true and s.id <> :id")
    void quitarPrincipalExcepto(@Param("id") Long id);
}
