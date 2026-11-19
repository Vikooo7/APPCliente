package com.example.rutalogcliente

import android.Manifest
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import com.example.rutalogcliente.util.Notificaciones
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rutalogcliente.ui.navigation.AppNavigation
import com.example.rutalogcliente.ui.theme.RutaLogTheme
import com.example.rutalogcliente.viewmodel.AuthViewModel
import com.example.rutalogcliente.viewmodel.EnvioViewModel
import com.example.rutalogcliente.viewmodel.PagosViewModel
import com.example.rutalogcliente.viewmodel.SyncViewModel

/**
 * RutaLog Perú · App Cliente.
 * Toma las dependencias del contenedor (Room, API y repositorios) e inicializa los ViewModel.
 */
class MainActivity : ComponentActivity() {

    /** Guía de la notificación que se tocó: se abre su seguimiento (push en tiempo real o aviso al sincronizar). */
    private val guiaAbrir = mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(Notificaciones.EXTRA_GUIA)?.let { guiaAbrir.value = it }
    }

    /** Al encender la pantalla o volver de otra app (por ejemplo, de pagar en Banco Andino), se revisa la red. */
    override fun onResume() {
        super.onResume()
        (application as RutaLogApplication).contenedor.conectividad.revisarAhora()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La barra superior siempre es azul oscuro: íconos del sistema en blanco.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))

        val contenedor = (application as RutaLogApplication).contenedor
        pedirPermisoDeAvisos()
        // Si hay sesión, este celular queda registrado para los avisos en tiempo real (el token puede haber cambiado).
        contenedor.registrarPush()
        if (savedInstanceState == null) guiaAbrir.value = intent?.getStringExtra(Notificaciones.EXTRA_GUIA)

        setContent {
            RutaLogTheme {
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModel.factory(contenedor.authRepository)
                )
                val envioViewModel: EnvioViewModel = viewModel(
                    factory = EnvioViewModel.factory(
                        repositorio = contenedor.envioRepository,
                        seguimientoRepository = contenedor.seguimientoRepository,
                        programarSincronizacion = contenedor::programarSincronizacion
                    )
                )
                val syncViewModel: SyncViewModel = viewModel(
                    factory = SyncViewModel.factory(
                        repositorio = contenedor.syncRepository,
                        conectividad = contenedor.conectividad,
                        simularError = contenedor.simularErrorServidor
                    )
                )
                val pagosViewModel: PagosViewModel = viewModel(
                    factory = PagosViewModel.factory(contenedor.pagoRepository)
                )
                AppNavigation(
                    authViewModel = authViewModel,
                    envioViewModel = envioViewModel,
                    syncViewModel = syncViewModel,
                    pagosViewModel = pagosViewModel,
                    guiaAbrir = guiaAbrir.value,
                    onGuiaAbierta = { guiaAbrir.value = null }
                )
            }
        }
    }

    /** Android 13+: pide una sola vez el permiso para avisar cuando un envío cambia de estado. */
    private val permisoAvisos = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private fun pedirPermisoDeAvisos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permisoAvisos.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
