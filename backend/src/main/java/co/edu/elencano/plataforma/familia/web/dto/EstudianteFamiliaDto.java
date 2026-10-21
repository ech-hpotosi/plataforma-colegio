package co.edu.elencano.plataforma.familia.web.dto;

import java.util.List;

/** Estudiante que la persona puede consultar, con sus matriculas activas (la mas reciente primero). */
public record EstudianteFamiliaDto(Long id, String nombres, String apellidos, String parentesco,
                                   List<Matricula> matriculas) {

    public record Matricula(Long id, int anio, String grupo, String sede) {
    }
}
