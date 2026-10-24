package com.shoppi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

        val container = (application as ShoppiApp).container
        val startDestination =
            if (container.session.haySesion()) Routes.DASHBOARD else Routes.LOGIN

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
