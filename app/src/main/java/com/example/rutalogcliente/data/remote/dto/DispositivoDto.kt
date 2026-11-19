package com.example.rutalogcliente.data.remote.dto

/** Token de Firebase Cloud Messaging de este celular y la app que lo usa. */
data class DispositivoRequest(val token: String, val app: String = "cliente")

/** pushActivo = false si el servidor todavía no tiene las credenciales de Firebase (los avisos llegan al sincronizar). */
data class DispositivoResponse(val registrado: Boolean?, val pushActivo: Boolean?)
