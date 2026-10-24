package com.shoppi.repository;

import com.shoppi.entity.DetalleVenta;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a las líneas de venta (se gestionan en cascada desde {@code Venta}).
 */
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    /** Indica si un producto ya forma parte de alguna venta. */
    boolean existsByProductoId(Long productoId);
}
