package com.shoppi.controller;

import com.shoppi.dto.producto.ProductoRequest;
import com.shoppi.dto.producto.ProductoResponse;
import com.shoppi.security.UsuarioAutenticado;
import com.shoppi.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CRUD de productos del usuario autenticado.
 *
 * <p>Requiere {@code Authorization: Bearer <token>}; el id del usuario sale del
 * JWT y nunca del cuerpo de la petición.</p>
 */
@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public List<ProductoResponse> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return productoService.listar(usuario.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoResponse crear(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ProductoRequest request) {
        return productoService.crear(usuario.id(), request);
    }

    @GetMapping("/{id}")
    public ProductoResponse obtener(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        return productoService.obtener(usuario.id(), id);
    }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody ProductoRequest request) {
        return productoService.actualizar(usuario.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        productoService.eliminar(usuario.id(), id);
    }
}
