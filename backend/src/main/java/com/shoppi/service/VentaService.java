package com.shoppi.service;

import com.shoppi.dto.venta.DetalleVentaRequest;
import com.shoppi.dto.venta.DetalleVentaResponse;
import com.shoppi.dto.venta.VentaRequest;
import com.shoppi.dto.venta.VentaResponse;
import com.shoppi.entity.Cliente;
import com.shoppi.entity.DetalleVenta;
import com.shoppi.entity.Inventario;
import com.shoppi.entity.Producto;
import com.shoppi.entity.TipoMovimiento;
import com.shoppi.entity.Usuario;
import com.shoppi.entity.Venta;
import com.shoppi.repository.ClienteRepository;
import com.shoppi.repository.InventarioRepository;
import com.shoppi.repository.ProductoRepository;
import com.shoppi.repository.UsuarioRepository;
import com.shoppi.repository.VentaRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Registro y consulta de ventas.
 *
 * <p>Crear una venta calcula subtotal, IVA (13 %) y total, descuenta el stock de
 * cada producto y registra el movimiento de inventario, todo en la misma
 * transacción. Si un producto no tiene stock suficiente, no se guarda nada.</p>
 */
@Service
public class VentaService {

    /** IVA de El Salvador; debe coincidir con {@code VentaCalculator} de Android. */
    private static final BigDecimal TASA_IMPUESTO = new BigDecimal("0.13");
    private static final String METODO_PAGO_POR_DEFECTO = "EFECTIVO";

    private final VentaRepository ventas;
    private final ClienteRepository clientes;
    private final ProductoRepository productos;
    private final InventarioRepository inventarios;
    private final UsuarioRepository usuarios;

    public VentaService(
            VentaRepository ventas,
            ClienteRepository clientes,
            ProductoRepository productos,
            InventarioRepository inventarios,
            UsuarioRepository usuarios) {
        this.ventas = ventas;
        this.clientes = clientes;
        this.productos = productos;
        this.inventarios = inventarios;
        this.usuarios = usuarios;
    }

    /** Registra una venta, descuenta stock y deja el historial de inventario. */
    @Transactional
    public VentaResponse crear(Long usuarioId, VentaRequest request) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado."));

        Cliente cliente = null;
        if (request.clienteId() != null) {
            cliente = clientes.findByIdAndUsuarioId(request.clienteId(), usuarioId)
                    .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado."));
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<LineaVenta> lineas = new ArrayList<>();

        for (DetalleVentaRequest detalle : request.detalles()) {
            Producto producto = productos.findByIdAndUsuarioId(detalle.productoId(), usuarioId)
                    .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado."));

            // Valida stock y descuenta sobre la entidad gestionada; si otra línea
            // falla, la transacción revierte también estos cambios.
            producto.registrarSalida(detalle.cantidad());

            BigDecimal importe = detalle.precioUnitario()
                    .multiply(BigDecimal.valueOf(detalle.cantidad()))
                    .setScale(2, RoundingMode.HALF_UP);

            subtotal = subtotal.add(importe);
            lineas.add(new LineaVenta(producto, detalle.cantidad(), detalle.precioUnitario(), importe));
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal impuestos = subtotal.multiply(TASA_IMPUESTO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(impuestos);

        Venta venta = new Venta(
                usuario,
                cliente,
                subtotal,
                impuestos,
                total,
                normalizarMetodoPago(request.metodoPago()));

        for (LineaVenta linea : lineas) {
            venta.agregarDetalle(new DetalleVenta(
                    linea.producto(),
                    linea.cantidad(),
                    linea.precioUnitario(),
                    linea.importe()));
        }

        Venta guardada = ventas.save(venta);

        for (LineaVenta linea : lineas) {
            inventarios.save(new Inventario(
                    linea.producto(),
                    TipoMovimiento.SALIDA,
                    linea.cantidad(),
                    "Venta #" + guardada.getId()));
        }

        return respuesta(guardada);
    }

    /**
     * Lista ventas del usuario, con filtros opcionales por cliente y rango de
     * fechas (ambas inclusive, en formato {@code YYYY-MM-DD}).
     */
    @Transactional(readOnly = true)
    public List<VentaResponse> listar(
            Long usuarioId,
            Long clienteId,
            LocalDate fechaInicio,
            LocalDate fechaFin) {

        if (fechaInicio != null && fechaFin != null && fechaInicio.isAfter(fechaFin)) {
            throw new IllegalArgumentException("fechaInicio no puede ser posterior a fechaFin.");
        }

        LocalDateTime desde = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
        LocalDateTime hasta = fechaFin != null ? fechaFin.atTime(LocalTime.MAX) : null;

        return ventas.findAll(filtro(usuarioId, clienteId, desde, hasta)).stream()
                .map(this::respuesta)
                .toList();
    }

    /** Obtiene una venta propia con su detalle o lanza 404. */
    @Transactional(readOnly = true)
    public VentaResponse obtener(Long usuarioId, Long ventaId) {
        return respuesta(ventas.findByIdAndUsuarioId(ventaId, usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada.")));
    }

    private Specification<Venta> filtro(
            Long usuarioId,
            Long clienteId,
            LocalDateTime desde,
            LocalDateTime hasta) {

        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            predicados.add(cb.equal(root.get("usuario").get("id"), usuarioId));

            if (clienteId != null) {
                predicados.add(cb.equal(root.get("cliente").get("id"), clienteId));
            }
            if (desde != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("fecha"), desde));
            }
            if (hasta != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("fecha"), hasta));
            }

            query.orderBy(cb.desc(root.get("fecha")), cb.desc(root.get("id")));
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    private VentaResponse respuesta(Venta venta) {
        List<DetalleVentaResponse> detalles = venta.getDetalles().stream()
                .map(detalle -> new DetalleVentaResponse(
                        detalle.getProducto().getId(),
                        detalle.getCantidad(),
                        detalle.getPrecioUnitario(),
                        detalle.getSubtotal()))
                .toList();

        return new VentaResponse(
                venta.getId(),
                venta.getFecha(),
                venta.getSubtotal(),
                venta.getImpuestos(),
                venta.getTotal(),
                venta.getMetodoPago(),
                venta.getCliente() != null ? venta.getCliente().getId() : null,
                detalles);
    }

    private String normalizarMetodoPago(String valor) {
        if (valor == null || valor.isBlank()) {
            return METODO_PAGO_POR_DEFECTO;
        }
        return valor.trim().toUpperCase(Locale.ROOT);
    }

    /** Datos de una línea ya validada, antes de construir la venta. */
    private record LineaVenta(
            Producto producto,
            int cantidad,
            BigDecimal precioUnitario,
            BigDecimal importe) {
    }
}
