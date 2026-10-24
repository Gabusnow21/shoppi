package com.shoppi.controller;

import com.shoppi.dto.inventario.MovimientoRequest;
import com.shoppi.dto.inventario.MovimientoResponse;
import com.shoppi.security.UsuarioAutenticado;
import com.shoppi.service.InventarioService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Movimientos de inventario del usuario autenticado.
 *
 * <p>Los endpoints requieren {@code Authorization: Bearer <token>} y solo
 * operan sobre productos propios.</p>
 */
@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @PostMapping("/movimiento")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoResponse registrar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody MovimientoRequest request) {
        return inventarioService.registrar(usuario.id(), request);
    }

    @GetMapping
    public List<MovimientoResponse> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return inventarioService.listar(usuario.id(), productoId, fechaInicio, fechaFin);
    }
}
