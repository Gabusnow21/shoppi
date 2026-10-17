package com.shoppi.ui.navigation

/**
 * Rutas de navegación (plan_ui_compose.md §2).
 * 5 destinos de BottomBar + sub-rutas.
 */
object Routes {
    // Auth
    const val LOGIN = "login"
    const val REGISTER = "register"

    // BottomBar (destinos top-level)
    const val DASHBOARD = "dashboard"
    const val PRODUCTOS = "productos"
    const val TERMINAL = "terminal"
    const val CLIENTES = "clientes"
    const val REPORTES = "reportes"

    // Sub-rutas
    const val INVENTARIO = "inventario/historial"
    const val PRODUCTO_DETAIL = "producto/{id}"
    const val PRODUCTO_FORM = "productoForm"
    const val CARRITO = "carrito/confirmacio"
    const val TICKET = "ticket/{ventaId}"
    const val REPORTE_DETAIL = "reporteDetalle/{periodo}"

    const val ARG_PRODUCTO_ID = "id"
    const val ARG_VENTA_ID = "ventaId"
    const val ARG_PERIODO = "periodo"

    fun productoDetail(id: Long) = "producto/$id"
    fun ticket(ventaId: Long) = "ticket/$ventaId"
    fun reporteDetail(periodo: String) = "reporteDetalle/$periodo"

    /** Rutas que muestran la BottomBar. */
    val topLevel = setOf(DASHBOARD, PRODUCTOS, TERMINAL, CLIENTES, REPORTES)
}
