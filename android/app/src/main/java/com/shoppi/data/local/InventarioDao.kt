package com.shoppi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shoppi.data.model.MovimientoInventario
import com.shoppi.data.model.SyncState
import kotlinx.coroutines.flow.Flow

/**
 * Acceso local a los movimientos de inventario.
 *
 * El historial es append-only (la API no expone PUT ni DELETE), así que solo
 * hay alta y sincronización: un movimiento nunca se edita ni se borra.
 */
@Dao
interface InventarioDao {

    /**
     * Historial con filtros opcionales. Los rangos de fecha llegan en epoch
     * millis; `null` significa "sin filtro" por ese lado.
     */
    @Query(
        """
        SELECT * FROM inventario
        WHERE usuarioId = :usuarioId
          AND (:productoId IS NULL OR productoId = :productoId)
          AND (:fechaInicio IS NULL OR fecha >= :fechaInicio)
          AND (:fechaFin IS NULL OR fecha <= :fechaFin)
        ORDER BY fecha DESC, id ASC
        """
    )
    fun observar(
        usuarioId: String,
        productoId: String?,
        fechaInicio: Long?,
        fechaFin: Long?
    ): Flow<List<MovimientoInventario>>

    @Query("SELECT * FROM inventario WHERE id = :id LIMIT 1")
    suspend fun porId(id: String): MovimientoInventario?

    @Query(
        "SELECT * FROM inventario WHERE usuarioId = :usuarioId AND remoteId = :remoteId LIMIT 1"
    )
    suspend fun porRemoteId(usuarioId: String, remoteId: Long): MovimientoInventario?

    @Query("SELECT * FROM inventario WHERE usuarioId = :usuarioId")
    suspend fun todos(usuarioId: String): List<MovimientoInventario>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(movimiento: MovimientoInventario)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarDesdeSincronizacion(movimiento: MovimientoInventario)

    @Query("SELECT * FROM inventario WHERE usuarioId = :usuarioId AND syncState != 'SYNCED'")
    suspend fun pendientes(usuarioId: String): List<MovimientoInventario>

    @Query(
        """
        UPDATE inventario
        SET remoteId = :remoteId, syncState = 'SYNCED'
        WHERE id = :id
        """
    )
    suspend fun marcarSincronizado(id: String, remoteId: Long)

    @Query("UPDATE inventario SET syncState = :syncState WHERE id = :id")
    suspend fun marcarSync(id: String, syncState: SyncState)

    @Query("DELETE FROM inventario")
    suspend fun borrarTodos()
}
