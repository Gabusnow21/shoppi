package com.shoppi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Modo de tema seleccionable (SYSTEM/LIGHT/DARK).
 * El toggle persistido en DataStore se conecta en tareas posteriores.
 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val ShoppiDarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    background = BgMainDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = BgAltDark,
    onSurfaceVariant = OnBackgroundSecDark,
    outline = BorderSubtleDark,
    error = Danger,
    tertiary = Warning,
)

private val ShoppiLightColors = lightColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    background = BgMainLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnBackgroundLight,
    surfaceVariant = BgAltLight,
    onSurfaceVariant = OnBackgroundSecLight,
    outline = BorderSubtleLight,
    error = Danger,
    tertiary = Warning,
)

/**
 * Tema de Shoppi. Un único árbol Compose por pantalla: cambiar de modo solo
 * cambia el colorScheme, nunca medidas ni layout.
 */
@Composable
fun ShoppiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) ShoppiDarkColors else ShoppiLightColors,
        typography = ShoppiTypography,
        content = content,
    )
}
