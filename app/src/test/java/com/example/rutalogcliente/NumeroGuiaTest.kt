package com.example.rutalogcliente

import com.example.rutalogcliente.data.local.entities.TipoOperacion
import com.example.rutalogcliente.data.repository.GuiaCambiada
import com.example.rutalogcliente.data.repository.cambioDeGuia
import com.example.rutalogcliente.model.NumeroGuia
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NumeroGuiaTest {

    @Test
    fun `el correlativo nuevo es al azar, en el rango del celular y no repite los propios`() {
        val usados = (100_001..100_500).toSet()
        val azar = Random(2026)
        val generados = (1..500).map { NumeroGuia.correlativoLibre(usados, azar) }
        assertTrue(generados.all { it in NumeroGuia.PRIMER_CORRELATIVO until NumeroGuia.INICIO_SERVIDOR })
        assertTrue(generados.none { it in usados })
        // Ya no son consecutivos: no se apilan detrás de la última guía (eso chocaba con las de otros).
        assertTrue(generados.toSet().size > 490)
    }

    @Test
    fun `si solo queda un numero libre, lo encuentra`() {
        val usados = (NumeroGuia.PRIMER_CORRELATIVO until NumeroGuia.INICIO_SERVIDOR).toMutableSet().apply { remove(321_000) }
        assertEquals(321_000, NumeroGuia.correlativoLibre(usados, Random(1)))
    }

    @Test
    fun `la guia generada lleva su digito verificador`() {
        assertEquals("RLP-26-300030-6", NumeroGuia.generar(300_030, 2026))
        assertEquals(300_030, NumeroGuia.correlativo("RLP-26-300030-6"))
    }

    @Test
    fun `se detecta el cambio de guia solo al registrar y si el servidor devuelve otra`() {
        assertEquals(
            GuiaCambiada("RLP-26-300030-6", "RLP-26-500048-7"),
            cambioDeGuia(TipoOperacion.CREAR, "RLP-26-300030-6", "RLP-26-500048-7")
        )
        assertNull(cambioDeGuia(TipoOperacion.CREAR, "RLP-26-345678-3", "RLP-26-345678-3"))
        assertNull(cambioDeGuia(TipoOperacion.ACTUALIZAR, "RLP-26-300030-6", "RLP-26-500048-7"))
    }
}
