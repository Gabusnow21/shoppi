package com.shoppi.repository;

import com.shoppi.entity.DispositivoFcm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a los tokens FCM por usuario/dispositivo.
 *
 * <p>Soporta upsert (T-222), listado de tokens para push (T-224) y borrado
 * en logout (T-231). Firebase es auxiliar: aquí solo se persiste el ruteo.</p>
 */
public interface DispositivoFcmRepository extends JpaRepository<DispositivoFcm, Long> {

    /** Busca un token ya registrado para un usuario (upsert). */
    Optional<DispositivoFcm> findByUsuarioIdAndFcmToken(Long usuarioId, String fcmToken);

    /** Lista todos los tokens de un usuario (para enviar push). */
    List<DispositivoFcm> findByUsuarioId(Long usuarioId);

    /** Borra un token concreto de un usuario (logout / revoke). */
    void deleteByUsuarioIdAndFcmToken(Long usuarioId, String fcmToken);

    /** Resuelve el usuario propietario de un token (si existe). */
    Optional<DispositivoFcm> findByFcmToken(String fcmToken);
}
