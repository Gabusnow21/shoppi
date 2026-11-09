package com.shoppi.dto.dispositivo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /api/dispositivos/fcm-token} y
 * {@code DELETE /api/dispositivos/fcm-token}.
 *
 * <p>{@code token} es obligatorio; {@code deviceId} y {@code plataforma} son
 * opcionales (plataforma por defecto {@code ANDROID}).</p>
 */
public record DispositivoFcmRequest(

        @NotBlank(message = "es obligatorio")
        @Size(max = 255, message = "no debe superar 255 caracteres")
        String token,

        @Size(max = 100, message = "no debe superar 100 caracteres")
        String deviceId,

        @Size(max = 20, message = "no debe superar 20 caracteres")
        String plataforma
) {
}
