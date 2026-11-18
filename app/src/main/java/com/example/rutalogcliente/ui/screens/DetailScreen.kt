package com.example.rutalogcliente.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TravelExplore
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.rutalogcliente.model.Envio
import com.example.rutalogcliente.model.EstadoEnvio
import com.example.rutalogcliente.model.EstadoSincronizacion
import com.example.rutalogcliente.model.MotivoFalla
import com.example.rutalogcliente.ui.components.MensajeInfo
import com.example.rutalogcliente.ui.components.QrGuia
import com.example.rutalogcliente.ui.components.color
import com.example.rutalogcliente.ui.components.formatoFechaHora
import com.example.rutalogcliente.ui.components.icono
import com.example.rutalogcliente.viewmodel.SeguimientoUiState
import java.util.Locale
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

/**
 * Seguimiento del envío (HU-R02): estado, línea de tiempo con la hora real de cada paso, eventos
 * con lugar, intentos fallidos y prueba de entrega. Permite editar o eliminar si sigue pendiente.
 */
@Composable
fun DetailScreen(
    envio: Envio?,
    seguimiento: SeguimientoUiState,
    onCargarSeguimiento: (Envio) -> Unit,
    cargarImagen: suspend (url: String) -> ImageBitmap?,
    onVolver: () -> Unit,
    onEditar: (Envio) -> Unit,
    onPagar: (Envio) -> Unit,
    onEliminar: (Envio, onError: (String) -> Unit) -> Unit
) {
    var confirmarEliminar by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Se vuelve a pedir cuando cambia el estado o la versión (por ejemplo, tras sincronizar).
    LaunchedEffect(envio?.numeroGuia, envio?.estado, envio?.version, envio?.idRemoto) {
        envio?.let(onCargarSeguimiento)
    }
    val datosSeguimiento = seguimiento.seguimiento.takeIf { seguimiento.guia == envio?.numeroGuia }
    // Hora del último evento de cada estado, para mostrarla en la línea de tiempo.
    val horas = datosSeguimiento?.eventos.orEmpty()
        .filter { it.fechaHora != null }
        .associate { it.estado to formatoFechaHora(it.fechaHora!!) }

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
                    LineaDeTiempo(estado = estado, horas = horas)
                }
            }

            // RF-R02: el envío nuevo se paga con QR antes del recojo.
            if (envio.porPagar || (envio.idRemoto == null && estado == EstadoEnvio.PENDIENTE)) {
                item {
                    Tarjeta {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pago del envío", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (envio.idRemoto == null) {
                            MensajeInfo(texto = "Podrás pagarlo con QR en cuanto el envío se sincronice.")
                        } else {
                            Text(
                                "Paga ${formatoSoles(envio.costoEnvio)} con Banco Andino. Recién con la confirmación del banco el envío se asigna al courier.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(onClick = { onPagar(envio) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Pagar ${formatoSoles(envio.costoEnvio)} con QR")
                            }
                        }
                    }
                }
            }

            // HU-C02: mientras espera el recojo, el remitente muestra o imprime este QR; el courier lo escanea.
            if (estado == EstadoEnvio.PENDIENTE) {
                item {
                    Tarjeta {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCode2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Guía para el recojo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            QrGuia(numeroGuia = envio.numeroGuia)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Pega la guía en el paquete o muestra este QR al courier: lo escanea al recogerlo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            item {
                TarjetaSeguimiento(
                    envio = envio,
                    estado = seguimiento.takeIf { it.guia == envio.numeroGuia } ?: SeguimientoUiState(),
                    onActualizar = { onCargarSeguimiento(envio) },
                    cargarImagen = cargarImagen
                )
            }

            envio.destinatario?.let { destinatario ->
                item {
                    Tarjeta {
                        Text(
                            text = "Destinatario",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilaDato(Icons.Default.Person, "Nombre", destinatario.nombre)
                            destinatario.telefono?.let { FilaDato(Icons.Default.Phone, "Celular", it) }
                            FilaDato(Icons.Default.Home, "Dirección", destinatario.direccion)
                        }
                    }
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
                            Icons.Default.QrCode,
                            "Cobro contraentrega",
                            if (envio.cobraContraentrega) formatoSoles(envio.montoContraentrega) else "No (envío prepagado)"
                        )
                        FilaDato(
                            Icons.Default.DirectionsCar,
                            "Courier asignado",
                            envio.transportistaAsignado ?: "Por asignar"
                        )
                        if (envio.intentos > 0) {
                            FilaDato(
                                Icons.Default.ReportProblem,
                                "Intentos de entrega fallidos",
                                "${envio.intentos} de ${EstadoEnvio.MAX_INTENTOS}"
                            )
                        }
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

/** HU-R02: eventos con hora y lugar, intentos fallidos y prueba de entrega (foto, firma y quién recibió). */
@Composable
private fun TarjetaSeguimiento(
    envio: Envio,
    estado: SeguimientoUiState,
    onActualizar: () -> Unit,
    cargarImagen: suspend (url: String) -> ImageBitmap?
) {
    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.TravelExplore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Seguimiento en vivo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (estado.cargando) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else if (envio.idRemoto != null) {
                TextButton(onClick = onActualizar) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Actualizar")
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        val datos = estado.seguimiento
        estado.error?.let { MensajeInfo(texto = it) }

        if (datos != null) {
            // ML-02: fecha estimada para el remitente.
            datos.estimacion?.let { llegada ->
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(14.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Llegada estimada",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = llegada.texto(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Calculada con el historial de entregas de tu ruta (modelo ML-02).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            datos.prueba?.let { prueba ->
                Surface(
                    color = EstadoEnvio.ENTREGADO.color.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Prueba de entrega",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EstadoEnvio.ENTREGADO.color
                        )
                        Text("Recibió: ${prueba.recibeNombre} · DNI ${prueba.recibeDni}", style = MaterialTheme.typography.bodyMedium)
                        prueba.fechaHora?.let {
                            Text(formatoFechaHora(it), style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ImagenEvidencia(
                                titulo = "Foto de la entrega",
                                url = prueba.fotoUrl,
                                fondoBlanco = false,
                                cargarImagen = cargarImagen,
                                modifier = Modifier.weight(1f)
                            )
                            ImagenEvidencia(
                                titulo = "Firma",
                                url = prueba.firmaUrl,
                                fondoBlanco = true, // la firma es trazo oscuro sobre transparente
                                cargarImagen = cargarImagen,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (datos.intentos.isNotEmpty()) {
                Text(
                    text = "Intentos de entrega fallidos",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                datos.intentos.forEach { intento ->
                    Text(
                        text = "${intento.numero}. ${MotivoFalla.etiqueta(intento.motivo)}" +
                            (intento.fecha?.let { " · ${formatoFechaHora(it)}" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = EstadoEnvio.FALLIDO.color
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // RF-C07: tras una fallida, RutaLog avisa al destinatario para reprogramar (SMS simulado).
            if (datos.avisos.isNotEmpty()) {
                Text(
                    text = "Avisos al destinatario",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                datos.avisos.forEach { aviso ->
                    Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.Sms,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "SMS a ${aviso.destino}" + (aviso.fecha?.let { " · ${formatoFechaHora(it)}" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = aviso.mensaje,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = "Historial",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (datos.eventos.isEmpty()) {
                Text(
                    text = "Este envío se registró antes del seguimiento en vivo: solo se muestra su estado actual.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            datos.eventos.asReversed().forEach { evento ->
                Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = evento.estado.icono,
                        contentDescription = null,
                        tint = evento.estado.color,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (evento.estado == EstadoEnvio.PENDIENTE) "Registrado" else evento.estado.etiqueta,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = listOfNotNull(
                                evento.fechaHora?.let(::formatoFechaHora),
                                if (evento.tieneLugar) {
                                    String.format(Locale.US, "GPS %.5f, %.5f", evento.latitud, evento.longitud)
                                } else {
                                    null
                                }
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/** Foto o firma de la entrega, descargada con la sesión del cliente (la API solo la entrega al dueño). */
@Composable
private fun ImagenEvidencia(
    titulo: String,
    url: String?,
    fondoBlanco: Boolean,
    cargarImagen: suspend (url: String) -> ImageBitmap?,
    modifier: Modifier = Modifier
) {
    val imagen by produceState<ImageBitmap?>(initialValue = null, url) {
        value = url?.let { cargarImagen(it) }
    }
    var cargando by remember(url) { mutableStateOf(url != null) }
    LaunchedEffect(imagen) { if (imagen != null) cargando = false }
    // Si tras unos segundos no llegó, se deja de mostrar la rueda (sin conexión).
    LaunchedEffect(url) {
        kotlinx.coroutines.delay(8000)
        cargando = false
    }

    Column(modifier = modifier) {
        Text(titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (fondoBlanco) Color.White else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val actual = imagen
            when {
                actual != null -> Image(
                    bitmap = actual,
                    contentDescription = titulo,
                    contentScale = if (fondoBlanco) ContentScale.Fit else ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                cargando -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)

                else -> Text(
                    text = if (url == null) "Sin imagen" else "No se pudo cargar",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (fondoBlanco) Color.DarkGray else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
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
