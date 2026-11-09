package com.shoppi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shoppi.ui.theme.ShoppiTheme

/**
 * Activity exclusiva del source set `debug` para verificar que Crashlytics
 * recibe un crash real. No se incluye en builds release.
 *
 * Lanzar con:
 * ```
 * adb shell am start -n com.shoppi/.CrashTestActivity
 * ```
 * y pulsar "Provocar crash".
 */
class CrashTestActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ShoppiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    PantallaPruebaCrash()
                }
            }
        }
    }
}

@Composable
private fun PantallaPruebaCrash() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Prueba de Crashlytics",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Esta pantalla solo existe en builds debug.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = {
                throw RuntimeException("Crash de prueba de Crashlytics")
            },
        ) {
            Text("Provocar crash")
        }
    }
}
