package com.shoppi.repository;

import com.shoppi.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Acceso a los usuarios persistidos en PostgreSQL.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Busca un usuario por su correo ya normalizado. */
    Optional<Usuario> findByEmail(String email);

    /** Comprueba si el correo ya está registrado. */
    boolean existsByEmail(String email);
}
