# Reglas ProGuard/R8 para Shoppi.
# Release usa minify=false por ahora; estas reglas se activan al habilitar R8 (FASE 4).

# Retrofit / Gson: conservar genéricos y anotaciones.
# -keepattributes Signature
# -keepattributes *Annotation*
# -keep class com.shoppi.data.remote.dto.** { *; }
# -keep class com.shoppi.data.remote.**Response { *; }
