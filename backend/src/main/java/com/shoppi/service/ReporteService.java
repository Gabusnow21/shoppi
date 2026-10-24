package com.shoppi.service;

import com.shoppi.dto.reporte.PeriodoReporte;
import com.shoppi.dto.reporte.ProductoInventarioResponse;
import com.shoppi.dto.reporte.ProductoMasVendidoResponse;
import com.shoppi.dto.reporte.ReporteInventarioResponse;
import com.shoppi.dto.reporte.ReporteVentasResponse;
import com.shoppi.dto.reporte.VentaPorDiaResponse;
import com.shoppi.entity.Producto;
import com.shoppi.repository.DetalleVentaRepository;
import com.shoppi.repository.ProductoRepository;
import com.shoppi.repository.VentaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reportes de ventas e inventario del usuario autenticado.
 *
 * <p>Todos los cálculos se filtran por usuario. El reporte de ventas agrupa por
 * día y rellena con cero los días sin movimientos para que el cliente pueda
 * dibujar una serie continua.</p>
 */
@Service
public class ReporteService {

    private static final int LIMITE_POR_DEFECTO = 10;
    private static final int LIMITE_MAXIMO = 100;
    private static final LocalDateTime DESDE_MINIMO = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime HASTA_MAXIMO = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    private final VentaRepository ventas;
    private final DetalleVentaRepository detalles;
    private final ProductoRepository productos;

    public ReporteService(
            VentaRepository ventas,
            DetalleVentaRepository detalles,
            ProductoRepository productos) {
        this.ventas = ventas;
        this.detalles = detalles;
        this.productos = productos;
    }

    /**
     * Ventas del período. Si no se envían fechas se usa el período actual:
     * hoy para diario, lunes a hoy para semanal y día 1 a hoy para mensual.
     */
    @Transactional(readOnly = true)
    public ReporteVentasResponse ventas(
            Long usuarioId,
            String periodo,
            LocalDate fechaInicio,
            LocalDate fechaFin) {

        PeriodoReporte periodoReporte = PeriodoReporte.desde(periodo);
        RangoFechas rango = resolverRango(periodoReporte, fechaInicio, fechaFin);

        List<Object[]> filas = ventas.fechasYMontos(
                usuarioId,
                rango.inicio().atStartOfDay(),
                rango.fin().atTime(LocalTime.MAX));

        Map<LocalDate, Long> cantidades = new HashMap<>();
        Map<LocalDate, BigDecimal> montos = new HashMap<>();
        BigDecimal montoTotal = BigDecimal.ZERO;
        long totalVentas = 0;

        for (Object[] fila : filas) {
            LocalDate dia = ((LocalDateTime) fila[0]).toLocalDate();
            BigDecimal monto = (BigDecimal) fila[1];

            cantidades.merge(dia, 1L, Long::sum);
            montos.merge(dia, monto, BigDecimal::add);
            montoTotal = montoTotal.add(monto);
            totalVentas++;
        }

        List<VentaPorDiaResponse> ventasPorDia = new ArrayList<>();
        for (LocalDate dia = rango.inicio(); !dia.isAfter(rango.fin()); dia = dia.plusDays(1)) {
            ventasPorDia.add(new VentaPorDiaResponse(
                    dia,
                    cantidades.getOrDefault(dia, 0L),
                    montos.getOrDefault(dia, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)));
        }

        return new ReporteVentasResponse(
                periodoReporte.etiqueta(),
                rango.inicio(),
                rango.fin(),
                totalVentas,
                montoTotal.setScale(2, RoundingMode.HALF_UP),
                ventasPorDia);
    }

