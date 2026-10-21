package co.edu.elencano.plataforma.usuarios.seguridad;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import co.edu.elencano.plataforma.comun.excepcion.ReglaNegocioException;

/**
 * Limita cuantas cuentas se crean por hora desde una misma direccion, para que el registro publico
 * de acudientes no se use para llenar la base de datos. Se lleva en memoria: se reinicia con el servidor.
 */
@Component
public class LimiteRegistros {

    private final int maximoPorHora;
    private final Clock reloj;
    private final Map<String, Deque<Instant>> registros = new ConcurrentHashMap<>();

    public LimiteRegistros(@Value("${plataforma.seguridad.registros-por-hora:10}") int maximoPorHora) {
        this.maximoPorHora = maximoPorHora;
        this.reloj = Clock.systemUTC();
    }

    public void verificar(String direccion) {
        Instant ahora = reloj.instant();
        Instant limite = ahora.minusSeconds(3600);
        Deque<Instant> recientes = registros.computeIfAbsent(direccion, d -> new ArrayDeque<>());
        synchronized (recientes) {
            while (!recientes.isEmpty() && recientes.peekFirst().isBefore(limite)) {
                recientes.pollFirst();
            }
            if (recientes.size() >= maximoPorHora) {
                throw new ReglaNegocioException("Se crearon demasiadas cuentas desde esta conexión. Intente más tarde");
            }
            recientes.addLast(ahora);
        }
    }
}
