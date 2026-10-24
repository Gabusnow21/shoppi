package com.shoppi.controller;

import com.shoppi.dto.auth.AuthResponse;
import com.shoppi.dto.auth.LoginRequest;
import com.shoppi.dto.auth.RegisterRequest;
import com.shoppi.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints públicos de autenticación.
 *
 * <p>Coinciden con {@code docs/api_specification.md}:
 * {@code POST /api/auth/register} y {@code POST /api/auth/login}. Ambos
 * devuelven el mismo {@link AuthResponse} con el JWT.</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Crea la cuenta y devuelve el JWT para usarla de inmediato. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registrar(@Valid @RequestBody RegisterRequest request) {
        return authService.registrar(request);
    }

    /** Valida las credenciales y devuelve un JWT nuevo. */
    @PostMapping("/login")
    public AuthResponse iniciarSesion(@Valid @RequestBody LoginRequest request) {
        return authService.iniciarSesion(request);
    }
}
