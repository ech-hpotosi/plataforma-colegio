package co.edu.elencano.plataforma.comun.archivos;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import co.edu.elencano.plataforma.comun.excepcion.RecursoNoEncontradoException;

/**
 * Almacenamiento en una carpeta privada del servidor (plataforma.archivos.directorio).
 * La carpeta no se publica en el servidor web; los archivos solo salen por la API, que verifica permisos.
 */
@Component
public class AlmacenamientoEnDisco implements AlmacenamientoArchivos {

    private final Path raiz;

    public AlmacenamientoEnDisco(@Value("${plataforma.archivos.directorio}") String directorio) {
        this.raiz = Path.of(directorio).toAbsolutePath().normalize();
    }

    @Override
    public String guardar(String carpeta, String extension, InputStream contenido) {
        String ruta = carpeta + "/" + UUID.randomUUID() + "." + extension;
        Path destino = resolver(ruta);
        try {
            Files.createDirectories(destino.getParent());
            Files.copy(contenido, destino);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo " + ruta, e);
        }
        return ruta;
    }

    @Override
    public Resource leer(String ruta) {
        Path archivo = resolver(ruta);
        if (!Files.isRegularFile(archivo)) {
            throw new RecursoNoEncontradoException("El archivo ya no está disponible");
        }
        return new FileSystemResource(archivo);
    }

    @Override
    public void eliminar(String ruta) {
        try {
            Files.deleteIfExists(resolver(ruta));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo eliminar el archivo " + ruta, e);
        }
    }

    /** Evita que una ruta manipulada salga de la carpeta raiz. */
    private Path resolver(String ruta) {
        Path archivo = raiz.resolve(ruta).normalize();
        if (!archivo.startsWith(raiz)) {
            throw new IllegalArgumentException("Ruta de archivo no permitida");
        }
        return archivo;
    }
}
