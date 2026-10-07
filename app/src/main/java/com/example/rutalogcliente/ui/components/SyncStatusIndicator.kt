package com.example.rutalogcliente.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rutalogcliente.model.EstadoSincronizacion

val EstadoSincronizacion.color: Color
    get() = when (this) {
        EstadoSincronizacion.PENDIENTE -> Color(0xFFB26A00)
        EstadoSincronizacion.ENVIANDO -> Color(0xFF1E88E5)
        EstadoSincronizacion.SINCRONIZADO -> Color(0xFF2E7D32)
        EstadoSincronizacion.ERROR -> Color(0xFFC62828)
    }

val EstadoSincronizacion.icono: ImageVector
    get() = when (this) {
        EstadoSincronizacion.PENDIENTE -> Icons.Default.CloudQueue
        EstadoSincronizacion.ENVIANDO -> Icons.Default.CloudUpload
        EstadoSincronizacion.SINCRONIZADO -> Icons.Default.CloudDone
        EstadoSincronizacion.ERROR -> Icons.Default.ErrorOutline
    }

/** Muestra si un registro ya está en el servidor, sigue en la cola o fue rechazado. */
@Composable
fun SyncStatusIndicator(estado: EstadoSincronizacion, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = estado.icono,
            contentDescription = null,
            tint = estado.color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = estado.etiqueta,
            color = estado.color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}
