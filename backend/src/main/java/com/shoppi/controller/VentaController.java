package com.shoppi.controller;

import com.shoppi.dto.venta.VentaRequest;
import com.shoppi.dto.venta.VentaResponse;
import com.shoppi.security.UsuarioAutenticado;
import com.shoppi.service.VentaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Ventas del usuario autenticado.
 *
 * <p>Requiere {@code Authorization: Bearer <token>}; el id del usuario sale del
 * JWT y solo se opera sobre clientes y productos propios.</p>
 */
@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @GetMapping
    public List<VentaResponse> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ventaService.listar(usuario.id(), clienteId, fechaInicio, fechaFin);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VentaResponse crear(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody VentaRequest request) {
        return ventaService.crear(usuario.id(), request);
    }

    @GetMapping("/{id}")
    public VentaResponse obtener(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        return ventaService.obtener(usuario.id(), id);
    }
}
