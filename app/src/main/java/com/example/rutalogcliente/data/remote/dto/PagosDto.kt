package com.example.rutalogcliente.data.remote.dto

/** Fase de pagos con Banco Andino. La app nunca habla con el banco: todo pasa por la API de RutaLog. */

data class PagoQrRequest(val envioId: Int, val tipo: String)

/** Cobro: el pago del envío (tipo "envio") o el contraentrega que cobró el courier (tipo "contraentrega"). */
data class CobroDto(
    val id: String,
    val envioId: Int,
    val numeroGuia: String?,
    val tipo: String,
    val monto: Double,
    /** POR_COBRAR, PENDIENTE_CONFIRMACION, CONFIRMADO, RECHAZADO o LIQUIDADO. */
    val estado: String,
    /** Texto del QR (BANCOANDINO:QR-...); solo mientras está por cobrar. */
    val codigoQr: String?,
    val nroOperacion: String?,
    val motivoRechazo: String?,
    val creadoEn: String?,
    val confirmadoEn: String?,
    val vencido: Boolean? = null,
    val pagoEstadoEnvio: String? = null
)

data class CobrosResponse(val datos: List<CobroDto>)

/** RF-R05: liquidación al remitente (recaudado − costo de envíos), transferida por Banco Andino. */
data class LiquidacionDto(
    val id: Int,
    val periodo: String,
    val recaudado: Double,
    val costoEnvios: Double,
    val neto: Double,
    val cobros: Int?,
    /** CALCULADA o TRANSFERIDA. */
    val estado: String,
    val cuentaDestino: String?,
    val nroOperacion: String?,
    val creadoEn: String?,
    val transferidaEn: String?
)

data class LiquidacionesResponse(val datos: List<LiquidacionDto>)

data class PerfilDto(val id: Int, val nombre: String, val correo: String, val rol: String, val cuentaBanco: String?)

data class CuentaBancoRequest(val cuenta: String)
data class CuentaBancoResponse(val cuentaBanco: String, val titular: String?)
