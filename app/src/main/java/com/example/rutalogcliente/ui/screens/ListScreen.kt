package com.example.rutalogcliente.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rutalogcliente.model.Envio
import com.example.rutalogcliente.model.EstadoEnvio
import com.example.rutalogcliente.ui.components.AppScaffold
import com.example.rutalogcliente.ui.components.EstadoVacio
import com.example.rutalogcliente.ui.components.ItemCard
import com.example.rutalogcliente.ui.components.PestanaCliente
import com.example.rutalogcliente.ui.components.color
import com.example.rutalogcliente.ui.components.icono

/** Lista de envíos guardados en Room (lectura del CRUD). */
@Composable
fun ListScreen(
    nombreUsuario: String,
    envios: List<Envio>,
    totalEnvios: Int,
    texto: String,
    filtroEstado: EstadoEnvio?,
    onTexto: (String) -> Unit,
    onEstado: (EstadoEnvio?) -> Unit,
    onEnvio: (Envio) -> Unit,
    onRegistrar: () -> Unit,
    onPestana: (PestanaCliente) -> Unit,
    onCerrarSesion: () -> Unit
) {
    AppScaffold(
        titulo = "Mis envíos",
        subtitulo = "Cliente · $nombreUsuario",
        pestana = PestanaCliente.MIS_ENVIOS,
        onPestana = onPestana,
        onCerrarSesion = onCerrarSesion
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = texto,
                    onValueChange = onTexto,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    placeholder = { Text("Buscar por guía o ruta") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (texto.isNotEmpty()) {
                            IconButton(onClick = { onTexto("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Borrar búsqueda")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = filtroEstado == null,
                            onClick = { onEstado(null) },
                            label = { Text("Todos") }
                        )
                    }
                    items(EstadoEnvio.entries) { estado ->
                        FilterChip(
                            selected = filtroEstado == estado,
                            onClick = { onEstado(if (filtroEstado == estado) null else estado) },
                            label = { Text(estado.etiqueta) },
                            leadingIcon = {
                                Icon(
                                    imageVector = estado.icono,
                                    contentDescription = null,
                                    tint = estado.color,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (envios.size == totalEnvios) {
                            "$totalEnvios envíos guardados"
                        } else {
                            "${envios.size} de $totalEnvios envíos"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (envios.isEmpty()) {
                item {
                    if (totalEnvios == 0) {
                        EstadoVacio(
                            icono = Icons.Default.Inventory,
                            titulo = "Aún no tienes envíos",
                            mensaje = "Registra tu primer envío y obtén su número de guía.",
                            accion = "Registrar envío",
                            onAccion = onRegistrar
                        )
                    } else {
                        EstadoVacio(
                            icono = Icons.Default.Search,
                            titulo = "Sin resultados",
                            mensaje = "Ningún envío coincide con la búsqueda o el filtro."
                        )
                    }
                }
            }

            items(envios, key = { it.id }) { envio ->
                ItemCard(
                    envio = envio,
                    onClick = { onEnvio(envio) },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .animateItem()
                )
            }
        }
    }
}
