package com.example.rutalogcliente

import com.example.rutalogcliente.model.ReglasCuenta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasCuentaTest {

    @Test
    fun cuentaValida_sinErrores() {
        val errores = ReglasCuenta.validar("Ana Quispe", "ana@gmail.com", "envios2026", "envios2026")
        assertFalse(errores.hayErrores)
    }

    @Test
    fun limpiaNombreYCorreo() {
        assertEquals("Ana Quispe", ReglasCuenta.limpiarNombre("  Ana   Quispe "))
        assertEquals("ana@gmail.com", ReglasCuenta.limpiarCorreo(" Ana@Gmail.COM "))
    }

    @Test
    fun correoInvalido() {
        assertNotNull(ReglasCuenta.validar("Ana", "ana@gmail", "envios2026", "envios2026").correo)
        assertNotNull(ReglasCuenta.validar("Ana", "", "envios2026", "envios2026").correo)
    }

    @Test
    fun claveDebil_oNoCoincide() {
        assertNotNull(ReglasCuenta.validar("Ana", "ana@gmail.com", "abc12", "abc12").clave)
        assertNotNull(ReglasCuenta.validar("Ana", "ana@gmail.com", "soloLetras", "soloLetras").clave)
        assertNotNull(ReglasCuenta.validar("Ana", "ana@gmail.com", "12345678", "12345678").clave)
        val distinta = ReglasCuenta.validar("Ana", "ana@gmail.com", "envios2026", "envios2025")
        assertNull(distinta.clave)
        assertNotNull(distinta.confirmacion)
        assertTrue(distinta.hayErrores)
    }

    @Test
    fun nombreCorto() {
        assertNotNull(ReglasCuenta.validar("A", "ana@gmail.com", "envios2026", "envios2026").nombre)
    }
}
