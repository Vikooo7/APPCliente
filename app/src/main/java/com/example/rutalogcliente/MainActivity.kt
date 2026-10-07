package com.example.rutalogcliente

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rutalogcliente.ui.navigation.AppNavigation
import com.example.rutalogcliente.ui.theme.RutaLogTheme
import com.example.rutalogcliente.viewmodel.AuthViewModel
import com.example.rutalogcliente.viewmodel.EnvioViewModel
import com.example.rutalogcliente.viewmodel.SyncViewModel

/**
 * RutaLog Perú · App Cliente.
 * Toma las dependencias del contenedor (Room, API y repositorios) e inicializa los ViewModel.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La barra superior siempre es azul oscuro: íconos del sistema en blanco.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))

        val contenedor = (application as RutaLogApplication).contenedor

        setContent {
            RutaLogTheme {
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModel.factory(contenedor.authRepository)
                )
                val envioViewModel: EnvioViewModel = viewModel(
                    factory = EnvioViewModel.factory(
                        repositorio = contenedor.envioRepository,
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
                AppNavigation(
                    authViewModel = authViewModel,
                    envioViewModel = envioViewModel,
                    syncViewModel = syncViewModel
                )
            }
        }
    }
}
