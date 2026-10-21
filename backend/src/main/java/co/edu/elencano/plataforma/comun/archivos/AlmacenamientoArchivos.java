package co.edu.elencano.plataforma.comun.archivos;

import java.io.InputStream;

import org.springframework.core.io.Resource;

/**
 * Guarda archivos subidos por los usuarios (documentos de matricula) fuera de la base de datos.
 * Las rutas que devuelve son relativas y las genera el sistema, nunca el nombre que trae el archivo.
 */
public interface AlmacenamientoArchivos {

    /** Guarda el contenido en la carpeta indicada y devuelve la ruta relativa con la que se recupera. */
    String guardar(String carpeta, String extension, InputStream contenido);

    Resource leer(String ruta);

    /** Elimina el archivo si existe. No falla si ya no esta. */
    void eliminar(String ruta);
}
