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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.rutalogcliente.model.Envio
import com.example.rutalogcliente.model.CatalogoRutas
import com.example.rutalogcliente.model.RutaTarifa
import com.example.rutalogcliente.ui.components.AppScaffold
import com.example.rutalogcliente.ui.components.InputField
import com.example.rutalogcliente.ui.components.MensajeError
import com.example.rutalogcliente.ui.components.PestanaCliente
import com.example.rutalogcliente.ui.components.SelectorDesplegable
import com.example.rutalogcliente.ui.components.formatoPeso
import com.example.rutalogcliente.ui.components.formatoSoles
import com.example.rutalogcliente.viewmodel.EnvioViewModel

/**
 * Crear (RF06, RF07, RF08, RF10) o editar un envío.
 * Si [envioEditado] es null, es un registro nuevo.
 */
@Composable
fun FormScreen(
    envioViewModel: EnvioViewModel,
    envioEditado: Envio?,
    esEdicion: Boolean,
    nombreUsuario: String,
    onVolver: () -> Unit,
    onPestana: (PestanaCliente) -> Unit,
    onCerrarSesion: () -> Unit,
    onVerSeguimiento: (Envio) -> Unit
) {
    var ruta by remember { mutableStateOf<RutaTarifa?>(null) }
    var peso by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }
    var registrado by remember { mutableStateOf<Envio?>(null) }

    // En edición, carga una sola vez los valores guardados en Room.
    LaunchedEffect(envioEditado?.id) {
        if (envioEditado != null) {
            ruta = CatalogoRutas.porNombre(envioEditado.ruta)
            peso = envioEditado.pesoKg.toString()
        }
    }

    val pesoNumero = envioViewModel.leerPeso(peso)
    val costo = if (ruta != null && pesoNumero != null && pesoNumero > 0) {
        envioViewModel.calcularCosto(pesoNumero, ruta!!)
    } else {
        null
    }

    val guardar = {
        error = null
        guardando = true
        if (esEdicion && envioEditado != null) {
            envioViewModel.actualizar(
                envio = envioEditado,
                ruta = ruta,
                pesoTexto = peso,
                onError = {
                    error = it
                    guardando = false
                },
                onActualizado = {
                    guardando = false
                    onVolver()
                }
            )
        } else {
            envioViewModel.registrar(
                ruta = ruta,
                pesoTexto = peso,
                onError = {
                    error = it
                    guardando = false
                },
                onRegistrado = {
                    guardando = false
                    registrado = it
                }
            )
        }
    }

    AppScaffold(
        titulo = if (esEdicion) "Editar envío" else "Registrar envío",
        subtitulo = if (esEdicion) envioEditado?.numeroGuia else "Cliente · $nombreUsuario",
        pestana = if (esEdicion) null else PestanaCliente.REGISTRAR,
        onPestana = onPestana,
        onVolver = if (esEdicion) onVolver else null,
        onCerrarSesion = if (esEdicion) null else onCerrarSesion
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (esEdicion) {
                    "Puedes cambiar la ruta o el peso mientras el envío siga pendiente de recojo."
                } else {
                    "Elige la ruta e indica el peso. El número de guía se genera al guardar."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Seccion(titulo = "Ruta", icono = Icons.Default.Map) {
                SelectorDesplegable(
                    etiqueta = "Ruta del envío (25 regiones)",
                    opciones = CatalogoRutas.rutas,
                    seleccion = ruta,
                    texto = { it?.nombre ?: "Selecciona una ruta" },
                    textoOpcion = { "${it?.nombre} · ${it?.region} · ${formatoSoles(it?.tarifaPorKg ?: 0.0)}/kg" },
                    onSeleccion = {
                        ruta = it
                        error = null
                    },
                    icono = Icons.Default.Map
                )
                ruta?.let { seleccionada ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)) {
                            Text(
                                text = "${seleccionada.region} · zona ${seleccionada.zona}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${seleccionada.distanciaKm} km · ~${seleccionada.horasEstimadas} h · " +
                                    "${seleccionada.modalidad} · tarifa ${formatoSoles(seleccionada.tarifaPorKg)}/kg",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Seccion(titulo = "Carga", icono = Icons.Default.Scale) {
                InputField(
                    valor = peso,
                    onValorChange = { valor ->
                        peso = valor.filter { it.isDigit() || it == '.' || it == ',' }.take(8)
                        error = null
                    },
                    etiqueta = "Peso",
                    icono = Icons.Default.Scale,
                    sufijo = "kg",
                    esError = error != null && (pesoNumero == null || pesoNumero <= 0),
                    textoAyuda = "Debe ser mayor que 0 kg",
                    tipoTeclado = KeyboardType.Decimal,
                    accionIme = ImeAction.Done,
                    onAccionIme = { guardar() }
                )
            }

            // RF08: costo = peso × tarifa por kg de la ruta
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Costo del envío",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = if (costo != null && pesoNumero != null && ruta != null) {
                                "${formatoPeso(pesoNumero)} × ${formatoSoles(ruta!!.tarifaPorKg)}/kg"
                            } else {
                                "Elige la ruta e ingresa el peso"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Text(
                        text = costo?.let { formatoSoles(it) } ?: "S/ --",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            AnimatedVisibility(visible = error != null) {
                MensajeError(texto = error ?: "")
            }

            Button(
                onClick = { guardar() },
                enabled = !guardando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(
                    imageVector = if (esEdicion) Icons.Default.Save else Icons.Default.ConfirmationNumber,
                    contentDescription = null
                )
                Text(if (esEdicion) "  Guardar cambios" else "  Registrar y generar guía")
            }
        }
    }

    registrado?.let { envio ->
        AlertDialog(
            onDismissRequest = {},
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
            },
            title = { Text("¡Envío registrado!") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Tu número de guía es:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = envio.numeroGuia,
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${envio.ruta} · ${formatoPeso(envio.pesoKg)} · ${formatoSoles(envio.costoEnvio)}",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        registrado = null
                        ruta = null
                        peso = ""
                        onVerSeguimiento(envio)
                    }
                ) { Text("Ver seguimiento") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        registrado = null
                        ruta = null
                        peso = ""
                    }
                ) { Text("Registrar otro") }
            }
        )
    }
}

@Composable
private fun Seccion(
    titulo: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            content()
        }
    }
}
