package com.example.rutalogcliente.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Perfil del usuario autorizado por la API. El id es el del servidor.
 * No se guarda la contraseña: la sesión es un token cifrado (ver SesionSegura).
 */
@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey val id: Int,
    val nombre: String,
    val correo: String,
    val rol: String
)
