# Reglas ProGuard/R8 para Shoppi.
# Release usa minify=false por ahora (FASE 4 puede activarlo).

# Retrofit / Gson (se añaden en T-101): conservar genéricos y anotaciones.
# -keepattributes Signature
# -keepattributes *Annotation*
# -keep class com.shoppi.data.remote.dto.** { *; }
