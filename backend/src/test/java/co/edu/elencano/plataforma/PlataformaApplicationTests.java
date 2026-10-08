package co.edu.elencano.plataforma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import co.edu.elencano.plataforma.usuarios.entidad.Rol;
import co.edu.elencano.plataforma.usuarios.entidad.Usuario;
import co.edu.elencano.plataforma.usuarios.repositorio.UsuarioRepository;

class PlataformaApplicationTests extends PruebaIntegracion {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void saludEsPublicaYEntregaCookieCsrf() throws Exception {
        mockMvc.perform(get("/api/salud"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OK"))
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void rutaProtegidaSinSesionResponde401() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void seCreaElAdministradorInicial() {
        Usuario admin = usuarioRepository.findByNombreUsuario("admin").orElseThrow();
        assertThat(admin.tieneRol(Rol.ADMINISTRADOR)).isTrue();
        assertThat(passwordEncoder.matches("clave-de-prueba", admin.getContrasenaHash())).isTrue();
    }
}
