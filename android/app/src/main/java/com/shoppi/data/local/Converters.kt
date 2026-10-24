package com.shoppi.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.shoppi.data.model.MetodoPago
import com.shoppi.data.model.SyncState
import com.shoppi.data.model.VentaItem

/**
 * Convertidores de Room.
 *
 * Se declaran explícitamente para que el valor almacenado en SQLite sea estable
 * y legible: enums por nombre y líneas de venta como JSON (`itemsJson`).
 * No dependen de Android, por lo que se prueban en JVM (`ConvertersTest`).
 */
class Converters {

    private val gson = Gson()
    private val tipoItems = object : TypeToken<List<VentaItem>>() {}.type

    @TypeConverter
    fun deSyncState(valor: SyncState?): String? = valor?.name

    @TypeConverter
    fun aSyncState(valor: String?): SyncState? = SyncState.fromStorage(valor)

    @TypeConverter
    fun deMetodoPago(valor: MetodoPago?): String? = valor?.name

    @TypeConverter
    fun aMetodoPago(valor: String?): MetodoPago? = valor?.let { MetodoPago.fromStorage(it) }

    /** `List<VentaItem>` → JSON para la columna `itemsJson`. */
    @TypeConverter
    fun deItems(items: List<VentaItem>?): String = gson.toJson(items ?: emptyList<VentaItem>())

    /** JSON de la columna `itemsJson` → `List<VentaItem>`. */
    @TypeConverter
    fun aItems(json: String?): List<VentaItem> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            gson.fromJson<List<VentaItem>>(json, tipoItems) ?: emptyList()
        } catch (_: Exception) {
            // Un JSON corrupto no debe tumbar la lectura de la base.
            emptyList()
        }
    }
}
