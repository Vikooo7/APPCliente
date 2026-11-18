package com.example.rutalogcliente

import com.example.rutalogcliente.data.mapper.aModelo
import com.example.rutalogcliente.data.remote.dto.EnvioDto
import com.example.rutalogcliente.data.remote.dto.EstimacionDto
import com.example.rutalogcliente.data.remote.dto.SeguimientoDto
import com.example.rutalogcliente.model.LlegadaEstimada
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LlegadaEstimadaTest {

    private val lima = ZoneId.of("America/Lima")
    private val jueves14h = Instant.parse("2026-11-19T19:00:00Z").toEpochMilli() // jueves 19/11, 14:00 en Lima

    @Test
    fun `muestra dia, hora y margen del modelo`() {
        val antes = Instant.parse("2026-11-18T15:00:00Z").toEpochMilli()
        assertEquals("jue 19/11 · 14:00 (± 4 h)", LlegadaEstimada(jueves14h, 4).texto(antes, lima))
    }

    @Test
    fun `si ya paso la hora estimada mas el margen, avisa la demora`() {
        val despues = jueves14h + 5 * 3_600_000L
        assertEquals("Con demora: la entrega está tomando más de lo estimado", LlegadaEstimada(jueves14h, 4).texto(despues, lima))
    }

    @Test
    fun `el mapper lee la estimacion de la API y la ignora si no viene`() {
        val envio = EnvioDto(1, "u", "RLP-1", "Lima → Callao", 1.0, 10.0, "en_reparto", null, 1)
        val con = SeguimientoDto(envio, null, null, null, null, EstimacionDto("2026-11-19T19:00:00Z", 12.2, 4)).aModelo()
        assertEquals(jueves14h, con.estimacion?.fecha)
        assertNull(SeguimientoDto(envio, null, null, null, null).aModelo().estimacion)
    }
}
