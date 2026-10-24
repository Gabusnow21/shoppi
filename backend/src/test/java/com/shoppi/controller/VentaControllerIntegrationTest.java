package com.shoppi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoppi.dto.venta.DetalleVentaRequest;
import com.shoppi.dto.venta.VentaRequest;
import com.shoppi.entity.Cliente;
import com.shoppi.entity.DetalleVenta;
import com.shoppi.entity.Inventario;
import com.shoppi.entity.Producto;
import com.shoppi.entity.TipoMovimiento;
import com.shoppi.entity.Usuario;
import com.shoppi.entity.Venta;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración de ventas: cálculo de totales, descuento de stock,
 * historial de inventario, filtros y aislamiento por usuario.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VentaControllerIntegrationTest {

    private static final String URL = "/api/ventas";

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
    private VentaRepository ventas;

    @Autowired
    private DetalleVentaRepository detalles;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Usuario juan;
    private Usuario ana;
    private String tokenJuan;
    private String tokenAna;
    private Producto arroz;
    private Producto frijol;
    private Producto leche;
    private Cliente maria;
    private Cliente carlos;

    @BeforeEach
    void setUp() {
        detalles.deleteAll();
        ventas.deleteAll();
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

        arroz = guardarProducto(juan, "Arroz", "1.50", 50);
        frijol = guardarProducto(juan, "Frijol", "3.00", 30);
        leche = guardarProducto(ana, "Leche", "0.90", 40);

        maria = guardarCliente(juan, "María García");
        carlos = guardarCliente(ana, "Carlos Ruiz");
    }

    @Test
    void crearVentaCalculaTotalesDescuentaStockYRegistraInventario() throws Exception {
        VentaRequest request = new VentaRequest(
                maria.getId(),
                "efectivo",
                List.of(
                        new DetalleVentaRequest(arroz.getId(), 2, new BigDecimal("1.50")),
                        new DetalleVentaRequest(frijol.getId(), 1, new BigDecimal("3.00"))));

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.fecha").exists())
                .andExpect(jsonPath("$.subtotal").value(6.00))
                .andExpect(jsonPath("$.impuestos").value(0.78))
                .andExpect(jsonPath("$.total").value(6.78))
                .andExpect(jsonPath("$.metodoPago").value("EFECTIVO"))
                .andExpect(jsonPath("$.clienteId").value(maria.getId().intValue()))
                .andExpect(jsonPath("$.detalles", hasSize(2)))
                .andExpect(jsonPath("$.detalles[0].productoId").value(arroz.getId().intValue()))
                .andExpect(jsonPath("$.detalles[0].cantidad").value(2))
                .andExpect(jsonPath("$.detalles[0].precioUnitario").value(1.50))
                .andExpect(jsonPath("$.detalles[0].subtotal").value(3.00));

        // T-065: la venta end-to-end descuenta el stock de cada producto.
        assertEquals(48, productos.findById(arroz.getId()).orElseThrow().getStockActual());
        assertEquals(29, productos.findById(frijol.getId()).orElseThrow().getStockActual());
        assertEquals(1, ventas.count());
        assertEquals(2, detalles.count());

        List<Inventario> movimientos = inventarios.findAll();
        assertEquals(2, movimientos.size());
        assertTrue(movimientos.stream().allMatch(m -> m.getTipoMovimiento() == TipoMovimiento.SALIDA));
        assertTrue(movimientos.stream()
                .allMatch(m -> m.getDescripcion().startsWith("Venta #")));
    }

    @Test
    void crearVentaSinClienteNiMetodoPagoAplicaValoresPorDefecto() throws Exception {
        VentaRequest request = new VentaRequest(
                null,
                null,
                List.of(new DetalleVentaRequest(arroz.getId(), 1, new BigDecimal("1.50"))));

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.metodoPago").value("EFECTIVO"))
                .andExpect(jsonPath("$.clienteId").doesNotExist());
    }

    @Test
    void crearVentaConClienteAjenoDevuelve404YNoPersisteNada() throws Exception {
        VentaRequest request = new VentaRequest(
                carlos.getId(),
                "EFECTIVO",
                List.of(new DetalleVentaRequest(arroz.getId(), 1, new BigDecimal("1.50"))));

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cliente no encontrado."));

        assertEquals(0, ventas.count());
        assertEquals(50, productos.findById(arroz.getId()).orElseThrow().getStockActual());
        assertEquals(0, inventarios.count());
    }

    @Test
    void crearVentaConProductoAjenoDevuelve404YNoPersisteNada() throws Exception {
        VentaRequest request = new VentaRequest(
                null,
                "EFECTIVO",
                List.of(new DetalleVentaRequest(leche.getId(), 1, new BigDecimal("0.90"))));

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Producto no encontrado."));

        assertEquals(0, ventas.count());
        assertEquals(40, productos.findById(leche.getId()).orElseThrow().getStockActual());
        assertEquals(0, inventarios.count());
    }

    @Test
    void crearVentaSinStockSuficienteDevuelve400YNoPersisteNada() throws Exception {
        VentaRequest request = new VentaRequest(
                null,
                "EFECTIVO",
                List.of(new DetalleVentaRequest(arroz.getId(), 100, new BigDecimal("1.50"))));

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Stock insuficiente")));

        assertEquals(50, productos.findById(arroz.getId()).orElseThrow().getStockActual());
        assertEquals(0, ventas.count());
        assertEquals(0, inventarios.count());
    }

    @Test
    void crearVentaConDetallesVaciosDevuelve400() throws Exception {
        VentaRequest request = new VentaRequest(null, "EFECTIVO", List.of());

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("detalles")));
    }

    @Test
    void crearVentaConCantidadInvalidaDevuelve400() throws Exception {
        VentaRequest request = new VentaRequest(
                null,
                "EFECTIVO",
                List.of(new DetalleVentaRequest(arroz.getId(), 0, new BigDecimal("1.50"))));

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cantidad")));
    }

    @Test
    void crearVentaConPrecioNegativoDevuelve400() throws Exception {
        VentaRequest request = new VentaRequest(
                null,
                "EFECTIVO",
                List.of(new DetalleVentaRequest(arroz.getId(), 1, new BigDecimal("-1.00"))));

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("precioUnitario")));
    }

    @Test
    void listarSoloDevuelveVentasDelUsuarioAutenticado() throws Exception {
        guardarVenta(juan, maria, arroz, 1, "1.50");
        guardarVenta(ana, carlos, leche, 2, "0.90");

        mockMvc.perform(get(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].detalles[0].productoId")
                        .value(arroz.getId().intValue()));
    }

    @Test
    void listarFiltraPorCliente() throws Exception {
        guardarVenta(juan, maria, arroz, 1, "1.50");
        guardarVenta(juan, null, frijol, 1, "3.00");

        mockMvc.perform(get(URL)
                        .param("clienteId", maria.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].clienteId").value(maria.getId().intValue()));
    }

    @Test
    void listarFiltraPorRangoDeFechas() throws Exception {
        LocalDate hoy = LocalDate.now();
        guardarVenta(juan, null, arroz, 1, "1.50");

        mockMvc.perform(get(URL)
                        .param("fechaInicio", hoy.toString())
                        .param("fechaFin", hoy.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get(URL)
                        .param("fechaInicio", hoy.plusDays(1).toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get(URL)
                        .param("fechaFin", hoy.minusDays(1).toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rangoDeFechasInvertidoDevuelve400() throws Exception {
        LocalDate hoy = LocalDate.now();

        mockMvc.perform(get(URL)
                        .param("fechaInicio", hoy.plusDays(1).toString())
                        .param("fechaFin", hoy.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("fechaInicio no puede ser posterior a fechaFin."));
    }

    @Test
    void fechaConFormatoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(get(URL)
                        .param("fechaInicio", "no-es-fecha")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parámetro inválido: fechaInicio"));
    }

    @Test
    void obtenerVentaPropiaDevuelveElDetalle() throws Exception {
        Venta venta = guardarVenta(juan, maria, arroz, 2, "1.50");

        mockMvc.perform(get(URL + "/" + venta.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(venta.getId().intValue()))
                .andExpect(jsonPath("$.subtotal").value(3.00))
                .andExpect(jsonPath("$.impuestos").value(0.39))
                .andExpect(jsonPath("$.total").value(3.39))
                .andExpect(jsonPath("$.detalles", hasSize(1)))
                .andExpect(jsonPath("$.detalles[0].cantidad").value(2));
    }

    @Test
    void obtenerVentaDeOtroUsuarioDevuelve404() throws Exception {
        Venta deAna = guardarVenta(ana, carlos, leche, 1, "0.90");

        mockMvc.perform(get(URL + "/" + deAna.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Venta no encontrada."));
    }

    @Test
    void eliminarClienteConservaLaVentaSinCliente() throws Exception {
        Venta venta = guardarVenta(juan, maria, arroz, 1, "1.50");

        mockMvc.perform(delete("/api/clientes/" + maria.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(URL + "/" + venta.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clienteId").doesNotExist());
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autenticación requerida."));
    }

    private Producto guardarProducto(Usuario usuario, String nombre, String precio, int stock) {
        return productos.saveAndFlush(new Producto(
                usuario, nombre, "Descripción de " + nombre,
                new BigDecimal(precio), stock, 5, "Granos", null));
    }

    private Cliente guardarCliente(Usuario usuario, String nombre) {
        return clientes.saveAndFlush(new Cliente(
                usuario, nombre, "7777-0000", "cliente@ejemplo.com", "San Salvador"));
    }

    private Venta guardarVenta(
            Usuario usuario,
            Cliente cliente,
            Producto producto,
            int cantidad,
            String precio) {

        BigDecimal precioUnitario = new BigDecimal(precio);
        BigDecimal importe = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
        BigDecimal impuestos = importe.multiply(new BigDecimal("0.13"))
                .setScale(2, java.math.RoundingMode.HALF_UP);

        Venta venta = new Venta(
                usuario,
                cliente,
                importe,
                impuestos,
                importe.add(impuestos),
                "EFECTIVO");
        venta.agregarDetalle(new DetalleVenta(producto, cantidad, precioUnitario, importe));
        return ventas.saveAndFlush(venta);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
