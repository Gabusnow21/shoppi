package com.shoppi.data.remote

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.shoppi.config.RemoteConfigManager
import com.shoppi.util.SessionManager
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Subida de imágenes a Firebase Storage usando un custom-token emitido por el
 * backend (Firebase es auxiliar; el login de negocio es JWT propio).
 */
class StorageDataSource(
    private val api: ApiService,
    private val session: SessionManager,
    private val maxMbProvider: () -> Int = { RemoteConfigManager.storageMaxMb },
) {

    suspend fun subirImagen(context: Context, uri: Uri): Result<String> = runCatching {
        val usuarioId = session.idUsuario()
            ?: throw IllegalStateException("No hay una sesión activa.")

        // Validación de tamaño en cliente (espejo de las reglas de Storage).
        val maxBytes = maxMbProvider() * 1024L * 1024L
        val size = context.contentResolver.openInputStream(uri)?.use { it.available().toLong() } ?: 0L
        require(size in 1..maxBytes) { "La imagen supera el límite de ${maxMbProvider()} MB." }

        // Canje del JWT propio por un Firebase custom-token.
        val customToken = api.crearFirebaseCustomToken().customToken
        FirebaseAuth.getInstance().signInWithCustomToken(customToken).await()

        val path = "productos/$usuarioId/${UUID.randomUUID()}.jpg"
        val ref = FirebaseStorage.getInstance().reference.child(path)
        ref.putFile(uri).await()
        ref.downloadUrl.await().toString()
    }
}