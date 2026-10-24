package com.shoppi.controller;

import com.shoppi.entity.Cliente;
import com.shoppi.entity.DetalleVenta;
import com.shoppi.entity.Producto;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración de reportes: ventas por período, top de productos y
 * estado de inventario, siempre aislados por usuario.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReporteControllerIntegrationTest {

    private static final String VENTAS = "/api/reportes/ventas";
    private static final String MAS_VENDIDOS = "/api/reportes/productos-mas-vendidos";
    private static final String INVENTARIO = "/api/reportes/inventario";

    @Autowired
    private MockMvc mockMvc;

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
    private JdbcTemplate jdbc;

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
    void reporteVentasAgrupaPorDiaYRellenaDiasSinVentas() throws Exception {
        LocalDate hoy = LocalDate.now();
        LocalDate ayer = hoy.minusDays(1);

        guardarVenta(juan, maria, arroz, 1, "1.50", ayer);   // total 1.70
        guardarVenta(juan, null, arroz, 2, "1.50", hoy);     // total 3.39
        guardarVenta(juan, null, frijol, 1, "3.00", hoy);    // total 3.39
        guardarVenta(ana, carlos, leche, 5, "0.90", hoy);    // de otro usuario

        mockMvc.perform(get(VENTAS)
                        .param("periodo", "diario")
                        .param("fechaInicio", ayer.toString())
                        .param("fechaFin", hoy.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo").value("diario"))
                .andExpect(jsonPath("$.fechaInicio").value(ayer.toString()))
                .andExpect(jsonPath("$.fechaFin").value(hoy.toString()))
                .andExpect(jsonPath("$.totalVentas").value(3))
                .andExpect(jsonPath("$.montoTotal").value(8.48))
                .andExpect(jsonPath("$.ventasPorDia", hasSize(2)))
                .andExpect(jsonPath("$.ventasPorDia[0].fecha").value(ayer.toString()))
                .andExpect(jsonPath("$.ventasPorDia[0].cantidad").value(1))
                .andExpect(jsonPath("$.ventasPorDia[0].monto").value(1.70))
                .andExpect(jsonPath("$.ventasPorDia[1].fecha").value(hoy.toString()))
                .andExpect(jsonPath("$.ventasPorDia[1].cantidad").value(2))
                .andExpect(jsonPath("$.ventasPorDia[1].monto").value(6.78));
    }

    @Test
    void reporteVentasSinFechasUsaElDiaActual() throws Exception {
        LocalDate hoy = LocalDate.now();
        guardarVenta(juan, null, arroz, 1, "1.50", hoy);

        mockMvc.perform(get(VENTAS)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo").value("diario"))
                .andExpect(jsonPath("$.fechaInicio").value(hoy.toString()))
                .andExpect(jsonPath("$.fechaFin").value(hoy.toString()))
                .andExpect(jsonPath("$.totalVentas").value(1));
    }

    @Test
    void reporteVentasSemanalUsaLunesComoInicio() throws Exception {
        LocalDate hoy = LocalDate.now();
        LocalDate lunes = hoy.with(DayOfWeek.MONDAY);

        mockMvc.perform(get(VENTAS)
                        .param("periodo", "semanal")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo").value("semanal"))
                .andExpect(jsonPath("$.fechaInicio").value(lunes.toString()))
                .andExpect(jsonPath("$.fechaFin").value(hoy.toString()));
    }

    @Test
    void reporteVentasMensualUsaElPrimerDiaDelMes() throws Exception {
        LocalDate hoy = LocalDate.now();
        LocalDate primero = hoy.withDayOfMonth(1);

        mockMvc.perform(get(VENTAS)
                        .param("periodo", "mensual")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo").value("mensual"))
                .andExpect(jsonPath("$.fechaInicio").value(primero.toString()))
                .andExpect(jsonPath("$.fechaFin").value(hoy.toString()));
    }

    @Test
    void reporteVentasConPeriodoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(get(VENTAS)
                        .param("periodo", "anual")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("periodo debe ser diario, semanal o mensual."));
    }

    @Test
    void reporteVentasConRangoInvertidoDevuelve400() throws Exception {
        LocalDate hoy = LocalDate.now();

        mockMvc.perform(get(VENTAS)
                        .param("fechaInicio", hoy.plusDays(1).toString())
                        .param("fechaFin", hoy.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("fechaInicio no puede ser posterior a fechaFin."));
    }

    @Test
    void reporteVentasConFechaInvalidaDevuelve400() throws Exception {
        mockMvc.perform(get(VENTAS)
                        .param("fechaInicio", "no-es-fecha")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parámetro inválido: fechaInicio"));
    }

    @Test
    void productosMasVendidosOrdenaPorCantidadYDevuelveMontos() throws Exception {
        LocalDate hoy = LocalDate.now();
        guardarVenta(juan, null, arroz, 5, "1.50", hoy);   // 7.50
        guardarVenta(juan, null, frijol, 2, "3.00", hoy);  // 6.00

        mockMvc.perform(get(MAS_VENDIDOS)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].productoId").value(arroz.getId().intValue()))
                .andExpect(jsonPath("$[0].nombre").value("Arroz"))
                .andExpect(jsonPath("$[0].cantidadVendida").value(5))
                .andExpect(jsonPath("$[0].montoTotal").value(7.50))
                .andExpect(jsonPath("$[1].productoId").value(frijol.getId().intValue()))
                .andExpect(jsonPath("$[1].cantidadVendida").value(2))
                .andExpect(jsonPath("$[1].montoTotal").value(6.00));
    }

    @Test
    void productosMasVendidosRespetaElLimite() throws Exception {
        LocalDate hoy = LocalDate.now();
        guardarVenta(juan, null, arroz, 5, "1.50", hoy);
        guardarVenta(juan, null, frijol, 2, "3.00", hoy);

        mockMvc.perform(get(MAS_VENDIDOS)
                        .param("limite", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Arroz"));
    }

    @Test
    void productosMasVendidosFiltraPorFecha() throws Exception {
        LocalDate hoy = LocalDate.now();
        LocalDate ayer = hoy.minusDays(1);
        guardarVenta(juan, null, arroz, 1, "1.50", hoy);
        guardarVenta(juan, null, frijol, 5, "3.00", ayer);

        mockMvc.perform(get(MAS_VENDIDOS)
                        .param("fechaInicio", hoy.toString())
                        .param("fechaFin", hoy.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Arroz"));
    }

    @Test
    void productosMasVendidosSoloIncluyeAlUsuarioAutenticado() throws Exception {
        LocalDate hoy = LocalDate.now();
        guardarVenta(juan, null, arroz, 1, "1.50", hoy);
        guardarVenta(ana, carlos, leche, 10, "0.90", hoy);

        mockMvc.perform(get(MAS_VENDIDOS)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Arroz"));
    }

    @Test
    void productosMasVendidosConLimiteInvalidoDevuelve400() throws Exception {
        mockMvc.perform(get(MAS_VENDIDOS)
                        .param("limite", "0")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("limite debe estar entre 1 y 100."));
    }

    @Test
    void reporteInventarioCalculaValoresYMarcaStockBajo() throws Exception {
        arroz.registrarSalida(47); // stock 3 <= mínimo 5
        productos.saveAndFlush(arroz);

        mockMvc.perform(get(INVENTARIO)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProductos").value(2))
                .andExpect(jsonPath("$.productosStockBajo").value(1))
                .andExpect(jsonPath("$.valorInventario").value(94.50))
                .andExpect(jsonPath("$.productos", hasSize(2)))
                .andExpect(jsonPath("$.productos[0].nombre").value("Arroz"))
                .andExpect(jsonPath("$.productos[0].stockActual").value(3))
                .andExpect(jsonPath("$.productos[0].stockBajo").value(true))
                .andExpect(jsonPath("$.productos[0].valorStock").value(4.50))
                .andExpect(jsonPath("$.productos[1].nombre").value("Frijol"))
                .andExpect(jsonPath("$.productos[1].stockBajo").value(false))
                .andExpect(jsonPath("$.productos[1].valorStock").value(90.00));
    }

    @Test
    void reporteInventarioSoloIncluyeProductosDelUsuario() throws Exception {
        mockMvc.perform(get(INVENTARIO)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenJuan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProductos").value(2))
                .andExpect(jsonPath("$.productos[*].nombre")
                        .value(org.hamcrest.Matchers.containsInAnyOrder("Arroz", "Frijol")));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get(VENTAS))
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
            String precio,
            LocalDate fecha) {

        BigDecimal precioUnitario = new BigDecimal(precio);
        BigDecimal importe = precioUnitario.multiply(BigDecimal.valueOf(cantidad))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal impuestos = importe.multiply(new BigDecimal("0.13"))
                .setScale(2, RoundingMode.HALF_UP);

        Venta venta = new Venta(
                usuario,
                cliente,
                importe,
                impuestos,
                importe.add(impuestos),
                "EFECTIVO");
        venta.agregarDetalle(new DetalleVenta(producto, cantidad, precioUnitario, importe));

        Venta guardada = ventas.saveAndFlush(venta);
        jdbc.update("update ventas set fecha = ? where id = ?",
                Timestamp.valueOf(fecha.atTime(12, 0)), guardada.getId());
        return guardada;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
