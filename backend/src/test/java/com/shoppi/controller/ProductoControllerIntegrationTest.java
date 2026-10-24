package com.shoppi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoppi.dto.producto.ProductoRequest;
import com.shoppi.entity.Producto;
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

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración del CRUD de productos, incluyendo el aislamiento por
 * usuario autenticado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductoControllerIntegrationTest {

    private static final String URL = "/api/productos";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private ProductoRepository productos;

    @Autowired
    private ClienteRepository clientes;

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
        clientes.deleteAll();
        productos.deleteAll();
        usuarios.deleteAll();

        juan = usuarios.saveAndFlush(new Usuario(
                "Juan Pérez", "juan@ejemplo.com", passwordEncoder.encode("contrasena123"), null));
        ana = usuarios.saveAndFlush(new Usuario(
                "Ana Gómez", "ana@ejemplo.com", passwordEncoder.encode("contrasena123"), null));

        tokenJuan = jwtService.generateToken(juan.getId(), juan.getEmail());
        tokenAna = jwtService.generateToken(ana.getId(), ana.getEmail());
    }

    @Test
    void crearProductoDevuelve201YLoPersiste() throws Exception {
        ProductoRequest request = new ProductoRequest(
                "Arroz", "Arroz premium 1lb", new BigDecimal("1.50"),
                50, 10, "Granos", null);

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Arroz"))
                .andExpect(jsonPath("$.descripcion").value("Arroz premium 1lb"))
                .andExpect(jsonPath("$.precio").value(1.50))
                .andExpect(jsonPath("$.stockActual").value(50))
                .andExpect(jsonPath("$.stockMinimo").value(10))
                .andExpect(jsonPath("$.categoria").value("Granos"));

        assertEquals(1, productos.findByUsuarioId(juan.getId()).size());
        assertEquals("Arroz", productos.findByUsuarioId(juan.getId()).get(0).getNombre());
    }

    @Test
    void crearProductoSinStockAplicaValoresPorDefecto() throws Exception {
        ProductoRequest request = new ProductoRequest(
                "Leche", null, new BigDecimal("0.90"), null, null, null, null);

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockActual").value(0))
                .andExpect(jsonPath("$.stockMinimo").value(5));
    }

    @Test
    void listarSoloDevuelveProductosDelUsuarioAutenticado() throws Exception {
        guardarProducto(juan, "Arroz");
        guardarProducto(ana, "Leche");

        mockMvc.perform(get(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Arroz"));
    }

    @Test
    void obtenerProductoDeOtroUsuarioDevuelve404() throws Exception {
        Producto deAna = guardarProducto(ana, "Leche");

        mockMvc.perform(get(URL + "/" + deAna.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Producto no encontrado."));
    }

    @Test
    void actualizarProductoPropioDevuelve200() throws Exception {
        Producto arroz = guardarProducto(juan, "Arroz");
        ProductoRequest cambios = new ProductoRequest(
                "Arroz integral", "Saco de 2lb", new BigDecimal("2.25"),
                30, 8, "Granos", "https://firebasestorage.googleapis.com/v0/b/shoppi/o/arroz.jpg");

        mockMvc.perform(put(URL + "/" + arroz.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cambios)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Arroz integral"))
                .andExpect(jsonPath("$.precio").value(2.25))
                .andExpect(jsonPath("$.stockActual").value(30));

        Producto guardado = productos.findById(arroz.getId()).orElseThrow();
        assertEquals("Arroz integral", guardado.getNombre());
        assertEquals(0, new BigDecimal("2.25").compareTo(guardado.getPrecio()));
    }

    @Test
    void actualizarProductoDeOtroUsuarioDevuelve404YNoLoModifica() throws Exception {
        Producto deAna = guardarProducto(ana, "Leche");
        ProductoRequest cambios = new ProductoRequest(
                "Leche editada", null, new BigDecimal("1.10"), 1, 1, null, null);

        mockMvc.perform(put(URL + "/" + deAna.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cambios)))
                .andExpect(status().isNotFound());

        assertEquals("Leche", productos.findById(deAna.getId()).orElseThrow().getNombre());
    }

    @Test
    void eliminarProductoPropioDevuelve204() throws Exception {
        Producto arroz = guardarProducto(juan, "Arroz");

        mockMvc.perform(delete(URL + "/" + arroz.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isNoContent());

        assertTrue(productos.findById(arroz.getId()).isEmpty());
    }

    @Test
    void eliminarProductoDeOtroUsuarioDevuelve404YNoLoBorra() throws Exception {
        Producto deAna = guardarProducto(ana, "Leche");

        mockMvc.perform(delete(URL + "/" + deAna.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isNotFound());

        assertFalse(productos.findById(deAna.getId()).isEmpty());
    }

    @Test
    void crearConDatosInvalidosDevuelve400ConDetalleDeCampos() throws Exception {
        String cuerpo = """
                {
                  "nombre": "   ",
                  "precio": -1,
                  "stockActual": -5,
                  "stockMinimo": -1
                }
                """;

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("nombre")))
                .andExpect(jsonPath("$.message", containsString("precio")))
                .andExpect(jsonPath("$.message", containsString("stockActual")))
                .andExpect(jsonPath("$.message", containsString("stockMinimo")));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autenticación requerida."));
    }

    private Producto guardarProducto(Usuario usuario, String nombre) {
        return productos.saveAndFlush(new Producto(
                usuario,
                nombre,
                "Descripción de " + nombre,
                new BigDecimal("1.50"),
                50,
                10,
                "Granos",
                null));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
