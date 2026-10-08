package co.edu.elencano.plataforma;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import jakarta.servlet.http.Cookie;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import co.edu.elencano.plataforma.usuarios.entidad.Rol;
import co.edu.elencano.plataforma.usuarios.entidad.TipoDocumento;
import co.edu.elencano.plataforma.usuarios.entidad.Usuario;
import co.edu.elencano.plataforma.usuarios.servicio.UsuarioService;
import co.edu.elencano.plataforma.usuarios.web.dto.CrearUsuarioDto;

/**
 * Base para las pruebas de integracion: levanta la aplicacion completa contra PostgreSQL en Docker.
 * Todas las clases que heredan comparten el mismo contexto y la misma base de datos,
 * por eso cada prueba crea usuarios con nombres propios.
 */
@SpringBootTest(properties = "plataforma.admin-inicial.contrasena=clave-de-prueba")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class PruebaIntegracion {

    protected static final String CONTRASENA = "clave-segura-123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UsuarioService usuarioService;

    private static int consecutivo = 1000;

    protected Usuario crearUsuario(String nombreUsuario, Rol... roles) {
        return usuarioService.crear(new CrearUsuarioDto(TipoDocumento.CC, String.valueOf(consecutivo++),
                "Nombre", "Apellido", null, null, nombreUsuario, CONTRASENA, Set.of(roles)));
    }

    /**
     * Envia el token CSRF como lo hace el navegador: en la cookie XSRF-TOKEN y en el encabezado X-XSRF-TOKEN.
     * No se usa csrf() de spring-security-test porque reemplaza el repositorio de tokens en el contexto
     * compartido y despues ninguna respuesta vuelve a entregar la cookie.
     */
    protected static RequestPostProcessor tokenCsrf() {
        return peticion -> {
            peticion.setCookies(new Cookie("XSRF-TOKEN", "token-de-prueba"));
            peticion.addHeader("X-XSRF-TOKEN", "token-de-prueba");
            return peticion;
        };
    }

    protected String jsonLogin(String nombreUsuario, String contrasena) {
        return """
                {"nombreUsuario": "%s", "contrasena": "%s"}
                """.formatted(nombreUsuario, contrasena);
    }

    /** Inicia sesion y devuelve la sesion HTTP para usarla en las siguientes peticiones. */
    protected MockHttpSession iniciarSesion(String nombreUsuario, String contrasena) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/api/auth/login").with(tokenCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin(nombreUsuario, contrasena)))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }
}
