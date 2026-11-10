package com.shoppi.data.remote.dto

/** Cuerpo de POST/DELETE /api/dispositivos/fcm-token. */
data class FcmTokenRequest(
    val token: String,
    val deviceId: String? = null,
    val plataforma: String? = "ANDROID"
)

/** Respuesta de POST /api/dispositivos/fcm-token. */
data class FcmTokenResponse(
    val ok: Boolean = true
)

/** Respuesta de POST /api/firebase/custom-token. */
data class FirebaseCustomTokenResponse(
    val customToken: String
)
