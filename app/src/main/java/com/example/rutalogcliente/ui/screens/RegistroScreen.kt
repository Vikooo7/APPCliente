package com.example.rutalogcliente.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.rutalogcliente.model.RolUsuario
import com.example.rutalogcliente.ui.components.AppScaffold
import com.example.rutalogcliente.ui.components.InputField
import com.example.rutalogcliente.ui.components.MensajeError
import com.example.rutalogcliente.viewmodel.AuthUiState

/** RFA02: registro de un nuevo usuario con rol "cliente". */
@Composable
fun RegistroScreen(
    estado: AuthUiState,
    onRegistrar: (nombre: String, correo: String, clave: String, confirmacion: String) -> Unit,
    onVolver: () -> Unit,
    onLimpiarMensajes: () -> Unit
) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }
    var confirmacion by rememberSaveable { mutableStateOf("") }

    val registrar = { onRegistrar(nombre, correo, clave, confirmacion) }

    AppScaffold(titulo = "Crear cuenta", subtitulo = "App Cliente", onVolver = onVolver) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Crea tu cuenta para registrar envíos y rastrearlos con su número de guía.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ElevatedCard(shape = RoundedCornerShape(24.dp)) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    InputField(
                        valor = nombre,
                        onValorChange = {
                            nombre = it
                            onLimpiarMensajes()
                        },
                        etiqueta = "Nombre o razón social",
                        icono = Icons.Default.Person,
                        mayusculas = KeyboardCapitalization.Words
                    )
                    InputField(
                        valor = correo,
                        onValorChange = {
                            correo = it.trim()
                            onLimpiarMensajes()
                        },
                        etiqueta = "Correo electrónico",
                        icono = Icons.Default.Email,
                        tipoTeclado = KeyboardType.Email
                    )
                    InputField(
                        valor = clave,
                        onValorChange = {
                            clave = it
                            onLimpiarMensajes()
                        },
                        etiqueta = "Contraseña",
                        icono = Icons.Default.Lock,
                        esClave = true,
                        textoAyuda = "Mínimo 4 caracteres"
                    )
                    InputField(
                        valor = confirmacion,
                        onValorChange = {
                            confirmacion = it
                            onLimpiarMensajes()
                        },
                        etiqueta = "Confirmar contraseña",
                        icono = Icons.Default.LockReset,
                        esClave = true,
                        accionIme = ImeAction.Done,
                        onAccionIme = registrar
                    )

                    // El rol no se elige: en la App Cliente siempre es "cliente".
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Rol: ${RolUsuario.CLIENTE.valor}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    AnimatedVisibility(visible = estado.error != null) {
                        MensajeError(texto = estado.error ?: "")
                    }

                    Button(
                        onClick = registrar,
                        enabled = !estado.cargando,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (estado.cargando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Crear cuenta", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
