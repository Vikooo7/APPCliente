package com.example.rutalogcliente.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rutalogcliente.model.EstadoEnvio

val EstadoEnvio.color: Color
    get() = when (this) {
        EstadoEnvio.PENDIENTE -> Color(0xFF8D6E63)
        EstadoEnvio.RECOGIDO -> Color(0xFF1E88E5)
        EstadoEnvio.EN_TRANSITO -> Color(0xFFEF6C00)
        EstadoEnvio.EN_REPARTO -> Color(0xFF8E24AA)
        EstadoEnvio.ENTREGADO -> Color(0xFF2E7D32)
    }

val EstadoEnvio.icono: ImageVector
    get() = when (this) {
        EstadoEnvio.PENDIENTE -> Icons.Default.Schedule
        EstadoEnvio.RECOGIDO -> Icons.Default.Inventory
        EstadoEnvio.EN_TRANSITO -> Icons.Default.LocalShipping
        EstadoEnvio.EN_REPARTO -> Icons.Default.LocationOn
        EstadoEnvio.ENTREGADO -> Icons.Default.CheckCircle
    }

@Composable
fun EstadoBadge(estado: EstadoEnvio, sobreFondoOscuro: Boolean = false) {
    Surface(
        color = if (sobreFondoOscuro) Color.White else estado.color.copy(alpha = 0.14f),
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = estado.icono,
                contentDescription = null,
                tint = estado.color,
                modifier = Modifier.size(14.dp)
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
}

private val pasosLineaTiempo = listOf(
    EstadoEnvio.RECOGIDO,
    EstadoEnvio.EN_TRANSITO,
    EstadoEnvio.EN_REPARTO,
    EstadoEnvio.ENTREGADO
)

/** Línea de tiempo: recogido → en tránsito → en reparto → entregado. */
@Composable
fun LineaDeTiempo(estado: EstadoEnvio) {
    Column {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ConfirmationNumber,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (estado == EstadoEnvio.PENDIENTE) {
                        EstadoEnvio.PENDIENTE.descripcion
                    } else {
                        "Guía emitida y carga en manos de RutaLog."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        pasosLineaTiempo.forEachIndexed { indice, paso ->
            PasoLineaTiempo(
                paso = paso,
                completado = estado.paso >= paso.paso,
                actual = estado == paso,
                lineaCompleta = estado.paso > paso.paso,
                esUltimo = indice == pasosLineaTiempo.lastIndex
            )
        }
    }
}

@Composable
private fun PasoLineaTiempo(
    paso: EstadoEnvio,
    completado: Boolean,
    actual: Boolean,
    lineaCompleta: Boolean,
    esUltimo: Boolean
) {
    val pulso = rememberInfiniteTransition(label = "PulsoPaso")
    val escala by pulso.animateFloat(
        initialValue = 1f,
        targetValue = if (actual) 1.35f else 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "EscalaPaso"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(40.dp)
                .fillMaxHeight()
        ) {
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                if (actual && paso != EstadoEnvio.ENTREGADO) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .scale(escala)
                            .clip(CircleShape)
                            .background(paso.color.copy(alpha = 0.25f))
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (completado) paso.color else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (completado && !actual) Icons.Default.Check else paso.icono,
                        contentDescription = null,
                        tint = if (completado) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (!esUltimo) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (lineaCompleta) paso.color else MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 6.dp, bottom = if (esUltimo) 0.dp else 22.dp)
        ) {
            Text(
                text = paso.etiqueta,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (actual) FontWeight.Bold else FontWeight.SemiBold,
                color = if (completado) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (completado) paso.descripcion else "Pendiente",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
