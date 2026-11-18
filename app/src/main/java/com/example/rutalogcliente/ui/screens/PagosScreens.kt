package com.example.rutalogcliente.ui.screens

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.rutalogcliente.data.remote.dto.CobroDto
import com.example.rutalogcliente.data.remote.dto.LiquidacionDto
import com.example.rutalogcliente.model.Envio
import com.example.rutalogcliente.ui.components.AppScaffold
import com.example.rutalogcliente.ui.components.MensajeError
import com.example.rutalogcliente.ui.components.MensajeInfo
import com.example.rutalogcliente.ui.components.QrCodigo
import com.example.rutalogcliente.ui.components.formatoFechaHora
import com.example.rutalogcliente.ui.components.formatoSoles
import com.example.rutalogcliente.viewmodel.MiDineroUi
import com.example.rutalogcliente.viewmodel.PagoEnvioUi
import java.time.Instant

private val VerdeOk = Color(0xFF2E7D32)
private val Ambar = Color(0xFFB26A00)
private val RojoMal = Color(0xFFC62828)

private fun fechaIso(iso: String?): String? = iso?.let { runCatching { formatoFechaHora(Instant.parse(it).toEpochMilli()) }.getOrNull() }

/**
 * RF-R02: pagar el costo del envío con QR de Banco Andino. Se muestra el QR (para pagar desde otro teléfono)
 * y un botón que abre la app Banco Andino en este mismo teléfono. La API confirma el pago con el banco.
 */
