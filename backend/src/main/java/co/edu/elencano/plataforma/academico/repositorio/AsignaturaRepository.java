package co.edu.elencano.plataforma.academico.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.elencano.plataforma.academico.entidad.Asignatura;

public interface AsignaturaRepository extends JpaRepository<Asignatura, Long> {

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
