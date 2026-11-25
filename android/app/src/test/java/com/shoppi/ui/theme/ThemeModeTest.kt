package com.shoppi.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `fromStorage reconoce los tres modos`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage("SYSTEM"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStorage("LIGHT"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorage("DARK"))
    }

    @Test
    fun `fromStorage es tolerante a valores desconocidos`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage(""))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage("oscuro"))
    }
}
