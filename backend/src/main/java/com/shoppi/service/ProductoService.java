package com.shoppi.service;

import com.shoppi.dto.producto.ProductoRequest;
import com.shoppi.dto.producto.ProductoResponse;
import com.shoppi.entity.Producto;
import com.shoppi.entity.Usuario;
import com.shoppi.repository.DetalleVentaRepository;
import com.shoppi.repository.ProductoRepository;
import com.shoppi.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CRUD de productos con alcance por usuario.
 *
 * <p>Todas las operaciones reciben el id del usuario autenticado y filtran por
 * él: un usuario nunca puede leer, editar ni borrar productos de otro. En esos
 * casos se responde 404 para no revelar su existencia.</p>
 */
@Service
public class ProductoService {

    private static final int STOCK_ACTUAL_POR_DEFECTO = 0;
    private static final int STOCK_MINIMO_POR_DEFECTO = 5;

    private final ProductoRepository productos;
    private final UsuarioRepository usuarios;
    private final DetalleVentaRepository detallesVenta;

    public ProductoService(
            ProductoRepository productos,
            UsuarioRepository usuarios,
            DetalleVentaRepository detallesVenta) {
        this.productos = productos;
        this.usuarios = usuarios;
        this.detallesVenta = detallesVenta;
    }

    /** Lista los productos del usuario autenticado. */
    @Transactional(readOnly = true)
    public List<ProductoResponse> listar(Long usuarioId) {
        return productos.findByUsuarioId(usuarioId).stream()
                .map(this::respuesta)
                .toList();
    }

    /** Obtiene un producto propio o lanza 404. */
    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long usuarioId, Long productoId) {
        return respuesta(buscarPropio(usuarioId, productoId));
    }

    /** Crea un producto para el usuario autenticado. */
    @Transactional
    public ProductoResponse crear(Long usuarioId, ProductoRequest request) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado."));

        Producto producto = new Producto(
                usuario,
                request.nombre().trim(),
                limpiar(request.descripcion()),
                request.precio(),
                request.stockActual() != null ? request.stockActual() : STOCK_ACTUAL_POR_DEFECTO,
                request.stockMinimo() != null ? request.stockMinimo() : STOCK_MINIMO_POR_DEFECTO,
                limpiar(request.categoria()),
                limpiar(request.imagenUrl()));

        return respuesta(productos.save(producto));
    }

    /** Actualiza un producto propio o lanza 404. */
    @Transactional
    public ProductoResponse actualizar(Long usuarioId, Long productoId, ProductoRequest request) {
        Producto producto = buscarPropio(usuarioId, productoId);

        producto.actualizar(
                request.nombre().trim(),
                limpiar(request.descripcion()),
                request.precio(),
                request.stockActual() != null ? request.stockActual() : producto.getStockActual(),
                request.stockMinimo() != null ? request.stockMinimo() : producto.getStockMinimo(),
                limpiar(request.categoria()),
                limpiar(request.imagenUrl()));

        return respuesta(producto);
    }

    /** Elimina un producto propio o lanza 404. */
    @Transactional
    public void eliminar(Long usuarioId, Long productoId) {
        Producto producto = buscarPropio(usuarioId, productoId);
        if (detallesVenta.existsByProductoId(productoId)) {
            throw new IllegalArgumentException(
                    "No se puede eliminar el producto porque tiene ventas registradas.");
        }
        productos.delete(producto);
    }

    private Producto buscarPropio(Long usuarioId, Long productoId) {
        return productos.findByIdAndUsuarioId(productoId, usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado."));
    }

    private ProductoResponse respuesta(Producto producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getStockActual(),
                producto.getStockMinimo(),
                producto.getCategoria(),
                producto.getImagenUrl());
    }

    private String limpiar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
