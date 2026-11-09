package com.shoppi.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.shoppi.entity.DispositivoFcm;
import com.shoppi.repository.DispositivoFcmRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Envío de push FCM al usuario autenticado.
 *
 * <p>Firebase es auxiliar: el push solo notifica (venta confirmada, stock bajo);
 * no transporta datos sensibles y no reemplaza la sincronización offline.</p>
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final DispositivoFcmRepository dispositivos;
    private final FirebaseMessaging messaging;

    public NotificacionService(DispositivoFcmRepository dispositivos, FirebaseMessaging messaging) {
        this.dispositivos = dispositivos;
        this.messaging = messaging;
    }

    /** Notifica una venta confirmada al usuario. */
    public void enviarVentaConfirmada(Long usuarioId, Long ventaId, BigDecimal total) {
        String cuerpo = "Venta #" + ventaId + " registrada por " + total + ".";
        enviar(usuarioId, "Venta confirmada", cuerpo,
                Map.of("type", "VENTA_CONFIRMADA", "ventaId", String.valueOf(ventaId)));
    }

    /** Notifica stock bajo al usuario. */
    public void enviarStockBajo(Long usuarioId, Long productoId, String nombreProducto, int stockActual, int stockMinimo) {
        String cuerpo = nombreProducto + " tiene " + stockActual
                + " unidades (mínimo " + stockMinimo + ").";
        enviar(usuarioId, "Stock bajo", cuerpo,
                Map.of("type", "STOCK_BAJO", "productoId", String.valueOf(productoId)));
    }

    /** Envía un push a todos los dispositivos del usuario. Nunca lanza. */
    public void enviar(Long usuarioId, String titulo, String cuerpo, Map<String, String> data) {
        List<String> tokens = dispositivos.findByUsuarioId(usuarioId).stream()
                .map(DispositivoFcm::getFcmToken)
                .toList();

        if (tokens.isEmpty()) {
            return;
        }

        List<Message> mensajes = tokens.stream()
                .map(token -> Message.builder()
                        .setToken(token)
                        .setNotification(Notification.builder()
                                .setTitle(titulo)
                                .setBody(cuerpo)
                                .build())
                        .putAllData(data == null ? Map.of() : data)
                        .build())
                .toList();

        try {
            BatchResponse respuesta = messaging.sendAll(mensajes);
            log.info("Push FCM a usuario {}: {} ok / {} fallos",
                    usuarioId, respuesta.getSuccessCount(), respuesta.getFailureCount());
        } catch (FirebaseMessagingException e) {
            log.warn("No se pudo enviar push FCM al usuario {}: {}", usuarioId, e.getMessage());
        }
    }
}
