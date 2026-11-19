
package com.shoppi.service;

import com.google.cloud.firestore.Firestore;
import com.shoppi.entity.Cliente;
import com.shoppi.entity.DetalleVenta;
import com.shoppi.entity.Inventario;
import com.shoppi.entity.Producto;
import com.shoppi.entity.Venta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FirestoreSyncService {

    private static final Logger log =
            LoggerFactory.getLogger(FirestoreSyncService.class);

    private static final String COLECCION_USUARIOS = "usuarios";
    private static final String COLECCION_PRODUCTOS = "productos";
    private static final String COLECCION_INVENTARIO = "inventario";
    private static final String COLECCION_VENTAS = "ventas";
    private static final String COLECCION_CLIENTES = "clientes";

    private final Firestore firestore;

    public FirestoreSyncService(Firestore firestore) {
        this.firestore = firestore;
    }

    public void guardarProducto(Producto producto) {

        Long usuarioId = producto.getUsuario().getId();
        Long productoId = producto.getId();

        Map<String, Object> datos = new HashMap<>();

        datos.put("productoId", productoId);
        datos.put("usuarioId", usuarioId);
        datos.put("nombre", producto.getNombre());
        datos.put("descripcion", producto.getDescripcion());
        datos.put("precio", producto.getPrecio().toPlainString());
        datos.put("stockActual", producto.getStockActual());
        datos.put("stockMinimo", producto.getStockMinimo());
        datos.put("categoria", producto.getCategoria());
        datos.put("imagenUrl", producto.getImagenUrl());
        datos.put("createdAt", producto.getCreatedAt().toString());
        datos.put("updatedAt", producto.getUpdatedAt().toString());

        ejecutarDespuesDeCommit(() -> {
            try {
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

            } catch (Exception e) {
                log.error(
                        "Error sincronizando producto {} con Firestore",
                        productoId, e
                );
            }
        });
    }

    public void eliminarProducto(Long usuarioId, Long productoId) {

        ejecutarDespuesDeCommit(() -> {
            try {
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

            } catch (Exception e) {
                log.error(
                        "Error eliminando producto {} de Firestore",
                        productoId, e
                );
            }
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
        datos.put("fecha", movimiento.getFecha().toString());
        datos.put("descripcion", movimiento.getDescripcion());

        ejecutarDespuesDeCommit(() -> {
            try {
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

            } catch (Exception e) {
                log.error(
                        "Error sincronizando movimiento {} con Firestore",
                        movimientoId, e
                );
            }
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
        datos.put("fecha", venta.getFecha().toString());
        datos.put("subtotal", venta.getSubtotal().toPlainString());
        datos.put("impuestos", venta.getImpuestos().toPlainString());
        datos.put("total", venta.getTotal().toPlainString());
        datos.put("metodoPago", venta.getMetodoPago());
        datos.put("createdAt", venta.getCreatedAt().toString());

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
                    detalle.getPrecioUnitario().toPlainString()
            );
            datosDetalle.put(
                    "subtotal",
                    detalle.getSubtotal().toPlainString()
            );

            detalles.add(datosDetalle);
        }

        datos.put("detalles", detalles);

        ejecutarDespuesDeCommit(() -> {
            try {
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

            } catch (Exception e) {
                log.error(
                        "Error sincronizando venta {} con Firestore",
                        ventaId, e
                );
            }
        });
    }

    // Guarda o actualiza un cliente en Firestore.
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
        datos.put(
                "createdAt",
                cliente.getCreatedAt() != null
                        ? cliente.getCreatedAt().toString()
                        : null
        );
        datos.put(
                "updatedAt",
                cliente.getUpdatedAt() != null
                        ? cliente.getUpdatedAt().toString()
                        : null
        );

        ejecutarDespuesDeCommit(() -> {
            try {
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

            } catch (Exception e) {
                log.error(
                        "Error sincronizando cliente {} con Firestore",
                        clienteId, e
                );
            }
        });
    }

    // Elimina un cliente de Firestore.
    public void eliminarCliente(Long usuarioId, Long clienteId) {

        ejecutarDespuesDeCommit(() -> {
            try {
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

            } catch (Exception e) {
                log.error(
                        "Error eliminando cliente {} de Firestore",
                        clienteId, e
                );
            }
        });
    }

    private void ejecutarDespuesDeCommit(Runnable operacion) {

        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            operacion.run();
                        }
                    }
            );

        } else {
            operacion.run();
        }
    }
}