    /**
     * Top de productos más vendidos. Sin fechas se considera todo el historial.
     */
    @Transactional(readOnly = true)
    public List<ProductoMasVendidoResponse> productosMasVendidos(
            Long usuarioId,
            Integer limite,
            LocalDate fechaInicio,
            LocalDate fechaFin) {

        int limiteFinal = limite != null ? limite : LIMITE_POR_DEFECTO;
        if (limiteFinal < 1 || limiteFinal > LIMITE_MAXIMO) {
            throw new IllegalArgumentException(
                    "limite debe estar entre 1 y " + LIMITE_MAXIMO + ".");
        }
        if (fechaInicio != null && fechaFin != null && fechaInicio.isAfter(fechaFin)) {
            throw new IllegalArgumentException("fechaInicio no puede ser posterior a fechaFin.");
        }

        LocalDateTime desde = fechaInicio != null
                ? fechaInicio.atStartOfDay()
                : DESDE_MINIMO;
        LocalDateTime hasta = fechaFin != null
                ? fechaFin.atTime(LocalTime.MAX)
                : HASTA_MAXIMO;

        return detalles.productosMasVendidos(
                        usuarioId, desde, hasta, PageRequest.of(0, limiteFinal)).stream()
                .map(fila -> new ProductoMasVendidoResponse(
                        ((Number) fila[0]).longValue(),
                        (String) fila[1],
                        ((Number) fila[2]).longValue(),
                        aDecimal(fila[3])))
                .toList();
    }

    /** Estado del inventario con valorización y marca de stock bajo. */
    @Transactional(readOnly = true)
    public ReporteInventarioResponse inventario(Long usuarioId) {
        List<Producto> lista = productos.findByUsuarioId(usuarioId);

        BigDecimal valorInventario = BigDecimal.ZERO;
        int productosStockBajo = 0;
        List<ProductoInventarioResponse> items = new ArrayList<>();

        for (Producto producto : lista) {
            BigDecimal valorStock = producto.getPrecio()
                    .multiply(BigDecimal.valueOf(producto.getStockActual()))
                    .setScale(2, RoundingMode.HALF_UP);
            boolean stockBajo = producto.getStockActual() <= producto.getStockMinimo();

            if (stockBajo) {
                productosStockBajo++;
            }
            valorInventario = valorInventario.add(valorStock);

            items.add(new ProductoInventarioResponse(
                    producto.getId(),
                    producto.getNombre(),
                    producto.getStockActual(),
                    producto.getStockMinimo(),
                    producto.getPrecio(),
                    valorStock,
                    stockBajo));
        }

        items.sort(Comparator.comparing(ProductoInventarioResponse::stockBajo).reversed()
                .thenComparing(item -> item.nombre().toLowerCase(Locale.ROOT)));

        return new ReporteInventarioResponse(
                lista.size(),
                productosStockBajo,
                valorInventario.setScale(2, RoundingMode.HALF_UP),
                items);
    }

    private RangoFechas resolverRango(
            PeriodoReporte periodo,
            LocalDate fechaInicio,
            LocalDate fechaFin) {

        LocalDate hoy = LocalDate.now();

        if (fechaInicio == null && fechaFin == null) {
            LocalDate inicio = switch (periodo) {
                case DIARIO -> hoy;
                case SEMANAL -> hoy.with(DayOfWeek.MONDAY);
                case MENSUAL -> hoy.withDayOfMonth(1);
            };
            return new RangoFechas(inicio, hoy);
        }

        if (fechaInicio == null) {
            fechaInicio = switch (periodo) {
                case DIARIO -> fechaFin;
                case SEMANAL -> fechaFin.with(DayOfWeek.MONDAY);
                case MENSUAL -> fechaFin.withDayOfMonth(1);
            };
        }
        if (fechaFin == null) {
            fechaFin = hoy;
        }
        if (fechaInicio.isAfter(fechaFin)) {
            throw new IllegalArgumentException("fechaInicio no puede ser posterior a fechaFin.");
        }
        return new RangoFechas(fechaInicio, fechaFin);
    }

    private BigDecimal aDecimal(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal decimal = valor instanceof BigDecimal bigDecimal
                ? bigDecimal
                : new BigDecimal(valor.toString());
        return decimal.setScale(2, RoundingMode.HALF_UP);
    }

    private record RangoFechas(LocalDate inicio, LocalDate fin) {
    }
}
