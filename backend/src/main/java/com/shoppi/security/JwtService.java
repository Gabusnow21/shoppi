package com.shoppi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Servicio para generar y validar los JWT propios de Shoppi (HS256).
 * El secreto se inyecta por la propiedad {@code shoppi.jwt.secret} (env {@code JWT_SECRET}).
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(
            @Value("${shoppi.jwt.secret}") String secret,
            @Value("${shoppi.jwt.expiration-minutes}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    /** Genera un token para el usuario autenticado. */
    public String generateToken(Long usuarioId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(usuarioId))
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    /** Devuelve el id de usuario contenido en el token (claim {@code sub}). */
    public Long extractUsuarioId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    /** Devuelve el email contenido en el token. */
    public String extractEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }

    /** Indica si el token tiene firma válida y no ha expirado. */
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
