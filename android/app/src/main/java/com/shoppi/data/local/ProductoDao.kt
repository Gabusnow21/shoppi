package com.shoppi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shoppi.data.model.Producto
import com.shoppi.data.model.SyncState
import kotlinx.coroutines.flow.Flow

/**
 * Acceso local a productos.
 *
 * La UI observa [observar] (Flow) y la sincronización usa [pendientes],
 * [marcarSync] y [guardarDesdeSincronizacion]. Room es la única base que escribe
 * la app; la API solo se toca desde `SyncManager`.
 */
@Dao
interface ProductoDao {

    @Query(
        """
        SELECT * FROM productos
        WHERE usuarioId = :usuarioId AND deleted = 0
        ORDER BY updatedAt DESC
        """
    )
    fun observar(usuarioId: String): Flow<List<Producto>>

    @Query("SELECT * FROM productos WHERE id = :id LIMIT 1")
    suspend fun porId(id: String): Producto?

    @Query(
        "SELECT * FROM productos WHERE usuarioId = :usuarioId AND remoteId = :remoteId LIMIT 1"
    )
    suspend fun porRemoteId(usuarioId: String, remoteId: Long): Producto?

    @Query("SELECT * FROM productos WHERE usuarioId = :usuarioId")
    suspend fun todos(usuarioId: String): List<Producto>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(producto: Producto)

    @Update
    suspend fun actualizar(producto: Producto)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarDesdeSincronizacion(producto: Producto)

    @Query(
        """
        UPDATE productos
        SET deleted = 1, updatedAt = :ahora, syncState = 'PENDING'
        WHERE id = :id
        """
    )
    suspend fun marcarBorrado(id: String, ahora: Long)

    @Query("SELECT * FROM productos WHERE usuarioId = :usuarioId AND syncState != 'SYNCED'")
    suspend fun pendientes(usuarioId: String): List<Producto>

    @Query(
        "SELECT COUNT(*) FROM productos WHERE usuarioId = :usuarioId AND syncState != 'SYNCED'"
    )
    fun contarPendientes(usuarioId: String): Flow<Int>

    @Query("UPDATE productos SET syncState = :syncState WHERE id = :id")
    suspend fun marcarSync(id: String, syncState: SyncState)

    @Query(
        """
        UPDATE productos
        SET remoteId = :remoteId, syncState = 'SYNCED', updatedAt = :ahora
        WHERE id = :id
        """
    )
    suspend fun marcarSincronizado(id: String, remoteId: Long, ahora: Long)

    @Query("DELETE FROM productos WHERE id = :id")
    suspend fun eliminarFisico(id: String)

    @Query("DELETE FROM productos")
    suspend fun borrarTodos()
}
