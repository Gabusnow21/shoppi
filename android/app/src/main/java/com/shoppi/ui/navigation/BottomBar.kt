package com.shoppi.ui.navigation

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.shoppi.ui.theme.Dimens

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val bottomDestinations = listOf(
    BottomDestination(Routes.DASHBOARD, "Inicio", Icons.Filled.Home),
    BottomDestination(Routes.PRODUCTOS, "Productos", Icons.Filled.Inventory),
    BottomDestination(Routes.TERMINAL, "Vender", Icons.Filled.PointOfSale),
    BottomDestination(Routes.CLIENTES, "Clientes", Icons.Filled.People),
    BottomDestination(Routes.REPORTES, "Reportes", Icons.Filled.BarChart),
)

/**
 * BottomBar de 80dp (canon dark). Se samplea `bottomnavba` de pantallas.txt:
 * dark #1E1E1E + borde top, light #FFFFFF + borde top; el color lo resuelve el tema.
 */
@Composable
fun ShoppiBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier.height(Dimens.BottomBarHeight),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        bottomDestinations.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onNavigate(destination.route) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label,
                    )
                },
                label = { Text(destination.label) },
            )
        }
    }
}
