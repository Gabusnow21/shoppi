package com.shoppi.service;

import com.shoppi.dto.dispositivo.DispositivoFcmRequest;
import com.shoppi.entity.DispositivoFcm;
import com.shoppi.entity.Usuario;
import com.shoppi.repository.DispositivoFcmRepository;
import com.shoppi.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro y borrado de tokens FCM del usuario autenticado.
 *
 * <p>El alcance es por usuario: un usuario solo puede registrar/borrar sus
 * propios tokens. Firebase es auxiliar: aquí solo se persiste el ruteo.</p>
 */
@Service
public class DispositivoService {

    private final DispositivoFcmRepository dispositivos;
    private final UsuarioRepository usuarios;

    public DispositivoService(DispositivoFcmRepository dispositivos, UsuarioRepository usuarios) {
        this.dispositivos = dispositivos;
        this.usuarios = usuarios;
    }

    /** Registra o actualiza el token del dispositivo (upsert por usuario+token). */
    @Transactional
    public void registrar(Long usuarioId, DispositivoFcmRequest request) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado."));

        String token = request.token().trim();
        String deviceId = limpiar(request.deviceId());
        String plataforma = (request.plataforma() == null || request.plataforma().isBlank())
                ? "ANDROID"
                : request.plataforma().trim();

        dispositivos.findByUsuarioIdAndFcmToken(usuarioId, token)
                .ifPresentOrElse(
                        existente -> existente.actualizar(deviceId, plataforma),
                        () -> dispositivos.save(new DispositivoFcm(usuario, token, deviceId, plataforma))
                );
    }

    /** Borra el token del usuario (logout / revoke). Idempotente. */
    @Transactional
    public void borrar(Long usuarioId, String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        dispositivos.deleteByUsuarioIdAndFcmToken(usuarioId, token.trim());
    }

    private String limpiar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
