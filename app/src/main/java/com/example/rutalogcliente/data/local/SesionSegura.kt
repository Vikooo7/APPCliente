package com.example.rutalogcliente.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Guarda el token de sesión cifrado (AES-256) con una clave del Android Keystore.
 * La contraseña del usuario nunca se guarda en el teléfono.
 */
class SesionSegura(context: Context) {

    private val prefs: SharedPreferences = crear(context.applicationContext)

    var token: String?
        get() = prefs.getString(CLAVE_TOKEN, null)
        set(valor) {
            prefs.edit().apply {
                if (valor == null) remove(CLAVE_TOKEN) else putString(CLAVE_TOKEN, valor)
            }.apply()
        }

    fun cerrar() {
        token = null
    }

    private fun crear(context: Context): SharedPreferences = try {
        abrirCifrado(context)
    } catch (e: Exception) {
        // Si la clave del Keystore quedó inválida (por ejemplo, tras restaurar el teléfono),
        // se descarta el archivo y se crea de nuevo: el usuario solo vuelve a iniciar sesión.
        context.deleteSharedPreferences(ARCHIVO)
        abrirCifrado(context)
    }

    private fun abrirCifrado(context: Context): SharedPreferences =
        EncryptedSharedPreferences.create(
            ARCHIVO,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    private companion object {
        const val ARCHIVO = "sesion_segura"
        const val CLAVE_TOKEN = "token"
    }
}
