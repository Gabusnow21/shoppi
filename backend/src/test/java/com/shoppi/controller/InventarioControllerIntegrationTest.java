package com.shoppi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoppi.dto.inventario.MovimientoRequest;
import com.shoppi.entity.Inventario;
import com.shoppi.entity.Producto;
import com.shoppi.entity.TipoMovimiento;
import com.shoppi.entity.Usuario;
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
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración de inventario: movimientos, actualización de stock,
 * filtros y aislamiento por usuario.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventarioControllerIntegrationTest {

    private static final String MOVIMIENTOS = "/api/inventario/movimiento";
    private static final String HISTORIAL = "/api/inventario";

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Usuario juan;
    private Usuario ana;
    private String tokenJuan;
    private String tokenAna;
    private Producto arroz;
    private Producto leche;

    @BeforeEach
    void setUp() {
        inventarios.deleteAll();
        productos.deleteAll();
        usuarios.deleteAll();

        juan = usuarios.saveAndFlush(new Usuario(
                "Juan Pérez", "juan@ejemplo.com", passwordEncoder.encode("contrasena123"), null));
        ana = usuarios.saveAndFlush(new Usuario(
                "Ana Gómez", "ana@ejemplo.com", passwordEncoder.encode("contrasena123"), null));

        tokenJuan = jwtService.generateToken(juan.getId(), juan.getEmail());
        tokenAna = jwtService.generateToken(ana.getId(), ana.getEmail());

        arroz = guardarProducto(juan, "Arroz", 50);
        leche = guardarProducto(ana, "Leche", 40);
    }

    @Test
    void registrarEntradaAumentaElStock() throws Exception {
        MovimientoRequest request = new MovimientoRequest(
                arroz.getId(), TipoMovimiento.ENTRADA, 20, "Compra a proveedor");

        mockMvc.perform(post(MOVIMIENTOS)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productoId").value(arroz.getId().intValue()))
                .andExpect(jsonPath("$.productoNombre").value("Arroz"))
                .andExpect(jsonPath("$.tipoMovimiento").value("ENTRADA"))
                .andExpect(jsonPath("$.cantidad").value(20))
                .andExpect(jsonPath("$.fecha").exists())
                .andExpect(jsonPath("$.descripcion").value("Compra a proveedor"));

        assertEquals(70, productos.findById(arroz.getId()).orElseThrow().getStockActual());
        assertEquals(1, inventarios.count());
    }

    @Test
    void registrarSalidaDisminuyeElStock() throws Exception {
        MovimientoRequest request = new MovimientoRequest(
                arroz.getId(), TipoMovimiento.SALIDA, 15, "Venta mostrador");

        mockMvc.perform(post(MOVIMIENTOS)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoMovimiento").value("SALIDA"))
                .andExpect(jsonPath("$.cantidad").value(15));

        assertEquals(35, productos.findById(arroz.getId()).orElseThrow().getStockActual());
    }

    @Test
    void salidaMayorAlStockDevuelve400YNoCambiaNada() throws Exception {
        MovimientoRequest request = new MovimientoRequest(
                arroz.getId(), TipoMovimiento.SALIDA, 100, null);

        mockMvc.perform(post(MOVIMIENTOS)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Stock insuficiente")));

        assertEquals(50, productos.findById(arroz.getId()).orElseThrow().getStockActual());
        assertEquals(0, inventarios.count());
    }

    @Test
    void movimientoSobreProductoAjenoDevuelve404() throws Exception {
        MovimientoRequest request = new MovimientoRequest(
                leche.getId(), TipoMovimiento.ENTRADA, 10, null);

        mockMvc.perform(post(MOVIMIENTOS)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Producto no encontrado."));

        assertEquals(40, productos.findById(leche.getId()).orElseThrow().getStockActual());
        assertEquals(0, inventarios.count());
    }

    @Test
    void listarSoloDevuelveMovimientosDelUsuarioAutenticado() throws Exception {
        guardarMovimiento(arroz, TipoMovimiento.ENTRADA, 10);
        guardarMovimiento(leche, TipoMovimiento.SALIDA, 5);

        mockMvc.perform(get(HISTORIAL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productoNombre").value("Arroz"));
    }

    @Test
    void listarFiltraPorProducto() throws Exception {
        Producto frijol = guardarProducto(juan, "Frijol", 30);
        guardarMovimiento(arroz, TipoMovimiento.ENTRADA, 10);
        guardarMovimiento(frijol, TipoMovimiento.ENTRADA, 5);

        mockMvc.perform(get(HISTORIAL)
                        .param("productoId", arroz.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productoNombre").value("Arroz"));
    }

    @Test
    void listarFiltraPorRangoDeFechas() throws Exception {
        LocalDate hoy = LocalDate.now();
        guardarMovimiento(arroz, TipoMovimiento.ENTRADA, 10);

        mockMvc.perform(get(HISTORIAL)
                        .param("fechaInicio", hoy.toString())
                        .param("fechaFin", hoy.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get(HISTORIAL)
                        .param("fechaInicio", hoy.plusDays(1).toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rangoDeFechasInvertidoDevuelve400() throws Exception {
        LocalDate hoy = LocalDate.now();

        mockMvc.perform(get(HISTORIAL)
                        .param("fechaInicio", hoy.plusDays(1).toString())
                        .param("fechaFin", hoy.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("fechaInicio no puede ser posterior a fechaFin."));
    }

    @Test
    void fechaConFormatoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(get(HISTORIAL)
                        .param("fechaInicio", "no-es-fecha")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parámetro inválido: fechaInicio"));
    }

    @Test
    void cantidadInvalidaDevuelve400() throws Exception {
        MovimientoRequest request = new MovimientoRequest(
                arroz.getId(), TipoMovimiento.ENTRADA, 0, null);

        mockMvc.perform(post(MOVIMIENTOS)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cantidad")));
    }

    @Test
    void tipoMovimientoInvalidoDevuelve400() throws Exception {
        String cuerpo = """
                {
                  "productoId": %d,
                  "tipoMovimiento": "AJUSTE",
                  "cantidad": 5
                }
                """.formatted(arroz.getId());

        mockMvc.perform(post(MOVIMIENTOS)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cuerpo de la petición inválido"));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get(HISTORIAL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autenticación requerida."));
    }

    private Producto guardarProducto(Usuario usuario, String nombre, int stock) {
        return productos.saveAndFlush(new Producto(
                usuario, nombre, "Descripción de " + nombre,
                new BigDecimal("1.50"), stock, 5, "Granos", null));
    }

    private Inventario guardarMovimiento(Producto producto, TipoMovimiento tipo, int cantidad) {
        return inventarios.saveAndFlush(
                new Inventario(producto, tipo, cantidad, "Movimiento de prueba"));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
