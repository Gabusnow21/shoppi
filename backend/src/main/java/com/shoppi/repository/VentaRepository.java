package com.shoppi.repository;

import com.shoppi.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * Acceso a las ventas, siempre filtrado por el usuario propietario.
 */
public interface VentaRepository
        extends JpaRepository<Venta, Long>, JpaSpecificationExecutor<Venta> {

    /** Busca una venta solo si pertenece al usuario indicado. */
    Optional<Venta> findByIdAndUsuarioId(Long id, Long usuarioId);
}
