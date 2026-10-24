package com.shoppi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shoppi.data.model.Usuario

/** Acceso local al usuario, usado para el login offline y para cachear el perfil. */
@Dao
interface UsuarioDao {

    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    suspend fun porEmail(email: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    suspend fun porId(id: String): Usuario?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(usuario: Usuario)

    @Query("DELETE FROM usuarios")
    suspend fun borrarTodos()
}
