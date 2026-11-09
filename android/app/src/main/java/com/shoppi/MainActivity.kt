package com.shoppi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.shoppi.ui.navigation.Routes
import com.shoppi.ui.navigation.ShoppiNavGraph
import com.shoppi.ui.theme.ShoppiTheme

/** Activity única; toda la UI vive en Compose (Navigation-Compose). */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val container = (application as ShoppiApp).container

        val pushType = intent?.getStringExtra("push_type")
        val startDestination = when (pushType) {
            "VENTA_CONFIRMADA" -> {
                val ventaId = intent?.getLongExtra("ventaId", -1L) ?: -1L
                if (ventaId > 0) Routes.ticket(ventaId) else Routes.DASHBOARD
            }
            "STOCK_BAJO" -> Routes.INVENTARIO
            else -> if (container.session.haySesion()) Routes.DASHBOARD else Routes.LOGIN
        }

        setContent {
            ShoppiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    ShoppiNavGraph(startDestination = startDestination)
                }
            }
        }
    }
}
