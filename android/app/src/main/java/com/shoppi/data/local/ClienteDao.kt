package com.shoppi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shoppi.data.model.Cliente
import com.shoppi.data.model.SyncState
import kotlinx.coroutines.flow.Flow

/** Acceso local a clientes, con el mismo patrón offline-first que [ProductoDao]. */
@Dao
interface ClienteDao {

    @Query(
        """
        SELECT * FROM clientes
        WHERE usuarioId = :usuarioId AND deleted = 0
        ORDER BY nombre ASC
        """
    )
    fun observar(usuarioId: String): Flow<List<Cliente>>

    @Query("SELECT * FROM clientes WHERE id = :id LIMIT 1")
    suspend fun porId(id: String): Cliente?

    @Query(
        "SELECT * FROM clientes WHERE usuarioId = :usuarioId AND remoteId = :remoteId LIMIT 1"
    )
    suspend fun porRemoteId(usuarioId: String, remoteId: Long): Cliente?

    @Query("SELECT * FROM clientes WHERE usuarioId = :usuarioId")
    suspend fun todos(usuarioId: String): List<Cliente>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(cliente: Cliente)

    @Update
    suspend fun actualizar(cliente: Cliente)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarDesdeSincronizacion(cliente: Cliente)

    @Query(
        """
        UPDATE clientes
        SET deleted = 1, updatedAt = :ahora, syncState = 'PENDING'
        WHERE id = :id
        """
    )
    suspend fun marcarBorrado(id: String, ahora: Long)

    @Query("SELECT * FROM clientes WHERE usuarioId = :usuarioId AND syncState != 'SYNCED'")
    suspend fun pendientes(usuarioId: String): List<Cliente>

    @Query(
        "SELECT COUNT(*) FROM clientes WHERE usuarioId = :usuarioId AND syncState != 'SYNCED'"
    )
    fun contarPendientes(usuarioId: String): Flow<Int>

    @Query("UPDATE clientes SET syncState = :syncState WHERE id = :id")
    suspend fun marcarSync(id: String, syncState: SyncState)

    @Query(
        """
        UPDATE clientes
        SET remoteId = :remoteId, syncState = 'SYNCED', updatedAt = :ahora
        WHERE id = :id
        """
    )
    suspend fun marcarSincronizado(id: String, remoteId: Long, ahora: Long)

    @Query("DELETE FROM clientes WHERE id = :id")
    suspend fun eliminarFisico(id: String)

    @Query("DELETE FROM clientes")
    suspend fun borrarTodos()
}
