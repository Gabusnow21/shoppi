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

import java.time.LocalDateTime;

/**
 * Token FCM de un dispositivo asociado a un usuario.
 *
 * <p>Firebase es auxiliar: esta tabla solo rutea push (ventas, stock, sync).
 * No contiene datos de negocio; la fuente de verdad sigue siendo PostgreSQL.</p>
 */
@Entity
@Table(name = "dispositivos_fcm")
public class DispositivoFcm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fcm_token", nullable = false, unique = true, length = 255)
    private String fcmToken;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "plataforma", length = 20)
    private String plataforma;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected DispositivoFcm() {
        // Requerido por JPA.
    }

    public DispositivoFcm(Usuario usuario, String fcmToken, String deviceId, String plataforma) {
        this.usuario = usuario;
        this.fcmToken = fcmToken;
        this.deviceId = deviceId;
        this.plataforma = plataforma == null ? "ANDROID" : plataforma;
    }

    @PrePersist
    void alCrear() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (plataforma == null) {
            plataforma = "ANDROID";
        }
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

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getPlataforma() {
        return plataforma;
    }

    public void setPlataforma(String plataforma) {
        this.plataforma = plataforma;
    }

    /** Actualiza campos mutables y refresca {@code updatedAt} (upsert). */
    public void actualizar(String deviceId, String plataforma) {
        this.deviceId = deviceId;
        this.plataforma = (plataforma == null || plataforma.isBlank()) ? "ANDROID" : plataforma;
        this.updatedAt = LocalDateTime.now();
    }
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
