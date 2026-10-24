package com.shoppi.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shoppi.ShoppiApp
import com.shoppi.ui.components.PlaceholderScreen
import com.shoppi.ui.dashboard.DashboardScreen
import com.shoppi.ui.login.LoginScreen
import com.shoppi.ui.login.RegisterScreen

/**
 * Grafo de navegación base. Los destinos apuntan a placeholders que se
 * reemplazan por las pantallas reales en las tareas 2.2+.
 *
 * `startDestination` lo decide `MainActivity` según la sesión guardada.
 */
@Composable
fun ShoppiNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.LOGIN,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute != null && currentRoute in Routes.topLevel

    val context = LocalContext.current
    val container = remember(context) {
        (context.applicationContext as ShoppiApp).container
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                ShoppiBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Routes.DASHBOARD) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            // --- Auth ---
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginExitoso = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onIrARegistro = { navController.navigate(Routes.REGISTER) },
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    onRegistroExitoso = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onVolver = { navController.popBackStack() },
                )
            }

            // --- BottomBar ---
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    nombreUsuario = container.session.nombreCompleto(),
                    onCerrarSesion = {
                        container.usuarios.cerrarSesion()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.PRODUCTOS) { PlaceholderScreen("Productos (T-124)") }
            composable(Routes.TERMINAL) { PlaceholderScreen("Punto de venta (T-153)") }
            composable(Routes.CLIENTES) { PlaceholderScreen("Clientes (T-141)") }
            composable(Routes.REPORTES) { PlaceholderScreen("Reportes (T-161)") }

            // --- Sub-rutas ---
            composable(Routes.INVENTARIO) { PlaceholderScreen("Inventario (T-132)") }
            composable(
                route = Routes.PRODUCTO_DETAIL,
                arguments = listOf(navArgument(Routes.ARG_PRODUCTO_ID) { type = NavType.StringType }),
            ) { PlaceholderScreen("Detalle producto (T-124)") }
            composable(
                route = Routes.PRODUCTO_FORM,
                arguments = listOf(
                    navArgument(Routes.ARG_PRODUCTO_FORM_ID) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { PlaceholderScreen("Form producto (T-125)") }
            composable(Routes.CARRITO) { PlaceholderScreen("Carrito (T-154)") }
            composable(
                route = Routes.TICKET,
                arguments = listOf(navArgument(Routes.ARG_VENTA_ID) { type = NavType.LongType }),
            ) { PlaceholderScreen("Ticket (T-154)") }
            composable(
                route = Routes.REPORTE_DETAIL,
                arguments = listOf(navArgument(Routes.ARG_PERIODO) { type = NavType.StringType }),
            ) { PlaceholderScreen("Detalle reporte (T-161)") }
        }
    }
}
