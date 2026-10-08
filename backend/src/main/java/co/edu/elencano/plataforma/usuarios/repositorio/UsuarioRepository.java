package co.edu.elencano.plataforma.usuarios.repositorio;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.elencano.plataforma.usuarios.entidad.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    boolean existsByNombreUsuario(String nombreUsuario);

    boolean existsByPersonaId(Long personaId);

    /** Busca por nombre de usuario, nombres, apellidos o numero de documento. Texto vacio devuelve todos. */
    @Query(value = """
            select u from Usuario u join fetch u.persona p
            where :texto = ''
               or lower(u.nombreUsuario) like concat('%', :texto, '%')
               or lower(p.nombres) like concat('%', :texto, '%')
               or lower(p.apellidos) like concat('%', :texto, '%')
               or p.numeroDocumento like concat('%', :texto, '%')
            """,
            countQuery = """
            select count(u) from Usuario u join u.persona p
            where :texto = ''
               or lower(u.nombreUsuario) like concat('%', :texto, '%')
               or lower(p.nombres) like concat('%', :texto, '%')
               or lower(p.apellidos) like concat('%', :texto, '%')
               or p.numeroDocumento like concat('%', :texto, '%')
            """)
    Page<Usuario> buscar(@Param("texto") String texto, Pageable pageable);
}
