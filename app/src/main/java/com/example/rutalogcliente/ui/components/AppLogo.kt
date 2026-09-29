package com.example.rutalogcliente.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.rutalogcliente.R

/** Logo completo (emblema + "RUTALOG PERÚ | Logística & Transporte"). */
@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    ancho: Dp = 200.dp
) {
    Image(
        painter = painterResource(id = R.drawable.logo_rutalog),
        contentDescription = "Logo RutaLog Perú",
        modifier = modifier
            .width(ancho)
            .aspectRatio(620f / 665f)
    )
}

/** Solo el emblema, en un círculo blanco (barra superior). */
@Composable
fun AppEmblema(
    modifier: Modifier = Modifier,
    tamano: Dp = 36.dp
) {
    Box(
        modifier = modifier
            .size(tamano)
            .clip(CircleShape)
            .background(Color.White)
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
            contentDescription = "RutaLog Perú",
            modifier = Modifier
                .fillMaxSize()
                .scale(1.7f)
        )
    }
}
