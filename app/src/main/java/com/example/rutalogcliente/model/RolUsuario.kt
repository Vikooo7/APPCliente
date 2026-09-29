package com.example.rutalogcliente.model

/** Valor que se guarda en la columna "rol" de la tabla usuarios. */
enum class RolUsuario(val valor: String) {
    CLIENTE("cliente"),
    ADMINISTRADOR("administrador"),
    EMPLEADO("empleado")
}
