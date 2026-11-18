package com.example.rutalogcliente.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rutalogcliente.util.IngresoGoogle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** "Continuar con Google": abre el selector de cuentas y entrega el ID token de Firebase. */
@Composable
fun BotonGoogle(
    habilitado: Boolean,
    onToken: (String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val alcance = rememberCoroutineScope()
    var abriendo by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = {
            abriendo = true
            alcance.launch {
                try {
                    onToken(IngresoGoogle.obtenerToken(context))
                } catch (e: IngresoGoogle.Cancelado) {
                    // Cerró la ventana de Google: no se muestra nada.
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IngresoGoogle.Fallo) {
                    onError(e.message ?: "No se pudo iniciar sesión con Google.")
                } catch (e: Exception) {
                    onError("No se pudo iniciar sesión con Google. Intenta de nuevo.")
                } finally {
                    abriendo = false
                }
            }
        },
        enabled = habilitado && !abriendo,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        if (abriendo) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text("Continuar con Google")
    }
}
