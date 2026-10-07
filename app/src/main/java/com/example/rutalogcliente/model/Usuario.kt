package com.example.rutalogcliente.model

/** Perfil del usuario con sesión iniciada. La contraseña nunca se guarda en el teléfono. */
data class Usuario(
    val id: Int,
    val nombre: String,
    val correo: String,
    val rol: String
)
