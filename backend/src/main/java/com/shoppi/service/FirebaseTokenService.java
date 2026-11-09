package com.shoppi.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.stereotype.Service;

/**
 * Emisión de Firebase custom-tokens para el usuario autenticado.
 *
 * <p>El {@code uid} de Firebase es {@code user-{idUsuario}}, que coincide con
 * las reglas de Storage ({@code productos/{userId}/...}). Firebase es auxiliar:
 * el token solo habilita subir imágenes; no reemplaza el JWT de Shoppi.</p>
 */
@Service
public class FirebaseTokenService {

    private final FirebaseAuth firebaseAuth;

    public FirebaseTokenService(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    /** Genera un custom-token para el usuario autenticado. */
    public String crearCustomToken(Long usuarioId) {
        String uid = "user-" + usuarioId;
        try {
            return firebaseAuth.createCustomToken(uid);
        } catch (FirebaseAuthException e) {
            throw new IllegalStateException("No se pudo generar el custom-token de Firebase.", e);
        }
    }
}
