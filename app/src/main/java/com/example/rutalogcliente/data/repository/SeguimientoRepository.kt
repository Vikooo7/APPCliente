package com.example.rutalogcliente.data.repository

import com.example.rutalogcliente.data.mapper.aModelo
import com.example.rutalogcliente.data.remote.ResultadoApi
import com.example.rutalogcliente.data.remote.api.ApiService
import com.example.rutalogcliente.data.remote.llamarApi
import com.example.rutalogcliente.model.Seguimiento

/**
 * HU-R02: seguimiento de una guía con sus eventos (hora y lugar) y la prueba de entrega.
 * Se consulta en línea cada vez: los eventos los registran la operación y el courier en el servidor.
 */
class SeguimientoRepository(private val api: ApiService) {

    /** Bytes de la foto o la firma de la entrega, o null si no se pudo descargar (sin conexión, sin permiso). */
    suspend fun imagen(url: String): ByteArray? = try {
        val respuesta = api.descargarArchivo(url)
        if (respuesta.isSuccessful) respuesta.body()?.use { it.bytes() } else null
    } catch (e: java.io.IOException) {
        null
    }

    suspend fun cargar(numeroGuia: String): Result<Seguimiento> =
        when (val resultado = llamarApi { api.obtenerSeguimiento(numeroGuia) }) {
            is ResultadoApi.Exito -> Result.success(resultado.valor.aModelo())
            is ResultadoApi.Rechazo -> Result.failure(Exception(resultado.mensaje))
            is ResultadoApi.Fallo -> Result.failure(
                Exception(
                    if (resultado.sinConexion) {
                        "Sin conexión: el seguimiento detallado necesita internet."
                    } else {
                        resultado.mensaje
                    }
                )
            )
        }
}
