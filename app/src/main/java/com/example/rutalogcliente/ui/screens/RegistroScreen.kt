package com.example.rutalogcliente.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.rutalogcliente.model.ReglasCuenta
import com.example.rutalogcliente.ui.components.ConnectivityBanner
import com.example.rutalogcliente.ui.components.InputField
import com.example.rutalogcliente.ui.components.LocalEnLinea
import com.example.rutalogcliente.ui.components.MensajeError
import com.example.rutalogcliente.ui.theme.AzulRuta
import com.example.rutalogcliente.viewmodel.AuthUiState

/**
 * Crear cuenta de cliente (remitente). La API la crea siempre con rol cliente:
 * los couriers y operadores los registra la empresa.
 */
@Composable
fun RegistroScreen(
    estado: AuthUiState,
    onRegistrar: (nombre: String, correo: String, clave: String, confirmacion: String) -> Unit,
    onIrALogin: () -> Unit,
    onLimpiarMensajes: () -> Unit
) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }
    var confirmacion by rememberSaveable { mutableStateOf("") }
    // Los errores por campo se muestran después del primer intento, no mientras se escribe por primera vez.
    var intentado by rememberSaveable { mutableStateOf(false) }

    val errores = ReglasCuenta.validar(nombre, correo, clave, confirmacion)
    val enviar = {
        intentado = true
        onRegistrar(nombre, correo, clave, confirmacion)
    }
    val cambiar: (String, (String) -> Unit) -> Unit = { valor, asignar ->
        asignar(valor)
        onLimpiarMensajes()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AzulRuta)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onIrALogin) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
            Text(
                text = "Crear cuenta",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        ConnectivityBanner(enLinea = LocalEnLinea.current)

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Regístrate para enviar carga y seguir tus envíos en todo el Perú.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    InputField(
                        valor = nombre,
                        onValorChange = { cambiar(it) { v -> nombre = v } },
                        etiqueta = "Nombre completo",
                        icono = Icons.Default.Person,
                        esError = intentado && errores.nombre != null,
                        textoAyuda = errores.nombre.takeIf { intentado },
                        mayusculas = KeyboardCapitalization.Words
                    )
                    InputField(
                        valor = correo,
                        onValorChange = { cambiar(it.trim()) { v -> correo = v } },
                        etiqueta = "Correo electrónico",
                        icono = Icons.Default.Email,
                        esError = intentado && errores.correo != null,
                        textoAyuda = errores.correo.takeIf { intentado },
                        tipoTeclado = KeyboardType.Email
                    )
                    InputField(
                        valor = clave,
                        onValorChange = { cambiar(it) { v -> clave = v } },
                        etiqueta = "Contraseña",
                        icono = Icons.Default.Lock,
                        esClave = true,
                        esError = intentado && errores.clave != null,
                        textoAyuda = if (intentado && errores.clave != null) {
                            errores.clave
                        } else {
                            "Mínimo ${ReglasCuenta.CLAVE_MINIMA} caracteres, con letras y números."
                        }
                    )
                    InputField(
                        valor = confirmacion,
                        onValorChange = { cambiar(it) { v -> confirmacion = v } },
                        etiqueta = "Repite la contraseña",
                        icono = Icons.Default.LockReset,
                        esClave = true,
                        esError = intentado && errores.confirmacion != null,
                        textoAyuda = errores.confirmacion.takeIf { intentado },
                        accionIme = ImeAction.Done,
                        onAccionIme = enviar
                    )

                    AnimatedVisibility(visible = estado.error != null) {
                        MensajeError(texto = estado.error ?: "")
                    }

                    Button(
                        onClick = enviar,
                        enabled = !estado.cargando,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        AnimatedContent(targetState = estado.cargando, label = "BotonRegistro") { cargando ->
                            if (cargando) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Creando tu cuenta…")
                                }
                            } else {
                                Text("Crear cuenta")
                            }
                        }
                    }
                }
            }

            TextButton(
                onClick = onIrALogin,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .navigationBarsPadding()
            ) {
                Text("¿Ya tienes cuenta? Inicia sesión", textAlign = TextAlign.Center)
            }
        }
    }
}
