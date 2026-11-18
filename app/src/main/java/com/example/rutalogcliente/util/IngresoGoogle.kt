package com.example.rutalogcliente.util

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.rutalogcliente.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Inicio con Google: Credential Manager muestra las cuentas del teléfono, Firebase valida la elegida
 * y entrega un ID token que la API de RutaLog verifica. La sesión de la app la sigue dando la API;
 * por eso se cierra la de Firebase apenas se obtiene el token.
 */
object IngresoGoogle {

    /** El usuario cerró la ventana de Google: no es un error que haya que mostrar. */
    class Cancelado : Exception()

    class Fallo(mensaje: String) : Exception(mensaje)

    suspend fun obtenerToken(context: Context): String {
        val opcion = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // muestra todas las cuentas del teléfono
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .build()
        val solicitud = GetCredentialRequest.Builder().addCredentialOption(opcion).build()

        val credencial = try {
            CredentialManager.create(context).getCredential(context, solicitud).credential
        } catch (e: GetCredentialCancellationException) {
            throw Cancelado()
        } catch (e: NoCredentialException) {
            throw Fallo("No hay cuentas de Google en este teléfono. Agrega una en Ajustes.")
        } catch (e: GetCredentialException) {
            throw Fallo("Google no pudo iniciar sesión. Intenta de nuevo.")
        }
        if (credencial !is CustomCredential ||
            credencial.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw Fallo("La cuenta elegida no es de Google.")
        }
        val googleId = GoogleIdTokenCredential.createFrom(credencial.data)

        val auth = FirebaseAuth.getInstance()
        return try {
            val usuario = auth.signInWithCredential(GoogleAuthProvider.getCredential(googleId.idToken, null))
                .await().user ?: throw Fallo("Google no devolvió la cuenta.")
            usuario.getIdToken(true).await().token ?: throw Fallo("Google no devolvió la cuenta.")
        } catch (e: FirebaseNetworkException) {
            throw Fallo("Sin conexión. Necesitas internet para iniciar sesión con Google.")
        } finally {
            auth.signOut()
        }
    }
}
