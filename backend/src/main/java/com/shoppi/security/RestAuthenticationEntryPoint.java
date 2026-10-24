package com.shoppi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoppi.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.time.Instant;

/**
 * Respuesta JSON estándar (HTTP 401) cuando una petición protegida llega sin
 * autenticación válida.
 *
 * <p>Se usa el mismo formato {@link ApiError} que el resto de la API para que el
 * cliente Android pueda procesar el error de forma uniforme.</p>
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String MENSAJE = "Autenticación requerida.";

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                MENSAJE,
                request.getRequestURI());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), error);
    }
}
