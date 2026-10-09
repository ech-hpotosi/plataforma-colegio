package co.edu.elencano.plataforma.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import co.edu.elencano.plataforma.PruebaIntegracion;
import co.edu.elencano.plataforma.usuarios.entidad.Rol;
import co.edu.elencano.plataforma.usuarios.entidad.Usuario;
import co.edu.elencano.plataforma.usuarios.repositorio.UsuarioRepository;

class AutenticacionTest extends PruebaIntegracion {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void loginCorrectoAbreSesionYYoDevuelveLosRoles() throws Exception {
        crearUsuario("docente.login", Rol.DOCENTE);

        MockHttpSession sesion = iniciarSesion("docente.login", CONTRASENA);

        mockMvc.perform(get("/api/yo").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreUsuario").value("docente.login"))
                .andExpect(jsonPath("$.roles[0]").value("DOCENTE"));
        assertThat(usuarioRepository.findByNombreUsuario("docente.login").orElseThrow().getUltimoAcceso())
                .isNotNull();
    }

    @Test
    void contrasenaIncorrectaResponde401() throws Exception {
        crearUsuario("clave.mala", Rol.SECRETARIA);

        mockMvc.perform(post("/api/auth/login").with(tokenCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin("clave.mala", "otra-clave")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Usuario o contraseña incorrectos"));
    }

    @Test
    void usuarioInexistenteRespondeIgualQueContrasenaIncorrecta() throws Exception {
        mockMvc.perform(post("/api/auth/login").with(tokenCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin("no.existe", "lo-que-sea")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Usuario o contraseña incorrectos"));
    }

    @Test
    void loginSinTokenCsrfEsRechazado() throws Exception {
        crearUsuario("sin.csrf", Rol.DOCENTE);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin("sin.csrf", CONTRASENA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cincoIntentosFallidosBloqueanLaCuenta() throws Exception {
        crearUsuario("bloqueo", Rol.DOCENTE);

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login").with(tokenCsrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonLogin("bloqueo", "incorrecta")))
                    .andExpect(status().isUnauthorized());
        }

        // Con la cuenta bloqueada, ni la contrasena correcta sirve
        mockMvc.perform(post("/api/auth/login").with(tokenCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin("bloqueo", CONTRASENA)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value(
                        "La cuenta está bloqueada temporalmente por intentos fallidos. Intente más tarde"));
    }

    @Test
    void usuarioInactivoNoPuedeIniciarSesion() throws Exception {
        Usuario usuario = crearUsuario("inactivo", Rol.DOCENTE);
        usuario.setActivo(false);
        usuarioRepository.save(usuario);

        mockMvc.perform(post("/api/auth/login").with(tokenCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin("inactivo", CONTRASENA)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value(
                        "El usuario está inactivo. Comuníquese con la secretaria del colegio"));
    }

    @Test
    void logoutCierraLaSesion() throws Exception {
        crearUsuario("logout", Rol.DOCENTE);
        MockHttpSession sesion = iniciarSesion("logout", CONTRASENA);

        mockMvc.perform(post("/api/auth/logout").with(tokenCsrf()).session(sesion))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/yo").session(sesion))
                .andExpect(status().isUnauthorized());
    }
}
