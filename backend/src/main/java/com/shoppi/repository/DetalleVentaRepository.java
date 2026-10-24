package com.shoppi.repository;

import com.shoppi.entity.DetalleVenta;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Acceso a las líneas de venta (se gestionan en cascada desde {@code Venta}).
 */
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    /** Indica si un producto ya forma parte de alguna venta. */
    boolean existsByProductoId(Long productoId);

    /**
     * Top de productos por cantidad vendida y monto, filtrando por usuario y
     * rango de fechas. El {@link Pageable} aplica el límite solicitado.
     */
    @Query("""
            select d.producto.id, d.producto.nombre, sum(d.cantidad), sum(d.subtotal)
            from DetalleVenta d
            where d.venta.usuario.id = :usuarioId
              and d.venta.fecha >= :desde
              and d.venta.fecha <= :hasta
            group by d.producto.id, d.producto.nombre
            order by sum(d.cantidad) desc, sum(d.subtotal) desc
            """)
    List<Object[]> productosMasVendidos(
            @Param("usuarioId") Long usuarioId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable);
}
