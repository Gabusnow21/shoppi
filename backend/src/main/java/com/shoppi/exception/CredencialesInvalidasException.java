package com.shoppi.exception;

/**
 * Credenciales incorrectas durante el inicio de sesión.
 *
 * <p>Se traduce a HTTP 401 en {@link GlobalExceptionHandler}. El mensaje es
 * deliberadamente genérico para no revelar si el correo existe.</p>
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
