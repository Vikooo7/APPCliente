package com.example.rutalogcliente.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.rutalogcliente.model.Envio
import com.example.rutalogcliente.model.EstadoEnvio
import com.example.rutalogcliente.model.estadoEnvio
import com.example.rutalogcliente.ui.components.AppScaffold
import com.example.rutalogcliente.ui.components.BannerDegradado
import com.example.rutalogcliente.ui.components.EstadoVacio
import com.example.rutalogcliente.ui.components.InputField
import com.example.rutalogcliente.ui.components.ItemCard
import com.example.rutalogcliente.ui.components.MensajeError
import com.example.rutalogcliente.ui.components.PestanaCliente

/** Pantalla principal del cliente (RFA05). Incluye el rastreo por número de guía (RF09). */
@Composable
fun HomeScreen(
    nombreUsuario: String,
    envios: List<Envio>,
    errorBusqueda: String?,
    onBuscarGuia: (String) -> Unit,
    onLimpiarErrorBusqueda: () -> Unit,
    onRegistrar: () -> Unit,
    onVerEnvios: () -> Unit,
    onEnvio: (Envio) -> Unit,
    onPestana: (PestanaCliente) -> Unit,
    onCerrarSesion: () -> Unit
) {
    var guia by rememberSaveable { mutableStateOf("") }
    val enCurso = envios.count { it.estadoEnvio != EstadoEnvio.ENTREGADO }
    val entregados = envios.count { it.estadoEnvio == EstadoEnvio.ENTREGADO }

    AppScaffold(
        titulo = "Inicio",
        subtitulo = "Cliente · $nombreUsuario",
        pestana = PestanaCliente.INICIO,
        onPestana = onPestana,
        onCerrarSesion = onCerrarSesion
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                BannerDegradado {
                    Text(
                        text = "Hola,",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = nombreUsuario,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Estadistica(envios.size, "Registrados", Modifier.weight(1f))
                        Estadistica(enCurso, "En curso", Modifier.weight(1f))
                        Estadistica(entregados, "Entregados", Modifier.weight(1f))
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccionRapida(
                        titulo = "Registrar envío",
                        subtitulo = "Genera tu guía al instante",
                        icono = Icons.Default.AddBox,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = onRegistrar,
                        modifier = Modifier.weight(1f)
                    )
                    AccionRapida(
                        titulo = "Mis envíos",
                        subtitulo = "Consulta, edita o elimina",
                        icono = Icons.Default.Inventory,
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = onVerEnvios,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Rastrear por número de guía",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        InputField(
                            valor = guia,
                            onValorChange = {
                                guia = it.uppercase()
                                onLimpiarErrorBusqueda()
                            },
                            etiqueta = "Número de guía",
                            icono = Icons.Default.ConfirmationNumber,
                            esError = errorBusqueda != null,
                            textoAyuda = "Ejemplo: RLP-26-100001-2 (los guiones son opcionales)",
                            mayusculas = KeyboardCapitalization.Characters,
                            accionIme = ImeAction.Search,
                            onAccionIme = { onBuscarGuia(guia) }
                        )
                        AnimatedVisibility(visible = errorBusqueda != null) {
                            MensajeError(texto = errorBusqueda ?: "")
                        }
                        Button(
                            onClick = { onBuscarGuia(guia) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Text("  Buscar envío")
                        }
                        if (envios.isNotEmpty()) {
                            Text(
                                text = "Tus guías",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(envios.take(5), key = { it.id }) { envio ->
                                    SuggestionChip(
                                        onClick = {
                                            guia = envio.numeroGuia
                                            onBuscarGuia(envio.numeroGuia)
                                        },
                                        label = { Text(envio.numeroGuia, fontFamily = FontFamily.Monospace) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Envíos recientes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (envios.isEmpty()) {
                item {
                    EstadoVacio(
                        icono = Icons.Default.Inventory,
                        titulo = "Aún no tienes envíos",
                        mensaje = "Registra tu primer envío y obtén su número de guía.",
                        accion = "Registrar envío",
                        onAccion = onRegistrar
                    )
                }
            }

            items(envios.take(4), key = { it.id }) { envio ->
                ItemCard(envio = envio, onClick = { onEnvio(envio) })
            }
        }
    }
}

@Composable
private fun Estadistica(valor: Int, etiqueta: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = valor.toString(),
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = etiqueta,
            color = Color.White.copy(alpha = 0.75f),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun AccionRapida(
    titulo: String,
    subtitulo: String,
    icono: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icono, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
