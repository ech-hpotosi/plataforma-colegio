package co.edu.elencano.plataforma.comun.web;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** Pagina de resultados con un formato estable para el frontend. */
public record Pagina<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    public static <E, T> Pagina<T> de(Page<E> page, Function<E, T> convertir) {
        return new Pagina<>(
                page.getContent().stream().map(convertir).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