@Composable
fun PagoEnvioScreen(
    envio: Envio?,
    estado: PagoEnvioUi,
    onIniciar: (Envio) -> Unit,
    onDetener: () -> Unit,
    onVolver: () -> Unit
) {
    LaunchedEffect(envio?.id) { envio?.let(onIniciar) }
    DisposableEffect(Unit) { onDispose { onDetener() } }
    val context = LocalContext.current

    AppScaffold(titulo = "Pagar envío", subtitulo = envio?.numeroGuia, onVolver = onVolver) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val cobro = estado.cobro
            when {
                estado.cargando -> CircularProgressIndicator()
                estado.confirmado && cobro != null -> PagoConfirmado(cobro, onVolver)
                cobro != null -> {
                    Text("Costo del envío", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatoSoles(cobro.monto), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    if (estado.vencido) {
                        MensajeError(texto = "Este QR venció sin pagarse. Genera uno nuevo.")
                        Button(onClick = { envio?.let(onIniciar) }) { Text("Generar nuevo QR") }
                    } else {
                        cobro.codigoQr?.let { texto ->
                            QrCodigo(texto = texto, descripcion = "QR de pago de ${formatoSoles(cobro.monto)}", tamano = 220.dp)
                            val codigo = texto.removePrefix("BANCOANDINO:")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(codigo, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                                TextButton(onClick = { copiar(context, codigo) }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(" Copiar")
                                }
                            }
                            Button(
                                onClick = { abrirBancoAndino(context, codigo) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Pagar con la app Banco Andino")
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Esperando la confirmación de Banco Andino…", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            "Desde otro teléfono: abre Banco Andino → Pagar con QR y escanea este código. " +
                                "El envío queda pagado cuando el banco lo confirma; recién entonces se asigna al courier.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            estado.error?.let { MensajeError(texto = it) }
        }
    }
}

@Composable
private fun PagoConfirmado(cobro: CobroDto, onVolver: () -> Unit) {
    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VerdeOk, modifier = Modifier.size(72.dp))
    Text("Envío pagado", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(formatoSoles(cobro.monto), style = MaterialTheme.typography.titleLarge)
    Text("Operación ${cobro.nroOperacion}", fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
    Text(
        "Banco Andino confirmó el pago. Tu envío ya puede asignarse a un courier para el recojo.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
    )
    Button(onClick = onVolver) { Text("Listo") }
}

/** Abre la app Banco Andino directo en la confirmación del pago (enlace bancoandino://pagar). */
private fun abrirBancoAndino(context: Context, codigo: String) {
    try {
        // Tarea propia: al volver a RutaLog se ve RutaLog, no la billetera (hallado al probar en el celular).
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("bancoandino://pagar?codigo=$codigo")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: ActivityNotFoundException) {
        copiar(context, codigo)
        android.widget.Toast.makeText(context, "Instala Banco Andino. Copiamos el código para que lo pegues.", android.widget.Toast.LENGTH_LONG).show()
    }
}

private fun copiar(context: Context, texto: String) {
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Código de pago", texto))
}

/** "Mi dinero": cuenta para liquidaciones, cobros contraentrega (RF-R04) y liquidaciones (RF-R05). */
@Composable
fun MiDineroScreen(
    estado: MiDineroUi,
    onCargar: () -> Unit,
    onGuardarCuenta: (String) -> Unit,
    onVolver: () -> Unit
) {
    LaunchedEffect(Unit) { onCargar() }
    var cuenta by rememberSaveable(estado.perfil?.cuentaBanco) { mutableStateOf(estado.perfil?.cuentaBanco.orEmpty()) }

    AppScaffold(titulo = "Mi dinero", subtitulo = "Contraentrega y liquidaciones", onVolver = onVolver) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Lo que cobramos por ti en la puerta se transfiere a tu cuenta los martes y viernes: recaudado menos el costo de tus envíos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (estado.cargando) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = onCargar) { Icon(Icons.Default.Refresh, contentDescription = "Actualizar") }
                    }
                }
            }
            estado.error?.let { item { MensajeError(texto = it) } }

            item {
                Tarjeta("Tu cuenta Banco Andino") {
                    OutlinedTextField(
                        value = cuenta,
                        onValueChange = { cuenta = it.trim() },
                        label = { Text("N.º de cuenta (191-XXXXXXX-X)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { onGuardarCuenta(cuenta) }, enabled = !estado.guardandoCuenta && cuenta.isNotBlank()) {
                        Text(if (estado.guardandoCuenta) "Verificando con el banco…" else "Guardar cuenta")
                    }
                    estado.mensajeCuenta?.let { MensajeInfo(texto = it) }
                    estado.errorCuenta?.let { MensajeError(texto = it) }
                }
            }

            item { Text("Liquidaciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (estado.liquidaciones.isEmpty()) {
                item { Text("Aún no tienes liquidaciones.", style = MaterialTheme.typography.bodySmall) }
            }
            items(estado.liquidaciones, key = { "liq-${it.id}" }) { FilaLiquidacion(it) }

            item { Text("Cobros contraentrega", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (estado.cobros.isEmpty()) {
                item { Text("Aún no hay cobros en la puerta.", style = MaterialTheme.typography.bodySmall) }
            }
            items(estado.cobros, key = { "cobro-${it.id}" }) { FilaCobro(it) }
        }
    }
}

@Composable
private fun FilaLiquidacion(l: LiquidacionDto) {
    val transferida = l.estado == "TRANSFERIDA"
    Tarjeta(null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Corte ${l.periodo}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatoSoles(l.neto), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Recaudado ${formatoSoles(l.recaudado)} − envíos ${formatoSoles(l.costoEnvios)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Chip(if (transferida) "TRANSFERIDA" else "EN PROCESO", if (transferida) VerdeOk else Ambar)
        }
        if (transferida) {
            Text(
                "Operación ${l.nroOperacion} · ${fechaIso(l.transferidaEn).orEmpty()}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun FilaCobro(c: CobroDto) {
    val (texto, color) = when (c.estado) {
        "CONFIRMADO" -> "CONFIRMADO" to VerdeOk
        "LIQUIDADO" -> "LIQUIDADO" to MaterialTheme.colorScheme.primary
        "PENDIENTE_CONFIRMACION" -> "POR CONFIRMAR" to Ambar
        "RECHAZADO" -> "RECHAZADO" to RojoMal
        else -> c.estado to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Tarjeta(null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(c.numeroGuia.orEmpty(), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                Text(formatoSoles(c.monto), style = MaterialTheme.typography.titleMedium)
                Text(
                    listOfNotNull(c.nroOperacion?.let { "Op. $it" }, fechaIso(c.confirmadoEn ?: c.creadoEn)).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                c.motivoRechazo?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = RojoMal) }
            }
            Chip(texto, color)
        }
    }
}

@Composable
private fun Chip(texto: String, color: Color) {
    Surface(color = color.copy(alpha = 0.14f), shape = RoundedCornerShape(50)) {
        Text(texto, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
private fun Tarjeta(titulo: String?, contenido: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            titulo?.let { Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            contenido()
        }
    }
}
