package com.shoppi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoppi.dto.cliente.ClienteRequest;
import com.shoppi.entity.Cliente;
import com.shoppi.entity.Usuario;
import com.shoppi.repository.ClienteRepository;
import com.shoppi.repository.InventarioRepository;
import com.shoppi.repository.ProductoRepository;
import com.shoppi.repository.UsuarioRepository;
import com.shoppi.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración del CRUD de clientes, incluyendo el aislamiento por
 * usuario autenticado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClienteControllerIntegrationTest {

    private static final String URL = "/api/clientes";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private ClienteRepository clientes;

    @Autowired
    private ProductoRepository productos;

    @Autowired
    private InventarioRepository inventarios;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Usuario juan;
    private Usuario ana;
    private String tokenJuan;
    private String tokenAna;

    @BeforeEach
    void setUp() {
        inventarios.deleteAll();
        productos.deleteAll();
        clientes.deleteAll();
        usuarios.deleteAll();

        juan = usuarios.saveAndFlush(new Usuario(
                "Juan Pérez", "juan@ejemplo.com", passwordEncoder.encode("contrasena123"), null));
        ana = usuarios.saveAndFlush(new Usuario(
                "Ana Gómez", "ana@ejemplo.com", passwordEncoder.encode("contrasena123"), null));

        tokenJuan = jwtService.generateToken(juan.getId(), juan.getEmail());
        tokenAna = jwtService.generateToken(ana.getId(), ana.getEmail());
    }

    @Test
    void crearClienteDevuelve201YLoPersiste() throws Exception {
        ClienteRequest request = new ClienteRequest(
                "María García", "7777-8888", "maria@ejemplo.com", "San Salvador");

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("María García"))
                .andExpect(jsonPath("$.telefono").value("7777-8888"))
                .andExpect(jsonPath("$.email").value("maria@ejemplo.com"))
                .andExpect(jsonPath("$.direccion").value("San Salvador"));

        assertEquals(1, clientes.findByUsuarioId(juan.getId()).size());
        assertEquals("María García", clientes.findByUsuarioId(juan.getId()).get(0).getNombre());
    }

    @Test
    void crearClienteSinCamposOpcionalesLosGuardaComoNulos() throws Exception {
        ClienteRequest request = new ClienteRequest("Pedro López", "  ", null, "");

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Pedro López"))
                .andExpect(jsonPath("$.telefono").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.direccion").doesNotExist());

        Cliente guardado = clientes.findByUsuarioId(juan.getId()).get(0);
        assertNull(guardado.getTelefono());
        assertNull(guardado.getEmail());
        assertNull(guardado.getDireccion());
    }

    @Test
    void listarSoloDevuelveClientesDelUsuarioAutenticado() throws Exception {
        guardarCliente(juan, "María García");
        guardarCliente(ana, "Carlos Ruiz");

        mockMvc.perform(get(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("María García"));
    }

    @Test
    void obtenerClienteDeOtroUsuarioDevuelve404() throws Exception {
        Cliente deAna = guardarCliente(ana, "Carlos Ruiz");

        mockMvc.perform(get(URL + "/" + deAna.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cliente no encontrado."));
    }

    @Test
    void actualizarClientePropioDevuelve200() throws Exception {
        Cliente maria = guardarCliente(juan, "María García");
        ClienteRequest cambios = new ClienteRequest(
                "María García de López", "8888-9999", "maria.lopez@ejemplo.com", "Santa Tecla");

        mockMvc.perform(put(URL + "/" + maria.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cambios)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("María García de López"))
                .andExpect(jsonPath("$.telefono").value("8888-9999"))
                .andExpect(jsonPath("$.email").value("maria.lopez@ejemplo.com"))
                .andExpect(jsonPath("$.direccion").value("Santa Tecla"));

        Cliente guardado = clientes.findById(maria.getId()).orElseThrow();
        assertEquals("María García de López", guardado.getNombre());
        assertEquals("Santa Tecla", guardado.getDireccion());
    }

    @Test
    void actualizarClienteDeOtroUsuarioDevuelve404YNoLoModifica() throws Exception {
        Cliente deAna = guardarCliente(ana, "Carlos Ruiz");
        ClienteRequest cambios = new ClienteRequest(
                "Carlos editado", null, null, null);

        mockMvc.perform(put(URL + "/" + deAna.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cambios)))
                .andExpect(status().isNotFound());

        assertEquals("Carlos Ruiz", clientes.findById(deAna.getId()).orElseThrow().getNombre());
    }

    @Test
    void eliminarClientePropioDevuelve204() throws Exception {
        Cliente maria = guardarCliente(juan, "María García");

        mockMvc.perform(delete(URL + "/" + maria.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isNoContent());

        assertTrue(clientes.findById(maria.getId()).isEmpty());
    }

    @Test
    void eliminarClienteDeOtroUsuarioDevuelve404YNoLoBorra() throws Exception {
        Cliente deAna = guardarCliente(ana, "Carlos Ruiz");

        mockMvc.perform(delete(URL + "/" + deAna.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isNotFound());

        assertFalse(clientes.findById(deAna.getId()).isEmpty());
    }

    @Test
    void crearConDatosInvalidosDevuelve400ConDetalleDeCampos() throws Exception {
        String cuerpo = """
                {
                  "nombre": "   ",
                  "telefono": "123456789012345678901",
                  "email": "no-es-correo"
                }
                """;

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("nombre")))
                .andExpect(jsonPath("$.message", containsString("telefono")))
                .andExpect(jsonPath("$.message", containsString("email")));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autenticación requerida."));
    }

    private Cliente guardarCliente(Usuario usuario, String nombre) {
        return clientes.saveAndFlush(new Cliente(
                usuario,
                nombre,
                "7777-0000",
                "cliente@ejemplo.com",
                "San Salvador"));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
