package com.shoppi.service;

import com.shoppi.dto.inventario.MovimientoRequest;
import com.shoppi.dto.inventario.MovimientoResponse;
import com.shoppi.entity.Inventario;
import com.shoppi.entity.Producto;
import com.shoppi.repository.InventarioRepository;
import com.shoppi.repository.ProductoRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Registro y consulta de movimientos de inventario.
 *
 * <p>Registrar un movimiento actualiza el stock del producto en la misma
 * transacción: si algo falla, no queda ni el movimiento ni el cambio de stock.
 * El alcance por usuario se valida a través del producto.</p>
 */
@Service
public class InventarioService {

    private final InventarioRepository inventarios;
    private final ProductoRepository productos;

    public InventarioService(InventarioRepository inventarios, ProductoRepository productos) {
        this.inventarios = inventarios;
        this.productos = productos;
    }

    /** Registra una entrada o salida y actualiza el stock del producto. */
    @Transactional
    public MovimientoResponse registrar(Long usuarioId, MovimientoRequest request) {
        Producto producto = productos.findByIdAndUsuarioId(request.productoId(), usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado."));

        switch (request.tipoMovimiento()) {
            case ENTRADA -> producto.registrarEntrada(request.cantidad());
            case SALIDA -> producto.registrarSalida(request.cantidad());
        }

        Inventario movimiento = inventarios.save(new Inventario(
                producto,
                request.tipoMovimiento(),
                request.cantidad(),
                limpiar(request.descripcion())));

        return respuesta(movimiento);
    }

    /**
     * Lista movimientos del usuario, con filtros opcionales por producto y rango
     * de fechas (ambas inclusive, en formato {@code YYYY-MM-DD}).
     */
    @Transactional(readOnly = true)
    public List<MovimientoResponse> listar(
            Long usuarioId,
            Long productoId,
            LocalDate fechaInicio,
            LocalDate fechaFin) {

        if (fechaInicio != null && fechaFin != null && fechaInicio.isAfter(fechaFin)) {
            throw new IllegalArgumentException("fechaInicio no puede ser posterior a fechaFin.");
        }

        LocalDateTime desde = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
        LocalDateTime hasta = fechaFin != null ? fechaFin.atTime(LocalTime.MAX) : null;

        return inventarios.findAll(filtro(usuarioId, productoId, desde, hasta)).stream()
                .map(this::respuesta)
                .toList();
    }

    private Specification<Inventario> filtro(
            Long usuarioId,
            Long productoId,
            LocalDateTime desde,
            LocalDateTime hasta) {

        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            predicados.add(cb.equal(root.get("producto").get("usuario").get("id"), usuarioId));

            if (productoId != null) {
                predicados.add(cb.equal(root.get("producto").get("id"), productoId));
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

    private MovimientoResponse respuesta(Inventario movimiento) {
        return new MovimientoResponse(
                movimiento.getId(),
                movimiento.getProducto().getId(),
                movimiento.getProducto().getNombre(),
                movimiento.getTipoMovimiento(),
                movimiento.getCantidad(),
                movimiento.getFecha(),
                movimiento.getDescripcion());
    }

    private String limpiar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
