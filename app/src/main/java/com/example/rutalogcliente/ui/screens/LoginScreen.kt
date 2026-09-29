package com.example.rutalogcliente.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.rutalogcliente.ui.components.AppLogo
import com.example.rutalogcliente.ui.components.InputField
import com.example.rutalogcliente.ui.components.MensajeError
import com.example.rutalogcliente.ui.components.MensajeInfo
import com.example.rutalogcliente.ui.theme.AzulRuta
import com.example.rutalogcliente.ui.theme.AzulRutaClaro
import com.example.rutalogcliente.viewmodel.AuthUiState

/** RFA03 + RFA04: inicio de sesión contra la tabla usuarios. */
@Composable
fun LoginScreen(
    estado: AuthUiState,
    onLogin: (correo: String, clave: String) -> Unit,
    onIrARegistro: () -> Unit,
    onLimpiarMensajes: () -> Unit
) {
    var correo by rememberSaveable(estado.correoSugerido) { mutableStateOf(estado.correoSugerido) }
    var clave by rememberSaveable { mutableStateOf("") }

    val sacudida = remember { Animatable(0f) }
    LaunchedEffect(estado.intentosFallidos) {
        if (estado.intentosFallidos > 0) {
            repeat(3) {
                sacudida.animateTo(12f, tween(50))
                sacudida.animateTo(-12f, tween(50))
            }
            sacudida.animateTo(0f, tween(50))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(Brush.verticalGradient(listOf(AzulRuta, AzulRutaClaro)))
                .statusBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(28.dp),
                    shadowElevation = 8.dp
                ) {
                    AppLogo(
                        ancho = 150.dp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "App Cliente · Registra y rastrea tu carga en todo el Perú.",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = sacudida.value.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Iniciar sesión",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    estado.mensaje?.let { MensajeInfo(texto = it) }

                    InputField(
                        valor = correo,
                        onValorChange = {
                            correo = it.trim()
                            onLimpiarMensajes()
                        },
                        etiqueta = "Correo electrónico",
                        icono = Icons.Default.Email,
                        esError = estado.error != null,
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
                        esError = estado.error != null,
                        accionIme = ImeAction.Done,
                        onAccionIme = { onLogin(correo, clave) }
                    )

                    AnimatedVisibility(visible = estado.error != null) {
                        MensajeError(texto = estado.error ?: "")
                    }

                    Button(
                        onClick = { onLogin(correo, clave) },
                        enabled = !estado.cargando,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        AnimatedContent(targetState = estado.cargando, label = "BotonLogin") { cargando ->
                            if (cargando) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Verificando…")
                                }
                            } else {
                                Text("Ingresar")
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            correo = "cliente@rutalog.pe"
                            clave = "1234"
                            onLimpiarMensajes()
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Usar cuenta demo")
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¿No tienes cuenta?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onIrARegistro) {
                    Text("Regístrate", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
