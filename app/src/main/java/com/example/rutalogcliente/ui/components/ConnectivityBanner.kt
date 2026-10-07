package com.example.rutalogcliente.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** true si el teléfono tiene conexión. AppNavigation lo provee a todas las pantallas. */
val LocalEnLinea = compositionLocalOf { true }

/** Cantidad de operaciones en la cola (pendientes y rechazadas), para la pestaña Sincronizar. */
val LocalOperacionesEnCola = compositionLocalOf { 0 }

/** Franja que avisa que se trabaja sin conexión, con los datos guardados en el teléfono. */
@Composable
fun ConnectivityBanner(enLinea: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = !enLinea,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF8A5A00))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Sin conexión: ves los datos guardados y tus cambios se enviarán al reconectar.",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
