-- ============================================
-- MIGRACIÓN: dispositivos_fcm
-- Tokens push FCM por usuario/dispositivo.
-- Firebase auxiliar: solo ruteo de push, no datos de negocio.
-- ============================================
CREATE TABLE IF NOT EXISTS dispositivos_fcm (
    id              BIGSERIAL PRIMARY KEY,
    usuario_id      BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    fcm_token       VARCHAR(255) UNIQUE NOT NULL,
    device_id       VARCHAR(100),
    plataforma      VARCHAR(20) DEFAULT 'ANDROID',
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dispositivos_usuario ON dispositivos_fcm(usuario_id);

COMMENT ON TABLE dispositivos_fcm IS 'Tokens FCM para push (ventas, stock, sync). Firebase auxiliar.';
COMMENT ON COLUMN dispositivos_fcm.usuario_id IS 'Propietario del token';
COMMENT ON COLUMN dispositivos_fcm.fcm_token IS 'Token de registro FCM del dispositivo';
COMMENT ON COLUMN dispositivos_fcm.device_id IS 'Identificador estable del dispositivo (opcional)';
COMMENT ON COLUMN dispositivos_fcm.plataforma IS 'ANDROID (único soportado por ahora)';
