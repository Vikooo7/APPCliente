package com.example.rutalogcliente.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * QR con el número de guía. El courier lo escanea con la App Courier al recoger el paquete (HU-C02):
 * el texto del QR es exactamente la guía (RLP-AA-NNNNNN-D).
 */
@Composable
fun QrGuia(numeroGuia: String, modifier: Modifier = Modifier, tamano: Dp = 180.dp) =
    QrCodigo(texto = numeroGuia, descripcion = "Código QR de la guía $numeroGuia", modifier = modifier, tamano = tamano)

/** QR de cualquier texto (por ejemplo, el cobro de Banco Andino "BANCOANDINO:QR-..."). */
@Composable
fun QrCodigo(texto: String, descripcion: String, modifier: Modifier = Modifier, tamano: Dp = 180.dp) {
    val qr = remember(texto) { generarQr(texto) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White) // siempre en blanco y negro, también en tema oscuro, para que se lea bien
            .padding(10.dp)
    ) {
        Image(
            bitmap = qr,
            contentDescription = descripcion,
            filterQuality = FilterQuality.None, // bordes nítidos al agrandar
            modifier = Modifier.size(tamano)
        )
    }
}

/** Matriz del QR en un bitmap de un píxel por módulo (se agranda al dibujarlo). */
internal fun generarQr(texto: String): ImageBitmap {
    val matriz = QRCodeWriter().encode(
        texto,
        BarcodeFormat.QR_CODE,
        0,
        0,
        mapOf(EncodeHintType.MARGIN to 1, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M)
    )
    val pixeles = IntArray(matriz.width * matriz.height) { i ->
        if (matriz.get(i % matriz.width, i / matriz.width)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    }
    return Bitmap.createBitmap(pixeles, matriz.width, matriz.height, Bitmap.Config.ARGB_8888).asImageBitmap()
}
