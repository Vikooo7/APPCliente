package com.example.rutalogcliente.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.rutalogcliente.ui.components.AppLogo
import com.example.rutalogcliente.ui.theme.AzulRuta
import com.example.rutalogcliente.ui.theme.RojoRuta
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * Línea de tiempo del Splash (≈ 2.9 s en total):
 *   0     – 700 ms  el logo aparece con un pequeño rebote
 *   350   – 850 ms  aparece la frase
 *   700   – 2600 ms el camión recorre la ruta y va encendiendo las 4 paradas
 *   2600  – 2900 ms "¡Listo!" con el check, y se pasa al Login
 * El camión llega al final exactamente cuando se entra a la app.
 */
private const val INICIO_RUTA_MS = 700
private const val DURACION_RUTA_MS = 1900
private const val PAUSA_FINAL_MS = 300L

private val paradas = listOf("Recojo", "Tránsito", "Reparto", "Entrega")

/** RFA01: Splash con la identidad visual de la app. */
@Composable
fun SplashScreen(onFinish: () -> Unit) {
    val finalizar by rememberUpdatedState(onFinish)

    val escalaLogo = remember { Animatable(0.7f) }
    val alfaLogo = remember { Animatable(0f) }
    val alfaFrase = remember { Animatable(0f) }
    val avance = remember { Animatable(0f) }
    val escalaCheck = remember { Animatable(0f) }

    // Vaivén del camión mientras avanza, para que se vea en movimiento.
    val vaiven by rememberInfiniteTransition(label = "VaivenCamion").animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(180, easing = LinearEasing), RepeatMode.Reverse),
        label = "VaivenY"
    )

    LaunchedEffect(Unit) {
        launch { escalaLogo.animateTo(1f, tween(700, easing = EaseOutBack)) }
        launch { alfaLogo.animateTo(1f, tween(600)) }
        launch {
            delay(350)
            alfaFrase.animateTo(1f, tween(500))
        }
        delay(INICIO_RUTA_MS.toLong())
        avance.animateTo(1f, tween(DURACION_RUTA_MS, easing = FastOutSlowInEasing))
        escalaCheck.animateTo(1f, tween(250, easing = EaseOutBack))
        delay(PAUSA_FINAL_MS)
        finalizar()
    }

    val progreso = avance.value
    val llego = progreso >= 1f
    val mensaje = when {
        llego -> "¡Listo! Entrando…"
        progreso > 0.66f -> "Llegando a destino…"
        progreso > 0.33f -> "En ruta por el Perú…"
        else -> "Preparando tu carga…"
    }

    // Fondo blanco igual al del logo: ícono → splash del sistema → este splash se ve continuo.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            AppLogo(
                ancho = 260.dp,
                modifier = Modifier
                    .scale(escalaLogo.value)
                    .alpha(alfaLogo.value)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Tu carga ubicada de costa a selva",
                color = AzulRuta,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(alfaFrase.value)
                    .offset(y = ((1f - alfaFrase.value) * 12).dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            RutaAnimada(
                progreso = progreso,
                vaivenY = if (llego) 0f else vaiven,
                escalaCheck = escalaCheck.value,
                modifier = Modifier
                    .width(260.dp)
                    .alpha(alfaFrase.value)
            )

            Spacer(modifier = Modifier.height(18.dp))

            AnimatedContent(
                targetState = mensaje,
                transitionSpec = {
                    (fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 2 })
                        .togetherWith(fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it / 2 })
                },
                label = "MensajeSplash"
            ) { texto ->
                Text(
                    text = texto,
                    color = if (llego) RojoRuta else AzulRuta.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (llego) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        Text(
            text = "App Cliente · v0.6",
            color = AzulRuta.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        )
    }
}

/** Barra de ruta con 4 paradas que se encienden a medida que pasa el camión. */
@Composable
private fun RutaAnimada(
    progreso: Float,
    vaivenY: Float,
    escalaCheck: Float,
    modifier: Modifier = Modifier
) {
    val tamanoCamion = 28.dp
    val tamanoParada = 12.dp

    Column(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            val ancho = maxWidth
            val recorrido = ancho - tamanoParada

            // Pista gris y tramo recorrido en rojo
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = (tamanoParada - 4.dp) / 2)
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(AzulRuta.copy(alpha = 0.12f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = (tamanoParada - 4.dp) / 2)
                    .width(recorrido * progreso + tamanoParada / 2)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(RojoRuta)
            )

            // Paradas: se encienden cuando el camión pasa por ellas
            paradas.indices.forEach { indice ->
                val posicion = indice / (paradas.size - 1f)
                val alcanzada = progreso >= posicion - 0.001f
                Parada(
                    alcanzada = alcanzada,
                    tamano = tamanoParada,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = recorrido * posicion)
                )
            }

            // Camión: su centro sigue al frente del tramo rojo
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = (recorrido * progreso + tamanoParada / 2 - tamanoCamion / 2)
                            .coerceIn(0.dp, ancho - tamanoCamion),
                        y = vaivenY.dp
                    )
                    .size(tamanoCamion),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = RojoRuta,
                    modifier = Modifier.size(tamanoCamion)
                )
                if (escalaCheck > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                            .scale(escalaCheck)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E7D32)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Nombre de cada parada debajo de su punto
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val recorrido = maxWidth - tamanoParada
            paradas.forEachIndexed { indice, nombre ->
                val posicion = indice / (paradas.size - 1f)
                val alcanzada = progreso >= posicion - 0.001f
                Text(
                    text = nombre,
                    color = if (alcanzada) AzulRuta else AzulRuta.copy(alpha = 0.35f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (alcanzada) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(64.dp)
                        .offset(x = (recorrido * posicion + tamanoParada / 2 - 32.dp).coerceIn(0.dp, maxWidth - 64.dp))
                )
            }
        }
    }
}

@Composable
private fun Parada(alcanzada: Boolean, tamano: Dp, modifier: Modifier = Modifier) {
    val escala = remember { Animatable(1f) }
    LaunchedEffect(alcanzada) {
        if (alcanzada) {
            escala.animateTo(1.5f, tween(120))
            escala.animateTo(1f, tween(160))
        }
    }
    Box(
        modifier = modifier
            .scale(escala.value)
            .size(tamano)
            .clip(CircleShape)
            .background(if (alcanzada) RojoRuta else Color.White)
            .padding(2.dp)
            .clip(CircleShape)
            .background(if (alcanzada) Color.White else AzulRuta.copy(alpha = 0.25f))
    )
}
