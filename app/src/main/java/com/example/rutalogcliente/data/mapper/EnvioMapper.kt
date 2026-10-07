package com.example.rutalogcliente.data.mapper

import com.example.rutalogcliente.data.local.entities.EnvioEntity
import com.example.rutalogcliente.data.local.entities.UsuarioEntity
import com.example.rutalogcliente.data.remote.dto.EnvioDto
import com.example.rutalogcliente.data.remote.dto.UsuarioDto
import com.example.rutalogcliente.model.Envio
import com.example.rutalogcliente.model.EstadoSincronizacion
import com.example.rutalogcliente.model.Usuario

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
    mensajeError = mensajeError
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
    mensajeError = null
)

fun UsuarioDto.aEntidad(): UsuarioEntity = UsuarioEntity(id = id, nombre = nombre, correo = correo, rol = rol)

fun UsuarioEntity.aModelo(): Usuario = Usuario(id = id, nombre = nombre, correo = correo, rol = rol)
