package com.shoppi.controller;

import com.shoppi.dto.dispositivo.DispositivoFcmRequest;
import com.shoppi.security.UsuarioAutenticado;
import com.shoppi.service.DispositivoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Registro y borrado de tokens FCM del usuario autenticado.
 *
 * <p>Requiere {@code Authorization: Bearer <token>}; el id del usuario sale del
 * JWT y nunca del cuerpo de la petición.</p>
 */
@RestController
@RequestMapping("/api/dispositivos")
public class DispositivoController {

    private final DispositivoService dispositivoService;

    public DispositivoController(DispositivoService dispositivoService) {
        this.dispositivoService = dispositivoService;
    }

    @PostMapping("/fcm-token")
    public Map<String, Boolean> registrar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody DispositivoFcmRequest request) {
        dispositivoService.registrar(usuario.id(), request);
        return Map.of("ok", true);
    }

    @DeleteMapping("/fcm-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody DispositivoFcmRequest request) {
        dispositivoService.borrar(usuario.id(), request.token());
    }
}
