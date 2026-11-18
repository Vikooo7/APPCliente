package com.example.rutalogcliente.data.repository

import android.content.Context
import com.example.rutalogcliente.data.remote.ResultadoApi
import com.example.rutalogcliente.data.remote.api.ApiService
import com.example.rutalogcliente.data.remote.dto.CobroDto
import com.example.rutalogcliente.data.remote.dto.CuentaBancoRequest
import com.example.rutalogcliente.data.remote.dto.CuentaBancoResponse
import com.example.rutalogcliente.data.remote.dto.LiquidacionDto
import com.example.rutalogcliente.data.remote.dto.PagoQrRequest
import com.example.rutalogcliente.data.remote.dto.PerfilDto
import com.example.rutalogcliente.data.remote.llamarApi

/**
 * Pagos del remitente con Banco Andino: pagar el envío con QR (RF-R02), ver los cobros contraentrega
 * (RF-R04) y las liquidaciones (RF-R05). Todo es en línea: el dinero lo confirma el banco, no el teléfono.
 */
class PagoRepository(private val api: ApiService, context: Context) {

    private val avisadas = context.getSharedPreferences("liquidaciones_avisadas", Context.MODE_PRIVATE)

    suspend fun crearQrEnvio(idRemoto: Int): Result<CobroDto> = aResult(llamarApi { api.crearPagoQr(PagoQrRequest(idRemoto, "envio")) })

    suspend fun consultar(idCobro: String): Result<CobroDto> = aResult(llamarApi { api.consultarPago(idCobro) })

    suspend fun cobros(): Result<List<CobroDto>> = aResult(llamarApi { api.obtenerCobros() }).map { it.datos }

    suspend fun liquidaciones(): Result<List<LiquidacionDto>> = aResult(llamarApi { api.obtenerLiquidaciones() }).map { it.datos }

    suspend fun perfil(): Result<PerfilDto> = aResult(llamarApi { api.obtenerPerfil() })

    suspend fun guardarCuenta(cuenta: String): Result<CuentaBancoResponse> =
        aResult(llamarApi { api.guardarCuentaBanco(CuentaBancoRequest(cuenta.trim())) })

    /**
     * Liquidaciones transferidas que todavía no se avisaron en el celular (RF-R05: "recibo una notificación
     * con el número de operación del banco"). La primera vez solo se anotan, para no avisar lo antiguo.
     */
    suspend fun liquidacionesPorAvisar(): List<LiquidacionDto> {
        val transferidas = liquidaciones().getOrNull()?.filter { it.estado == "TRANSFERIDA" } ?: return emptyList()
        val vistas = avisadas.getStringSet(CLAVE, null)
        val nuevas = if (vistas == null) emptyList() else transferidas.filter { it.id.toString() !in vistas }
        avisadas.edit().putStringSet(CLAVE, transferidas.map { it.id.toString() }.toSet() + vistas.orEmpty()).apply()
        return nuevas
    }

    private fun <T> aResult(resultado: ResultadoApi<T>): Result<T> = when (resultado) {
        is ResultadoApi.Exito -> Result.success(resultado.valor)
        is ResultadoApi.Rechazo -> Result.failure(Exception(resultado.mensaje))
        is ResultadoApi.Fallo -> Result.failure(
            Exception(if (resultado.sinConexion) "Sin conexión: los pagos se confirman en línea con Banco Andino." else resultado.mensaje)
        )
    }

    private companion object {
        const val CLAVE = "ids"
    }
}
