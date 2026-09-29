package com.example.rutalogcliente

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rutalogcliente.data.local.AppDatabase
import com.example.rutalogcliente.data.local.Envio
import com.example.rutalogcliente.model.CatalogoRutas
import com.example.rutalogcliente.model.RutaTarifa
import com.example.rutalogcliente.ui.components.AppScaffold
import com.example.rutalogcliente.ui.components.EstadoVacio
import com.example.rutalogcliente.ui.components.ItemCard
import com.example.rutalogcliente.ui.components.MensajeError
import com.example.rutalogcliente.ui.components.MensajeInfo
import com.example.rutalogcliente.ui.components.PestanaCliente
import com.example.rutalogcliente.ui.components.formatoPeso
import com.example.rutalogcliente.ui.components.formatoSoles
import com.example.rutalogcliente.ui.navigation.AppNavigation
import com.example.rutalogcliente.ui.theme.RutaLogTheme
import com.example.rutalogcliente.viewmodel.AuthViewModel
import com.example.rutalogcliente.viewmodel.EnvioViewModel

/** RutaLog Perú · App Cliente. Crea la base de datos Room e inicializa los ViewModel. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La barra superior siempre es azul oscuro: íconos del sistema en blanco.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))

        val db = AppDatabase.getDB(this)

        setContent {
            RutaLogTheme {
                val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(db.usuarioDao()))
                val envioViewModel: EnvioViewModel = viewModel(factory = EnvioViewModel.factory(db.envioDao()))
                AppNavigation(authViewModel = authViewModel, envioViewModel = envioViewModel)
            }
        }
    }
}

/**
 * Pantalla principal de registro (pestaña "Registrar"), al estilo del ejemplo de clase:
 * 2 OutlinedTextField (ruta y peso) + botón Guardar + LazyColumn con los envíos guardados en Room.
 *
 * Guardar llama a EnvioViewModel.registrar(): valida el peso (RF10), calcula el costo (RF08),
 * inserta en Room (RF06) y genera el número de guía (RF07).
 */
@Composable
fun PantallaPrincipal(
    envioViewModel: EnvioViewModel,
    nombreUsuario: String,
    onEnvio: (Envio) -> Unit,
    onPestana: (PestanaCliente) -> Unit,
    onCerrarSesion: () -> Unit
) {
    val envios by envioViewModel.envios.collectAsState()

    var ruta by remember { mutableStateOf<RutaTarifa?>(null) }
    var peso by rememberSaveable { mutableStateOf("") }
    var menuRutas by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var guiaGenerada by remember { mutableStateOf<String?>(null) }

    val pesoNumero = envioViewModel.leerPeso(peso)
    val costo = ruta?.let { r -> pesoNumero?.takeIf { it > 0 }?.let { envioViewModel.calcularCosto(it, r) } }

    val guardar = {
        error = null
        guiaGenerada = null
        envioViewModel.registrar(
            ruta = ruta,
            pesoTexto = peso,
            onError = { error = it },
            onRegistrado = { envio ->
                guiaGenerada = "Guía ${envio.numeroGuia} · ${envio.ruta} · ${formatoSoles(envio.costoEnvio)}"
                ruta = null
                peso = ""
            }
        )
    }

    AppScaffold(
        titulo = "Registrar envío",
        subtitulo = "Cliente · $nombreUsuario",
        pestana = PestanaCliente.REGISTRAR,
        onPestana = onPestana,
        onCerrarSesion = onCerrarSesion
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Campo 1: ruta (de solo lectura; al tocarlo se abre la lista de rutas)
                    Box {
                        OutlinedTextField(
                            value = ruta?.nombre ?: "",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Ruta") },
                            placeholder = { Text("Selecciona una ruta") },
                            leadingIcon = { Icon(Icons.Default.Map, contentDescription = null) },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (menuRutas) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            },
                            supportingText = {
                                Text(ruta?.let { "${it.region} · tarifa ${formatoSoles(it.tarifaPorKg)}/kg" } ?: "27 rutas hacia las 25 regiones")
                            },
                            singleLine = true
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { menuRutas = true }
                        )
                        DropdownMenu(expanded = menuRutas, onDismissRequest = { menuRutas = false }) {
                            CatalogoRutas.rutas.forEach { opcion ->
                                DropdownMenuItem(
                                    text = { Text("${opcion.nombre} · ${formatoSoles(opcion.tarifaPorKg)}/kg") },
                                    onClick = {
                                        ruta = opcion
                                        menuRutas = false
                                        error = null
                                    }
                                )
                            }
                        }
                    }

                    // Campo 2: peso en kg
                    OutlinedTextField(
                        value = peso,
                        onValueChange = { valor ->
                            peso = valor.filter { it.isDigit() || it == '.' || it == ',' }.take(8)
                            error = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Peso (kg)") },
                        leadingIcon = { Icon(Icons.Default.Scale, contentDescription = null) },
                        supportingText = {
                            Text(
                                if (costo != null && pesoNumero != null && ruta != null) {
                                    "Costo: ${formatoPeso(pesoNumero)} × ${formatoSoles(ruta!!.tarifaPorKg)} = ${formatoSoles(costo)}"
                                } else {
                                    "Debe ser mayor que 0 kg"
                                }
                            )
                        },
                        isError = error != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { guardar() }),
                        singleLine = true
                    )

                    AnimatedVisibility(visible = error != null) {
                        MensajeError(texto = error ?: "")
                    }
                    AnimatedVisibility(visible = guiaGenerada != null) {
                        MensajeInfo(texto = guiaGenerada ?: "")
                    }

                    Button(
                        onClick = { guardar() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Text("  Guardar")
                    }
                }
            }

            Row(
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Envíos guardados (${envios.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Lista de envíos leída de Room (se actualiza sola con Flow)
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (envios.isEmpty()) {
                    item {
                        EstadoVacio(
                            icono = Icons.Default.Inventory,
                            titulo = "Aún no hay envíos",
                            mensaje = "Completa la ruta y el peso, y toca Guardar."
                        )
                    }
                }
                items(envios, key = { it.id }) { envio ->
                    ItemCard(envio = envio, onClick = { onEnvio(envio) })
                }
            }
        }
    }
}
