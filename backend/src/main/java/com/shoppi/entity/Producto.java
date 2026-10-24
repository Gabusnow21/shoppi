package com.shoppi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Producto del catálogo, siempre asociado a un usuario propietario.
 *
 * <p>El precio se guarda como {@link BigDecimal} para no perder precisión
 * monetaria. El stock nunca puede ser negativo y la imagen solo se almacena
 * como URL pública (el binario vive en Firebase Storage).</p>
 */
@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "stock_actual", nullable = false)
    private int stockActual;

    @Column(name = "stock_minimo", nullable = false)
    private int stockMinimo;

    @Column(length = 100)
    private String categoria;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Producto() {
        // Requerido por JPA.
    }

    public Producto(
            Usuario usuario,
            String nombre,
            String descripcion,
            BigDecimal precio,
            int stockActual,
            int stockMinimo,
            String categoria,
            String imagenUrl) {
        this.usuario = usuario;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.categoria = categoria;
        this.imagenUrl = imagenUrl;
    }

    /** Actualiza los datos editables; el propietario no cambia. */
    public void actualizar(
            String nombre,
            String descripcion,
            BigDecimal precio,
            int stockActual,
            int stockMinimo,
            String categoria,
            String imagenUrl) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.categoria = categoria;
        this.imagenUrl = imagenUrl;
    }

    @PrePersist
    void alCrear() {
        LocalDateTime ahora = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = ahora;
        }
        updatedAt = ahora;
    }

    @PreUpdate
    void alActualizar() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public int getStockActual() {
        return stockActual;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
