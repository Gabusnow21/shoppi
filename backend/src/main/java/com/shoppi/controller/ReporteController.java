package com.shoppi.controller;

import com.shoppi.dto.reporte.ProductoMasVendidoResponse;
import com.shoppi.dto.reporte.ReporteInventarioResponse;
import com.shoppi.dto.reporte.ReporteVentasResponse;
import com.shoppi.security.UsuarioAutenticado;
import com.shoppi.service.ReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Reportes del usuario autenticado.
 *
 * <p>Requiere {@code Authorization: Bearer <token>}; el id del usuario sale del
 * JWT y nunca de los parámetros.</p>
 */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/ventas")
    public ReporteVentasResponse ventas(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(defaultValue = "diario") String periodo,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return reporteService.ventas(usuario.id(), periodo, fechaInicio, fechaFin);
    }

    @GetMapping("/productos-mas-vendidos")
    public List<ProductoMasVendidoResponse> productosMasVendidos(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(defaultValue = "10") Integer limite,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return reporteService.productosMasVendidos(usuario.id(), limite, fechaInicio, fechaFin);
    }

    @GetMapping("/inventario")
    public ReporteInventarioResponse inventario(
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return reporteService.inventario(usuario.id());
    }
}
