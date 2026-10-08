package co.edu.elencano.plataforma.academico.servicio;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.elencano.plataforma.academico.entidad.Docente;
import co.edu.elencano.plataforma.academico.entidad.Sede;
import co.edu.elencano.plataforma.academico.repositorio.DocenteRepository;
import co.edu.elencano.plataforma.academico.repositorio.SedeRepository;
import co.edu.elencano.plataforma.academico.web.dto.ActualizarDocenteDto;
import co.edu.elencano.plataforma.academico.web.dto.DocenteDto;
import co.edu.elencano.plataforma.comun.excepcion.RecursoNoEncontradoException;
import co.edu.elencano.plataforma.comun.excepcion.ReglaNegocioException;
import co.edu.elencano.plataforma.usuarios.entidad.Rol;
import co.edu.elencano.plataforma.usuarios.entidad.Usuario;
import co.edu.elencano.plataforma.usuarios.repositorio.UsuarioRepository;

/**
 * Docentes: son las personas con un usuario activo con rol DOCENTE. Los datos academicos
 * (especialidad, escalafon, sedes) se guardan en la tabla docente, que se crea la primera vez que se necesita.
 */
@Service
public class DocenteService {

    private final DocenteRepository docenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final SedeRepository sedeRepository;

    public DocenteService(DocenteRepository docenteRepository, UsuarioRepository usuarioRepository,
                          SedeRepository sedeRepository) {
        this.docenteRepository = docenteRepository;
        this.usuarioRepository = usuarioRepository;
        this.sedeRepository = sedeRepository;
    }

    @Transactional(readOnly = true)
    public List<DocenteDto> listar() {
        List<Usuario> usuarios = usuarioRepository.listarActivosConRol(Rol.DOCENTE);
        Map<Long, Docente> docentes = docenteRepository
                .findAllById(usuarios.stream().map(u -> u.getPersona().getId()).toList()).stream()
                .collect(Collectors.toMap(Docente::getId, Function.identity()));
        return usuarios.stream()
                .map(u -> DocenteDto.de(u.getPersona(), docentes.get(u.getPersona().getId())))
                .toList();
    }

    /**
     * Devuelve el docente con el id de persona dado, creandolo si hace falta.
     * Falla si la persona no tiene un usuario activo con rol DOCENTE.
     */
    @Transactional
    public Docente obtenerParaAsignar(Long personaId) {
        Usuario usuario = usuarioRepository.findByPersonaId(personaId)
                .filter(u -> u.isActivo() && u.tieneRol(Rol.DOCENTE))
                .orElseThrow(() -> new ReglaNegocioException("La persona " + personaId + " no es un docente activo"));
        return docenteRepository.findById(personaId)
                .orElseGet(() -> docenteRepository.save(new Docente(usuario.getPersona())));
    }

    @Transactional
    public Docente actualizar(Long personaId, ActualizarDocenteDto datos) {
        Docente docente = obtenerParaAsignar(personaId);
        Set<Sede> sedes = new HashSet<>(sedeRepository.findAllById(datos.sedeIds()));
        if (sedes.size() != datos.sedeIds().size()) {
            throw new RecursoNoEncontradoException("Alguna de las sedes no existe");
        }
        docente.setEspecialidad(Textos.vacioANulo(datos.especialidad()));
        docente.setEscalafon(Textos.vacioANulo(datos.escalafon()));
        docente.setSedes(sedes);
        return docente;
    }
}
