package com.shoppi.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.shoppi.data.model.Cliente
import com.shoppi.data.model.Producto
import com.shoppi.data.model.Usuario
import com.shoppi.data.model.Venta

/**
 * Base de datos local (SQLite vía Room).
 *
 * Room es la fuente de verdad del cliente: toda escritura nace aquí con
 * `syncState = PENDING` y el `SyncManager` la replica después en la API REST.
 * Se mantiene una sola instancia por proceso.
 */
@Database(
    entities = [Usuario::class, Producto::class, Cliente::class, Venta::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao

    abstract fun productoDao(): ProductoDao

    abstract fun clienteDao(): ClienteDao

    abstract fun ventaDao(): VentaDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun obtener(contexto: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    contexto.applicationContext,
                    AppDatabase::class.java,
                    "shoppi.db"
                )
                    // En desarrollo el esquema evoluciona por fases; antes de
                    // publicar se reemplaza por migraciones reales.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instancia = it }
            }
    }
}
