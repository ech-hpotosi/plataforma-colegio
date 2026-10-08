package co.edu.elencano.plataforma.estudiantes.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.elencano.plataforma.estudiantes.entidad.Acudiente;

public interface AcudienteRepository extends JpaRepository<Acudiente, Long> {
}
