
package com.shoppi.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.shoppi.entity.Cliente;
import com.shoppi.entity.DetalleVenta;
import com.shoppi.entity.Inventario;
import com.shoppi.entity.Producto;
import com.shoppi.entity.Venta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Espejo de lectura de PostgreSQL hacia Firestore.
 *
 * <p>PostgreSQL sigue siendo la fuente de verdad: cada escritura se replica en
 * {@code usuarios/{usuarioId}/{coleccion}/{docId}} después del commit, en un hilo
 * aparte y con reintentos. Un fallo del espejo se registra en el log pero nunca
 * revierte la operación de negocio.</p>
 *
 * <p>Los números se guardan como números (no como texto) y las fechas como
 * {@link Timestamp}, para que Firestore pueda ordenar y sumar.</p>
 */
@Service
public class FirestoreSyncService {

    private static final Logger log =
            LoggerFactory.getLogger(FirestoreSyncService.class);

    private static final String COLECCION_USUARIOS = "usuarios";
    private static final String COLECCION_PRODUCTOS = "productos";
    private static final String COLECCION_INVENTARIO = "inventario";
    private static final String COLECCION_VENTAS = "ventas";
    private static final String COLECCION_CLIENTES = "clientes";

    /** Número total de intentos por escritura (1 inicial + reintentos). */
    private static final int MAX_INTENTOS = 4;
    /** Espera base entre reintentos: 0,5 s, 1 s, 2 s. */
    private static final long RETRASO_BASE_MS = 500;

    private final Firestore firestore;

    /** Permite desactivar el espejo (tests o entornos sin Firestore). */
    private final boolean habilitado;

    /** Escribe fuera del hilo de la petición y reintenta sin bloquear al usuario. */
    private final ScheduledExecutorService ejecutor =
            Executors.newScheduledThreadPool(2, tarea -> {
                Thread hilo = new Thread(tarea, "firestore-sync");
                hilo.setDaemon(true);
                return hilo;
            });

    public FirestoreSyncService(
            Firestore firestore,
            @Value("${shoppi.firebase.firestore-enabled:true}")
            boolean habilitado) {
        this.firestore = firestore;
        this.habilitado = habilitado;
    }

    public void guardarProducto(Producto producto) {

        Long usuarioId = producto.getUsuario().getId();
        Long productoId = producto.getId();

        Map<String, Object> datos = new HashMap<>();

        datos.put("productoId", productoId);
        datos.put("usuarioId", usuarioId);
        datos.put("nombre", producto.getNombre());
        datos.put("descripcion", producto.getDescripcion());
        datos.put("precio", numero(producto.getPrecio()));
        datos.put("stockActual", producto.getStockActual());
        datos.put("stockMinimo", producto.getStockMinimo());
        datos.put("categoria", producto.getCategoria());
        datos.put("imagenUrl", producto.getImagenUrl());
        datos.put("createdAt", aTimestamp(producto.getCreatedAt()));
        datos.put("updatedAt", aTimestamp(producto.getUpdatedAt()));

        ejecutarDespuesDeCommit("producto " + productoId, () -> {
            firestore.collection(COLECCION_USUARIOS)
                    .document(usuarioId.toString())
                    .collection(COLECCION_PRODUCTOS)
                    .document(productoId.toString())
                    .set(datos)
                    .get();

            log.info(
                    "Producto {} sincronizado con Firestore para usuario {}",
                    productoId, usuarioId
            );
        });
    }

    public void eliminarProducto(Long usuarioId, Long productoId) {

        ejecutarDespuesDeCommit("producto " + productoId, () -> {
            firestore.collection(COLECCION_USUARIOS)
                    .document(usuarioId.toString())
                    .collection(COLECCION_PRODUCTOS)
                    .document(productoId.toString())
                    .delete()
                    .get();

            log.info(
                    "Producto {} eliminado de Firestore para usuario {}",
                    productoId, usuarioId
            );
        });
    }

    public void guardarMovimiento(Inventario movimiento) {

        Producto producto = movimiento.getProducto();

        Long usuarioId = producto.getUsuario().getId();
        Long productoId = producto.getId();
        Long movimientoId = movimiento.getId();

        Map<String, Object> datos = new HashMap<>();

        datos.put("movimientoId", movimientoId);
        datos.put("usuarioId", usuarioId);
        datos.put("productoId", productoId);
        datos.put("productoNombre", producto.getNombre());
        datos.put("tipoMovimiento",
                movimiento.getTipoMovimiento().name());
        datos.put("cantidad", movimiento.getCantidad());
        datos.put("fecha", aTimestamp(movimiento.getFecha()));
        datos.put("descripcion", movimiento.getDescripcion());

        ejecutarDespuesDeCommit("movimiento " + movimientoId, () -> {
            firestore.collection(COLECCION_USUARIOS)
                    .document(usuarioId.toString())
                    .collection(COLECCION_INVENTARIO)
                    .document(movimientoId.toString())
                    .set(datos)
                    .get();

            log.info(
                    "Movimiento {} sincronizado con Firestore para usuario {}",
                    movimientoId, usuarioId
            );
        });
    }

