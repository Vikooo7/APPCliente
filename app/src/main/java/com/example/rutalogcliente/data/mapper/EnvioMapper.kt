package com.example.rutalogcliente.data.mapper

import com.example.rutalogcliente.data.local.entities.EnvioEntity
import com.example.rutalogcliente.data.local.entities.UsuarioEntity
import com.example.rutalogcliente.data.remote.dto.EnvioDto
import com.example.rutalogcliente.data.remote.dto.SeguimientoDto
import com.example.rutalogcliente.data.remote.dto.UsuarioDto
import com.example.rutalogcliente.model.AvisoDestinatario
import com.example.rutalogcliente.model.Destinatario
import com.example.rutalogcliente.model.Envio
import com.example.rutalogcliente.model.EstadoEnvio
import com.example.rutalogcliente.model.EstadoSincronizacion
import com.example.rutalogcliente.model.EventoSeguimiento
import com.example.rutalogcliente.model.IntentoEntrega
import com.example.rutalogcliente.model.LlegadaEstimada
import com.example.rutalogcliente.model.PruebaEntrega
import com.example.rutalogcliente.model.Seguimiento
import com.example.rutalogcliente.model.Usuario
import java.time.Instant

/** Room → modelo de la app. */
fun EnvioEntity.aModelo(): Envio = Envio(
    id = id,
    idRemoto = idRemoto,
    uuid = uuid,
    numeroGuia = numeroGuia,
    ruta = ruta,
    pesoKg = pesoKg,
    costoEnvio = costoEnvio,
    estado = estado,
    transportistaAsignado = transportistaAsignado,
    version = version,
    estadoSync = EstadoSincronizacion.desde(estadoSync),
    mensajeError = mensajeError,
    destinatario = if (destinatarioNombre != null && destinatarioDireccion != null) {
        Destinatario(destinatarioNombre, destinatarioTelefono, destinatarioDireccion)
    } else {
        null
    },
    montoContraentrega = montoContraentrega,
    intentos = intentos,
    pagoEstado = pagoEstado,
    guiaAnterior = guiaAnterior
)

/**
 * API → Room. Lo que llega del servidor queda como SINCRONIZADO.
 * [idLocal] es 0 para insertar una fila nueva o el id de la fila local que se actualiza.
 */
fun EnvioDto.aEntidad(idLocal: Int = 0): EnvioEntity = EnvioEntity(
    id = idLocal,
    idRemoto = id,
    uuid = uuid,
    numeroGuia = numeroGuia,
    ruta = ruta,
    pesoKg = pesoKg,
    costoEnvio = costoEnvio,
    estado = estado,
    transportistaAsignado = transportistaAsignado,
    version = version,
    estadoSync = EstadoSincronizacion.SINCRONIZADO.name,
    eliminadoLocal = false,
    mensajeError = null,
    destinatarioNombre = destinatarioNombre,
    destinatarioTelefono = destinatarioTelefono,
    destinatarioDireccion = destinatarioDireccion,
    montoContraentrega = montoContraentrega ?: 0.0,
    intentos = intentos ?: 0,
    pagoEstado = pagoEstado ?: "NO_REQUERIDO"
)

private fun milisegundos(fecha: String?): Long? =
    fecha?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

/** API → modelo. El seguimiento se consulta en línea: no se guarda en Room. */
fun SeguimientoDto.aModelo(): Seguimiento = Seguimiento(
    eventos = eventos.orEmpty().map {
        EventoSeguimiento(EstadoEnvio.desde(it.estado), milisegundos(it.fechaHora), it.latitud, it.longitud)
    },
    intentos = intentosFallidos.orEmpty().map { IntentoEntrega(it.numero, it.motivo, milisegundos(it.fecha)) },
    prueba = pruebaEntrega?.let {
        PruebaEntrega(
            recibeNombre = it.recibeNombre.orEmpty(),
            recibeDni = it.recibeDni.orEmpty(),
            fechaHora = milisegundos(it.fechaHora),
            fotoUrl = it.fotoUrl,
            firmaUrl = it.firmaUrl
        )
    },
    avisos = avisos.orEmpty().mapNotNull { aviso ->
        aviso.mensaje?.let { AvisoDestinatario(it, aviso.destino.orEmpty(), milisegundos(aviso.enviadoEn)) }
    },
    estimacion = estimacion?.let { e ->
        milisegundos(e.fechaEstimada)?.let { LlegadaEstimada(it, e.margenHoras ?: 0) }
    }
)

fun UsuarioDto.aEntidad(): UsuarioEntity = UsuarioEntity(id = id, nombre = nombre, correo = correo, rol = rol)

fun UsuarioEntity.aModelo(): Usuario = Usuario(id = id, nombre = nombre, correo = correo, rol = rol)
