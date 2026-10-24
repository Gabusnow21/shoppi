package com.shoppi.sync

/**
 * Resumen de una sincronización.
 *
 * [enviados] son registros locales empujados a la API; [recibidos] son registros
 * remotos adoptados en Room. [sinConexion] indica que se cortó por red y los
 * pendientes siguen en `PENDING` para el próximo intento.
 */
data class ResultadoSync(
    val enviados: Int = 0,
    val recibidos: Int = 0,
    val fallidos: Int = 0,
    val sinConexion: Boolean = false,
    val mensaje: String = ""
) {
    val exitoso: Boolean get() = fallidos == 0 && !sinConexion
}
