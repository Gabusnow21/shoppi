package com.shoppi.repository;

import com.shoppi.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a los clientes, siempre filtrado por el usuario propietario.
 */
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /** Lista los clientes de un usuario. */
    List<Cliente> findByUsuarioId(Long usuarioId);

    /** Busca un cliente solo si pertenece al usuario indicado. */
    Optional<Cliente> findByIdAndUsuarioId(Long id, Long usuarioId);
}
