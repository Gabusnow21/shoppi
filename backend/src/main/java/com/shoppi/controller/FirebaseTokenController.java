package com.shoppi.controller;

import com.shoppi.security.UsuarioAutenticado;
import com.shoppi.service.FirebaseTokenService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Emisión de Firebase custom-tokens a partir del JWT propio.
 *
 * <p>Requiere {@code Authorization: Bearer <token>}; el id del usuario sale del
 * JWT y nunca del cuerpo de la petición.</p>
 */
@RestController
@RequestMapping("/api/firebase")
public class FirebaseTokenController {

    private final FirebaseTokenService firebaseTokenService;

    public FirebaseTokenController(FirebaseTokenService firebaseTokenService) {
        this.firebaseTokenService = firebaseTokenService;
    }

    @PostMapping("/custom-token")
    public Map<String, String> crearCustomToken(
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return Map.of("customToken", firebaseTokenService.crearCustomToken(usuario.id()));
    }
}
