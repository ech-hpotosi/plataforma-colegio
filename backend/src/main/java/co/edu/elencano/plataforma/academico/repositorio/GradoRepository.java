package co.edu.elencano.plataforma.academico.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.elencano.plataforma.academico.entidad.Grado;

public interface GradoRepository extends JpaRepository<Grado, Long> {

    List<Grado> findAllByOrderByOrdenAsc();
}
