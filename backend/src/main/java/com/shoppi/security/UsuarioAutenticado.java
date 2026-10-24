package com.shoppi.security;

import java.security.Principal;

/**
 * Usuario autenticado a partir de un JWT válido.
 *
 * <p>Se expone como principal de Spring Security para que los controladores
 * puedan recibirlo con {@code @AuthenticationPrincipal} sin volver a consultar
 * la base de datos.</p>
 */
public record UsuarioAutenticado(Long id, String email) implements Principal {

    @Override
    public String getName() {
        return email;
    }
}