    public void guardarVenta(Venta venta) {

        Long usuarioId = venta.getUsuario().getId();
        Long ventaId = venta.getId();

        Map<String, Object> datos = new HashMap<>();

        datos.put("ventaId", ventaId);
        datos.put("usuarioId", usuarioId);
        datos.put(
                "clienteId",
                venta.getCliente() != null
                        ? venta.getCliente().getId()
                        : null
        );
        datos.put("fecha", aTimestamp(venta.getFecha()));
        datos.put("subtotal", numero(venta.getSubtotal()));
        datos.put("impuestos", numero(venta.getImpuestos()));
        datos.put("total", numero(venta.getTotal()));
        datos.put("metodoPago", venta.getMetodoPago());
        datos.put("createdAt", aTimestamp(venta.getCreatedAt()));

        List<Map<String, Object>> detalles = new ArrayList<>();

        for (DetalleVenta detalle : venta.getDetalles()) {

            Producto producto = detalle.getProducto();

            Map<String, Object> datosDetalle = new HashMap<>();

            datosDetalle.put("detalleId", detalle.getId());
            datosDetalle.put("productoId", producto.getId());
            datosDetalle.put("productoNombre", producto.getNombre());
            datosDetalle.put("cantidad", detalle.getCantidad());
            datosDetalle.put(
                    "precioUnitario",
                    numero(detalle.getPrecioUnitario())
            );
            datosDetalle.put(
                    "subtotal",
                    numero(detalle.getSubtotal())
            );

            detalles.add(datosDetalle);
        }

        datos.put("detalles", detalles);

        ejecutarDespuesDeCommit("venta " + ventaId, () -> {
            firestore.collection(COLECCION_USUARIOS)
                    .document(usuarioId.toString())
                    .collection(COLECCION_VENTAS)
                    .document(ventaId.toString())
                    .set(datos)
                    .get();

            log.info(
                    "Venta {} sincronizada con Firestore para usuario {}",
                    ventaId, usuarioId
            );
        });
    }

    public void guardarCliente(Cliente cliente) {

        Long usuarioId = cliente.getUsuario().getId();
        Long clienteId = cliente.getId();

        Map<String, Object> datos = new HashMap<>();

        datos.put("clienteId", clienteId);
        datos.put("usuarioId", usuarioId);
        datos.put("nombre", cliente.getNombre());
        datos.put("telefono", cliente.getTelefono());
        datos.put("email", cliente.getEmail());
        datos.put("direccion", cliente.getDireccion());
        datos.put("createdAt", aTimestamp(cliente.getCreatedAt()));
        datos.put("updatedAt", aTimestamp(cliente.getUpdatedAt()));

        ejecutarDespuesDeCommit("cliente " + clienteId, () -> {
            firestore.collection(COLECCION_USUARIOS)
                    .document(usuarioId.toString())
                    .collection(COLECCION_CLIENTES)
                    .document(clienteId.toString())
                    .set(datos)
                    .get();

            log.info(
                    "Cliente {} sincronizado con Firestore para usuario {}",
                    clienteId, usuarioId
            );
        });
    }

    public void eliminarCliente(Long usuarioId, Long clienteId) {

        ejecutarDespuesDeCommit("cliente " + clienteId, () -> {
            firestore.collection(COLECCION_USUARIOS)
                    .document(usuarioId.toString())
                    .collection(COLECCION_CLIENTES)
                    .document(clienteId.toString())
                    .delete()
                    .get();

            log.info(
                    "Cliente {} eliminado de Firestore para usuario {}",
                    clienteId, usuarioId
            );
        });
    }

    /**
     * Ejecuta la operación solo si la transacción confirma. Fuera de una
     * transacción se ejecuta igualmente.
     */
    private void ejecutarDespuesDeCommit(
            String descripcion,
            OperacionFirestore operacion) {

        if (!habilitado) {
            return;
        }

        boolean enTransaccion =
                TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive();

        if (enTransaccion) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            programar(descripcion, operacion, 0);
                        }
                    }
            );
        } else {
            programar(descripcion, operacion, 0);
        }
    }

    private void programar(
            String descripcion,
            OperacionFirestore operacion,
            int intento) {

        long espera = intento == 0
                ? 0
                : RETRASO_BASE_MS * (1L << (intento - 1));

        ejecutor.schedule(() -> {
            try {
                operacion.ejecutar();
            } catch (Exception e) {
                int siguiente = intento + 1;
                if (siguiente < MAX_INTENTOS) {
                    log.warn(
                            "Firestore: fallo al sincronizar {} (intento {}/{}): {}. Reintentando...",
                            descripcion, siguiente, MAX_INTENTOS, e.getMessage()
                    );
                    programar(descripcion, operacion, siguiente);
                } else {
                    log.error(
                            "Firestore: no se pudo sincronizar {} tras {} intentos",
                            descripcion, MAX_INTENTOS, e
                    );
                }
            }
        }, espera, TimeUnit.MILLISECONDS);
    }

    private static Double numero(BigDecimal valor) {
        return valor == null ? null : valor.doubleValue();
    }

    private static Timestamp aTimestamp(LocalDateTime fecha) {
        if (fecha == null) {
            return null;
        }
        return Timestamp.of(
                Date.from(fecha.atZone(ZoneId.systemDefault()).toInstant())
        );
    }

    @FunctionalInterface
    private interface OperacionFirestore {
        void ejecutar() throws Exception;
    }
}
