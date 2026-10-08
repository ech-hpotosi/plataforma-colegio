package co.edu.elencano.plataforma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import co.edu.elencano.plataforma.usuarios.entidad.Rol;
import co.edu.elencano.plataforma.usuarios.entidad.Usuario;
import co.edu.elencano.plataforma.usuarios.repositorio.UsuarioRepository;

@SpringBootTest(properties = "plataforma.admin-inicial.contrasena=clave-de-prueba")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PlataformaApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void saludEsPublica() throws Exception {
        mockMvc.perform(get("/api/salud"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OK"));
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
