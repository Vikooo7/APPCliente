package com.example.rutalogcliente

import com.example.rutalogcliente.model.DestinatarioForm
import com.example.rutalogcliente.model.EstadoEnvio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** HU-R01: reglas del destinatario y del cobro contraentrega (las mismas de la API). */
class ReglasEnvioTest {

    private val valido = DestinatarioForm(
        nombre = "Carlos Mendoza",
        telefono = "987654321",
        direccion = "Av. Huancavelica 512, El Tambo"
    )

    @Test
    fun destinatarioCompleto_sinContraentrega_esValido() {
        assertNull(valido.validar())
        assertEquals(0.0, valido.montoContraentrega(), 0.0)
    }

    @Test
    fun faltanDatosDelDestinatario() {
        assertNotNull(valido.copy(nombre = " ").validar())
        assertNotNull(valido.copy(telefono = "").validar())
        assertNotNull(valido.copy(direccion = "").validar())
    }

    @Test
    fun elCelularTiene9DigitosYEmpiezaCon9() {
        assertNotNull(valido.copy(telefono = "12345678").validar())
        assertNotNull(valido.copy(telefono = "812345678").validar())
        assertNull(valido.copy(telefono = "912345678").validar())
    }

    @Test
    fun contraentrega_exigeUnMontoValido() {
        val cobro = valido.copy(cobrarContraentrega = true)
        assertNotNull(cobro.copy(monto = "").validar())
        assertNotNull(cobro.copy(monto = "abc").validar())
        assertNotNull(cobro.copy(monto = "0").validar())
        assertNotNull(cobro.copy(monto = "20000").validar())
        assertNull(cobro.copy(monto = "189,456").validar())
        assertEquals(189.46, cobro.copy(monto = "189,456").montoContraentrega(), 0.0001)
    }

    @Test
    fun sinActivarElCobro_elMontoNoCuenta() {
        assertEquals(0.0, valido.copy(cobrarContraentrega = false, monto = "50").montoContraentrega(), 0.0)
    }

    @Test
    fun estadosNuevos_seReconocen() {
        assertEquals(EstadoEnvio.FALLIDO, EstadoEnvio.desde("fallido"))
        assertEquals(EstadoEnvio.DEVUELTO, EstadoEnvio.desde("devuelto"))
        assertEquals(EstadoEnvio.PENDIENTE, EstadoEnvio.desde("desconocido"))
    }
}
