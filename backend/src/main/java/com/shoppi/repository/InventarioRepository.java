package com.shoppi.repository;

import com.shoppi.entity.Inventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Acceso al historial de inventario.
 *
 * <p>Los filtros opcionales por producto y fecha se construyen con
 * {@link JpaSpecificationExecutor} para mantener el alcance por usuario.</p>
 */
public interface InventarioRepository
        extends JpaRepository<Inventario, Long>, JpaSpecificationExecutor<Inventario> {
}
