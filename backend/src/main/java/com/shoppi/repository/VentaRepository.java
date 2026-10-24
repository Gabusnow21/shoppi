package com.shoppi.repository;

import com.shoppi.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a las ventas, siempre filtrado por el usuario propietario.
 */
public interface VentaRepository
        extends JpaRepository<Venta, Long>, JpaSpecificationExecutor<Venta> {

    /** Busca una venta solo si pertenece al usuario indicado. */
    Optional<Venta> findByIdAndUsuarioId(Long id, Long usuarioId);

    /**
     * Devuelve fecha y total de las ventas del rango, solo con las columnas
     * necesarias para armar el reporte diario.
     */
    @Query("""
            select v.fecha, v.total
            from Venta v
            where v.usuario.id = :usuarioId
              and v.fecha >= :desde
              and v.fecha <= :hasta
            order by v.fecha asc
            """)
    List<Object[]> fechasYMontos(
            @Param("usuarioId") Long usuarioId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
