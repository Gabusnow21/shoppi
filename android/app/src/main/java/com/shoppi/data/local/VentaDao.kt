package com.shoppi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shoppi.data.model.SyncState
import com.shoppi.data.model.Venta
import kotlinx.coroutines.flow.Flow

/** Acceso local a ventas. La API no expone borrado, así que no hay soft delete. */
@Dao
interface VentaDao {

    @Query(
        """
        SELECT * FROM ventas
        WHERE usuarioId = :usuarioId
        ORDER BY fecha DESC
        """
    )
    fun observar(usuarioId: String): Flow<List<Venta>>

    @Query("SELECT * FROM ventas WHERE id = :id LIMIT 1")
    suspend fun porId(id: String): Venta?

    @Query(
        "SELECT * FROM ventas WHERE usuarioId = :usuarioId AND remoteId = :remoteId LIMIT 1"
    )
    suspend fun porRemoteId(usuarioId: String, remoteId: Long): Venta?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(venta: Venta)

    @Update
    suspend fun actualizar(venta: Venta)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarDesdeSincronizacion(venta: Venta)

    @Query("SELECT * FROM ventas WHERE usuarioId = :usuarioId AND syncState != 'SYNCED'")
    suspend fun pendientes(usuarioId: String): List<Venta>

    @Query("SELECT COUNT(*) FROM ventas WHERE usuarioId = :usuarioId AND syncState != 'SYNCED'")
    fun contarPendientes(usuarioId: String): Flow<Int>

    @Query("UPDATE ventas SET syncState = :syncState WHERE id = :id")
    suspend fun marcarSync(id: String, syncState: SyncState)

    @Query(
        """
        UPDATE ventas
        SET remoteId = :remoteId, syncState = 'SYNCED', updatedAt = :ahora
        WHERE id = :id
        """
    )
    suspend fun marcarSincronizado(id: String, remoteId: Long, ahora: Long)

    @Query("DELETE FROM ventas")
    suspend fun borrarTodos()
}
