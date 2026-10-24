package com.shoppi.repository;

import com.shoppi.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a los productos, siempre filtrado por el usuario propietario.
 */
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /** Lista los productos de un usuario. */
    List<Producto> findByUsuarioId(Long usuarioId);

    /** Busca un producto solo si pertenece al usuario indicado. */
    Optional<Producto> findByIdAndUsuarioId(Long id, Long usuarioId);
}
