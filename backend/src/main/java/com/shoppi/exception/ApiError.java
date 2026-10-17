package com.shoppi.exception;

import java.time.Instant;

/**
 * Formato estándar de error de la API.
 *
 * <pre>
 * {
 *   "timestamp": "2026-01-01T12:00:00Z",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "email: no debe estar vacío",
 *   "path": "/api/auth/register"
 * }
 * </pre>
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
