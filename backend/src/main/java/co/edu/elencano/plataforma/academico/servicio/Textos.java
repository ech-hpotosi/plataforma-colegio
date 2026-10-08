package co.edu.elencano.plataforma.academico.servicio;

import org.springframework.util.StringUtils;

/** Utilidades de texto para los servicios academicos. */
final class Textos {

    private Textos() {
    }

    static String vacioANulo(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }
}
