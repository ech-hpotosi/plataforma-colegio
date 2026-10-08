package co.edu.elencano.plataforma.academico.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.elencano.plataforma.academico.entidad.Docente;

public interface DocenteRepository extends JpaRepository<Docente, Long> {
}
