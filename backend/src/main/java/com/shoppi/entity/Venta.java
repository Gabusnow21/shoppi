package com.shoppi.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Venta realizada por un usuario.
 *
 * <p>Los totales se calculan en el servicio y se guardan desnormalizados para
 * listar y reportar sin recalcular las líneas. Si se elimina el cliente, la
 * venta conserva su historial con {@code cliente_id} en {@code null}, igual que
 * el esquema de PostgreSQL.</p>
 */
@Entity
@Table(name = "ventas")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Cliente cliente;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal impuestos;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "metodo_pago", nullable = false, length = 50)
    private String metodoPago;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles = new ArrayList<>();

    protected Venta() {
        // Requerido por JPA.
    }

    public Venta(
            Usuario usuario,
            Cliente cliente,
            BigDecimal subtotal,
            BigDecimal impuestos,
            BigDecimal total,
            String metodoPago) {
        this.usuario = usuario;
        this.cliente = cliente;
        this.subtotal = subtotal;
        this.impuestos = impuestos;
        this.total = total;
        this.metodoPago = metodoPago;
    }

    /** Añade una línea manteniendo ambos lados de la relación. */
    public void agregarDetalle(DetalleVenta detalle) {
        detalle.asignarVenta(this);
        detalles.add(detalle);
    }

    @PrePersist
    void alCrear() {
        LocalDateTime ahora = LocalDateTime.now();
        if (fecha == null) {
            fecha = ahora;
        }
        createdAt = ahora;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getImpuestos() {
        return impuestos;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<DetalleVenta> getDetalles() {
        return detalles;
    }
}
