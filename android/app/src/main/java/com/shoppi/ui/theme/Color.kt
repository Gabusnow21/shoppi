package com.shoppi.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// Design tokens dual-theme (plan_ui_compose.md §1)
// El canon de MEDIDAS es el modo oscuro; el claro solo aporta paleta.
// ============================================================

// --- Dark ---
val BgMainDark = Color(0xFF121212)
val BgAltDark = Color(0xFF131319)
val SurfaceDark = Color(0xFF1E1E1E)
val SurfaceBarDark = Color(0xFF1E1E1E)
val BorderSubtleDark = Color(0xFF444444)
val OnBackgroundDark = Color(0xFFFFFFFF)
val OnBackgroundSecDark = Color(0xFFB3B3B3)

// --- Light ---
val BgMainLight = Color(0xFFF9F9F9)
val BgAltLight = Color(0xFFF9F9F9)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceBarLight = Color(0xFFFFFFFF)
val BorderSubtleLight = Color(0xFFE0E0E0)
val OnBackgroundLight = Color(0xFF1A1A1A)
val OnBackgroundSecLight = Color(0xFF616161)

// --- Compartidos (mismos en ambos modos) ---
val Accent = Color(0xFF4CAF50)   // botones primarios / CTA venta
val Danger = Color(0xFFFF5252)   // stock bajo / borrar
val Warning = Color(0xFFFFC107)  // stock mínimo / badges
val OnAccent = Color(0xFFFFFFFF) // texto/icono sobre Accent
