package co.edu.elencano.plataforma.usuarios;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import co.edu.elencano.plataforma.PruebaIntegracion;
import co.edu.elencano.plataforma.usuarios.entidad.Rol;
import co.edu.elencano.plataforma.usuarios.entidad.Usuario;

class UsuarioControllerTest extends PruebaIntegracion {

    private MockHttpSession sesionAdmin;

    @BeforeEach
    void iniciarSesionComoAdmin() throws Exception {
        sesionAdmin = iniciarSesion("admin", "clave-de-prueba");
    }

    private String jsonNuevoUsuario(String documento, String nombreUsuario) {
        return """
                {"tipoDocumento": "CC", "numeroDocumento": "%s", "nombres": "Maria", "apellidos": "Jojoa",
                 "telefono": "3001234567", "correo": "maria@ejemplo.co",
                 "nombreUsuario": "%s", "contrasena": "clave-segura-123", "roles": ["DOCENTE"]}
                """.formatted(documento, nombreUsuario);
    }

    @Test
    void administradorCreaUsuarioYLuegoEsePuedeIniciarSesion() throws Exception {
        mockMvc.perform(post("/api/usuarios").with(tokenCsrf()).session(sesionAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonNuevoUsuario("5001", "maria.jojoa")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreUsuario").value("maria.jojoa"))
                .andExpect(jsonPath("$.roles[0]").value("DOCENTE"))
                .andExpect(jsonPath("$.contrasena").doesNotExist());

        iniciarSesion("maria.jojoa", "clave-segura-123");
    }

    @Test
    void nombreDeUsuarioRepetidoResponde409() throws Exception {
        crearUsuario("repetido", Rol.DOCENTE);

        mockMvc.perform(post("/api/usuarios").with(tokenCsrf()).session(sesionAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonNuevoUsuario("5002", "repetido")))
                .andExpect(status().isConflict());
    }

    @Test
    void datosInvalidosResponden400ConElCampo() throws Exception {
        mockMvc.perform(post("/api/usuarios").with(tokenCsrf()).session(sesionAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonNuevoUsuario("5003", "Con Espacios")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombreUsuario").exists());
    }

    @Test
    void usuarioSinRolAdministradorRecibe403() throws Exception {
        crearUsuario("solo.docente", Rol.DOCENTE);
        MockHttpSession sesionDocente = iniciarSesion("solo.docente", CONTRASENA);

        mockMvc.perform(get("/api/usuarios").session(sesionDocente))
                .andExpect(status().isForbidden());
    }

    @Test
    void buscarFiltraPorTexto() throws Exception {
        crearUsuario("buscable.unico", Rol.ACUDIENTE);

        mockMvc.perform(get("/api/usuarios").param("buscar", "buscable").session(sesionAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].nombreUsuario").value("buscable.unico"));
    }

    @Test
    void administradorNoPuedeQuitarseSuPropioRol() throws Exception {
        Long idAdmin = usuarioService.buscar("admin", 0, 20).getContent().stream()
                .filter(u -> u.getNombreUsuario().equals("admin")).findFirst().orElseThrow().getId();

        mockMvc.perform(put("/api/usuarios/" + idAdmin).with(tokenCsrf()).session(sesionAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombres": "Administrador", "apellidos": "Plataforma", "activo": true,
                                 "roles": ["DOCENTE"]}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void cambiarContrasenaPermiteEntrarConLaNueva() throws Exception {
        Usuario usuario = crearUsuario("cambio.clave", Rol.SECRETARIA);

        mockMvc.perform(put("/api/usuarios/" + usuario.getId() + "/contrasena").with(tokenCsrf()).session(sesionAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contrasena": "nueva-clave-456"}
                                """))
                .andExpect(status().isNoContent());

        iniciarSesion("cambio.clave", "nueva-clave-456");
    }
}
