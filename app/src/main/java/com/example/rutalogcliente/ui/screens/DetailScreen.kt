package com.example.rutalogcliente.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rutalogcliente.model.Envio
import com.example.rutalogcliente.model.EstadoSincronizacion
import com.example.rutalogcliente.ui.components.SyncStatusIndicator
import com.example.rutalogcliente.model.CatalogoRutas
import com.example.rutalogcliente.model.esModificable
import com.example.rutalogcliente.model.estadoEnvio
import com.example.rutalogcliente.ui.components.AppScaffold
import com.example.rutalogcliente.ui.components.BannerDegradado
import com.example.rutalogcliente.ui.components.EstadoBadge
import com.example.rutalogcliente.ui.components.LineaDeTiempo
import com.example.rutalogcliente.ui.components.MensajeError
import com.example.rutalogcliente.ui.components.formatoPeso
import com.example.rutalogcliente.ui.components.formatoSoles

/** Seguimiento del envío: estado, línea de tiempo y datos. Permite editar o eliminar si sigue pendiente. */
@Composable
fun DetailScreen(
    envio: Envio?,
    onVolver: () -> Unit,
    onEditar: (Envio) -> Unit,
    onEliminar: (Envio, onError: (String) -> Unit) -> Unit
) {
    var confirmarEliminar by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AppScaffold(titulo = "Seguimiento", subtitulo = envio?.numeroGuia, onVolver = onVolver) {
        if (envio == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@AppScaffold
        }

        val estado = envio.estadoEnvio
        val ruta = CatalogoRutas.porNombre(envio.ruta)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                BannerDegradado {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "N° de guía",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = envio.numeroGuia,
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        EstadoBadge(estado = estado, sobreFondoOscuro = true)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = envio.ruta,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (ruta != null) {
                        Text(
                            text = "${ruta.region} (${ruta.zona}) · ${ruta.distanciaKm} km · ~${ruta.horasEstimadas} h · ${ruta.modalidad}",
                            color = Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                Tarjeta {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timeline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Línea de tiempo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    LineaDeTiempo(estado = estado)
                }
            }

            item {
                Tarjeta {
                    Text(
                        text = "Datos del envío",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilaDato(Icons.Default.Map, "Ruta", envio.ruta)
                        if (ruta != null) {
                            FilaDato(Icons.Default.Place, "Región de destino", "${ruta.region} · zona ${ruta.zona}")
                            FilaDato(Icons.Default.Schedule, "Tiempo estimado de viaje", "~${ruta.horasEstimadas} horas")
                        }
                        FilaDato(Icons.Default.Scale, "Peso", formatoPeso(envio.pesoKg))
                        FilaDato(
                            Icons.Default.Payments,
                            "Costo del envío",
                            if (ruta != null) {
                                "${formatoSoles(envio.costoEnvio)}  (${formatoPeso(envio.pesoKg)} × ${formatoSoles(ruta.tarifaPorKg)}/kg)"
                            } else {
                                formatoSoles(envio.costoEnvio)
                            }
                        )
                        FilaDato(
                            Icons.Default.DirectionsCar,
                            "Transportista asignado",
                            envio.transportistaAsignado ?: "Por asignar"
                        )
                    }
                }
            }

            item {
                Tarjeta {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Sincronización",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        SyncStatusIndicator(estado = envio.estadoSync)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (envio.estadoSync) {
                            EstadoSincronizacion.SINCRONIZADO ->
                                "Este envío está guardado en el servidor (id remoto ${envio.idRemoto}, versión ${envio.version})."
                            EstadoSincronizacion.ERROR ->
                                "El servidor rechazó el último cambio. Ve a la pestaña Sincronizar para reintentar o descartar."
                            else ->
                                "Guardado en el teléfono. Se enviará al servidor cuando haya conexión."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    envio.mensajeError?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        MensajeError(texto = it)
                    }
                }
            }

            item {
                if (envio.esModificable) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        error?.let { MensajeError(texto = it) }
                        OutlinedButton(
                            onClick = { onEditar(envio) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Text("  Editar envío")
                        }
                        Button(
                            onClick = { confirmarEliminar = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Text("  Eliminar envío")
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "La carga ya fue recogida: el envío no se puede editar ni eliminar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (confirmarEliminar) {
            AlertDialog(
                onDismissRequest = { confirmarEliminar = false },
                icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                title = { Text("¿Eliminar envío?") },
                text = { Text("Se eliminará la guía ${envio.numeroGuia} de la base de datos.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            confirmarEliminar = false
                            onEliminar(envio) { error = it }
                        }
                    ) { Text("Eliminar") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmarEliminar = false }) { Text("Cancelar") }
                }
            )
        }
    }
}

@Composable
private fun Tarjeta(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) { content() }
    }
}

@Composable
private fun FilaDato(icono: ImageVector, etiqueta: String, valor: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text = valor, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
