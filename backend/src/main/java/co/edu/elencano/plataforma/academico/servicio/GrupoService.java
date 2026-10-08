package co.edu.elencano.plataforma.academico.servicio;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.elencano.plataforma.academico.entidad.AnioLectivo;
import co.edu.elencano.plataforma.academico.entidad.Docente;
import co.edu.elencano.plataforma.academico.entidad.Grado;
import co.edu.elencano.plataforma.academico.entidad.Grupo;
import co.edu.elencano.plataforma.academico.entidad.Sede;
import co.edu.elencano.plataforma.academico.repositorio.CargaAcademicaRepository;
import co.edu.elencano.plataforma.academico.repositorio.GrupoRepository;
import co.edu.elencano.plataforma.academico.web.dto.GuardarGrupoDto;
import co.edu.elencano.plataforma.comun.excepcion.RecursoNoEncontradoException;
import co.edu.elencano.plataforma.comun.excepcion.ReglaNegocioException;

/** Grupos de cada anio lectivo. El director de grupo debe trabajar en la sede del grupo. */
@Service
public class GrupoService {

    private final GrupoRepository grupoRepository;
    private final CargaAcademicaRepository cargaRepository;
    private final AnioLectivoService anioService;
    private final SedeService sedeService;
    private final GradoService gradoService;
    private final DocenteService docenteService;

    public GrupoService(GrupoRepository grupoRepository, CargaAcademicaRepository cargaRepository,
                        AnioLectivoService anioService, SedeService sedeService, GradoService gradoService,
                        DocenteService docenteService) {
        this.grupoRepository = grupoRepository;
        this.cargaRepository = cargaRepository;
        this.anioService = anioService;
        this.sedeService = sedeService;
        this.gradoService = gradoService;
        this.docenteService = docenteService;
    }

    @Transactional(readOnly = true)
    public List<Grupo> listar(Long anioId, Long sedeId) {
        return grupoRepository.listar(anioId, sedeId);
    }

    @Transactional(readOnly = true)
    public Grupo obtener(Long id) {
        return grupoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el grupo " + id));
    }

    @Transactional
    public Grupo crear(GuardarGrupoDto datos) {
        AnioLectivo anio = anioService.obtenerAbierto(datos.anioLectivoId());
        Grupo grupo = new Grupo(anio, sedeService.obtener(datos.sedeId()), gradoService.obtener(datos.gradoId()));
        return grupoRepository.save(aplicar(grupo, datos));
    }

    @Transactional
    public Grupo actualizar(Long id, GuardarGrupoDto datos) {
        Grupo grupo = obtener(id);
        anioService.obtenerAbierto(grupo.getAnioLectivo().getId());
        Sede sede = sedeService.obtener(datos.sedeId());
        Grado grado = gradoService.obtener(datos.gradoId());
        boolean cambiaGrado = !grado.getId().equals(grupo.getGrado().getId());
        if (cambiaGrado && cargaRepository.existsByGrupoId(id)) {
            throw new ReglaNegocioException("No se puede cambiar el grado porque el grupo ya tiene carga academica");
        }
        grupo.setSede(sede);
        grupo.setGrado(grado);
        return aplicar(grupo, datos);
    }

    private Grupo aplicar(Grupo grupo, GuardarGrupoDto datos) {
        String nombre = datos.nombre().trim();
        Long id = grupo.getId() == null ? 0L : grupo.getId();
        if (grupoRepository.existsByAnioLectivoIdAndSedeIdAndGradoIdAndNombreIgnoreCaseAndIdNot(
                grupo.getAnioLectivo().getId(), grupo.getSede().getId(), grupo.getGrado().getId(), nombre, id)) {
            throw new ReglaNegocioException("Ya existe el grupo " + nombre + " de " + grupo.getGrado().getNombre()
                    + " en la sede " + grupo.getSede().getNombre());
        }
        Docente director = null;
        if (datos.directorId() != null) {
            director = docenteService.obtenerParaAsignar(datos.directorId());
            if (!director.trabajaEn(grupo.getSede())) {
                throw new ReglaNegocioException(director.getPersona().getNombreCompleto()
                        + " no trabaja en la sede " + grupo.getSede().getNombre());
            }
        }
        grupo.setNombre(nombre);
        grupo.setJornada(datos.jornada());
        grupo.setCupo(datos.cupo());
        grupo.setDirector(director);
        return grupo;
    }
}
