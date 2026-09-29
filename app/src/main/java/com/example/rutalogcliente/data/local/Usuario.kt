package com.example.rutalogcliente.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla "usuarios" de la App Cliente. Aquí el rol siempre es "cliente".
 *
 * La clave se guarda en texto plano solo por ser una práctica académica;
 * en un proyecto real se guardaría un hash (por ejemplo, con bcrypt o Argon2).
 */
@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val correo: String,
    val clave: String,
    val rol: String
)
