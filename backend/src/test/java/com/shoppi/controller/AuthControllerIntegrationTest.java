package com.shoppi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoppi.dto.auth.LoginRequest;
import com.shoppi.dto.auth.RegisterRequest;
import com.shoppi.entity.Usuario;
import com.shoppi.repository.ClienteRepository;
import com.shoppi.repository.DetalleVentaRepository;
import com.shoppi.repository.InventarioRepository;
import com.shoppi.repository.ProductoRepository;
import com.shoppi.repository.UsuarioRepository;
import com.shoppi.repository.VentaRepository;
import com.shoppi.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración de {@code /api/auth} con H2 en memoria.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private ProductoRepository productos;

    @Autowired
    private InventarioRepository inventarios;

    @Autowired
    private ClienteRepository clientes;

    @Autowired
    private VentaRepository ventas;

    @Autowired
    private DetalleVentaRepository detalles;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void limpiarUsuarios() {
        // La base H2 es compartida entre clases: respetar el orden de las FK.
        detalles.deleteAll();
        ventas.deleteAll();
        inventarios.deleteAll();
        productos.deleteAll();
        clientes.deleteAll();
        usuarios.deleteAll();
    }

    @Test
    void registrarDevuelve201ConTokenYGuardaPasswordHasheada() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Juan Pérez", "Juan@Ejemplo.com", "contraseña123", "7777-7777");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Juan Pérez"))
                .andExpect(jsonPath("$.email").value("juan@ejemplo.com"))
                .andExpect(jsonPath("$.token").isNotEmpty());

        Usuario guardado = usuarios.findByEmail("juan@ejemplo.com").orElseThrow();
        assertNotEquals("contraseña123", guardado.getPasswordHash());
        assertTrue(passwordEncoder.matches("contraseña123", guardado.getPasswordHash()));
    }

    @Test
    void registrarCorreoDuplicadoDevuelve400() throws Exception {
        usuarios.save(new Usuario(
                "Juan Pérez", "juan@ejemplo.com", passwordEncoder.encode("contrasena123"), null));

        RegisterRequest request = new RegisterRequest(
                "Otro Juan", "juan@ejemplo.com", "otraClave123", null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El correo ya está registrado."));
    }

    @Test
    void registrarDatosInvalidosDevuelve400ConDetalleDeCampos() throws Exception {
        String cuerpo = """
                {
                  "nombre": "Juan",
                  "email": "no-es-un-correo",
                  "password": "corta",
                  "telefono": "7777-7777"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("nombre")))
                .andExpect(jsonPath("$.message", containsString("email")))
                .andExpect(jsonPath("$.message", containsString("password")));
    }

    @Test
    void loginDevuelve200ConTokenParaCredencialesValidas() throws Exception {
        usuarios.save(new Usuario(
                "Juan Pérez", "juan@ejemplo.com", passwordEncoder.encode("contrasena123"), null));

        LoginRequest request = new LoginRequest("Juan@Ejemplo.com", "contrasena123");

        String respuesta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Juan Pérez"))
                .andExpect(jsonPath("$.email").value("juan@ejemplo.com"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(respuesta).get("token").asText();
        assertTrue(jwtService.isValid(token));
        assertEquals("juan@ejemplo.com", jwtService.extractEmail(token));
    }

    @Test
    void loginPasswordIncorrectaDevuelve401Generico() throws Exception {
        usuarios.save(new Usuario(
                "Juan Pérez", "juan@ejemplo.com", passwordEncoder.encode("contrasena123"), null));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("juan@ejemplo.com", "otraClave123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Correo o contraseña incorrectos."));
    }

    @Test
    void loginCorreoDesconocidoDevuelve401Generico() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("nadie@ejemplo.com", "contrasena123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Correo o contraseña incorrectos."));
    }

    @Test
    void loginCamposVaciosDevuelve400() throws Exception {
        String cuerpo = """
                {"email": "", "password": ""}
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("email")))
                .andExpect(jsonPath("$.message", containsString("password")));
    }
}
