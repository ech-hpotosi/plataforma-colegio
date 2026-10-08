package co.edu.elencano.plataforma.usuarios.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.elencano.plataforma.usuarios.entidad.Persona;
import co.edu.elencano.plataforma.usuarios.entidad.TipoDocumento;

public interface PersonaRepository extends JpaRepository<Persona, Long> {

    Optional<Persona> findByTipoDocumentoAndNumeroDocumento(TipoDocumento tipoDocumento, String numeroDocumento);
}
