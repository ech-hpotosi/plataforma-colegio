package co.edu.elencano.plataforma.matricula.web.dto;

import java.util.List;

/** Anio lectivo con la matricula en linea abierta, y las sedes y grados que se pueden solicitar. */
public record OfertaMatriculaDto(Long anioLectivoId, int anio, List<Opcion> sedes, List<Opcion> grados) {

    public record Opcion(Long id, String nombre) {
    }
}
