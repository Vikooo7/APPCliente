package com.example.rutalogcliente.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.rutalogcliente.PantallaPrincipal
import com.example.rutalogcliente.ui.components.LocalEnLinea
import com.example.rutalogcliente.ui.components.LocalOperacionesEnCola
import com.example.rutalogcliente.ui.components.PestanaCliente
import com.example.rutalogcliente.ui.screens.DetailScreen
import com.example.rutalogcliente.ui.screens.FormScreen
import com.example.rutalogcliente.ui.screens.HomeScreen
import com.example.rutalogcliente.ui.screens.ListScreen
import com.example.rutalogcliente.ui.screens.LoginScreen
import com.example.rutalogcliente.ui.screens.SplashScreen
import com.example.rutalogcliente.ui.screens.SyncScreen
import com.example.rutalogcliente.viewmodel.AuthViewModel
import com.example.rutalogcliente.viewmodel.EnvioViewModel
import com.example.rutalogcliente.viewmodel.SyncViewModel

object Rutas {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val HOME = "home"
    const val LISTA = "lista"
    const val SINCRONIZAR = "sincronizar"
    const val DETALLE = "detalle/{envioId}"
    const val FORMULARIO = "formulario?envioId={envioId}"

    fun detalle(id: Int) = "detalle/$id"
    fun formulario(id: Int? = null) = if (id == null) "formulario" else "formulario?envioId=$id"
}

/** Splash → Login (o directo a Inicio si ya hay sesión) → Inicio / Registrar / Mis envíos / Sincronizar. */
@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    envioViewModel: EnvioViewModel,
    syncViewModel: SyncViewModel,
    navController: NavHostController = rememberNavController()
) {
    val auth by authViewModel.estado.collectAsState()
    val sync by syncViewModel.estado.collectAsState()
    val nombreUsuario = auth.usuario?.nombre.orEmpty()
    // Mientras se comprueba la sesión guardada no se expulsa al usuario al Login.
    val haySesion = auth.usuario != null || auth.comprobandoSesion

    // El SyncViewModel solo sincroniza sola (al volver la conexión) si hay sesión.
    syncViewModel.haySesion = { authViewModel.estado.value.usuario != null }

    val irAPestana: (PestanaCliente) -> Unit = { pestana ->
        val destino = when (pestana) {
            PestanaCliente.INICIO -> Rutas.HOME
            PestanaCliente.REGISTRAR -> Rutas.formulario()
            PestanaCliente.MIS_ENVIOS -> Rutas.LISTA
            PestanaCliente.SINCRONIZAR -> Rutas.SINCRONIZAR
        }
        navController.navigate(destino) {
            popUpTo(Rutas.HOME)
            launchSingleTop = true
        }
    }

    val cerrarSesion: () -> Unit = {
        authViewModel.cerrarSesion()
        navController.navigate(Rutas.LOGIN) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    CompositionLocalProvider(
        LocalEnLinea provides sync.enLinea,
        LocalOperacionesEnCola provides sync.operaciones.size
    ) {
        NavHost(
            navController = navController,
            startDestination = Rutas.SPLASH,
            enterTransition = { fadeIn(tween(250)) + slideInHorizontally(tween(250)) { it / 10 } },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(250)) },
            popExitTransition = { fadeOut(tween(150)) }
        ) {
            composable(Rutas.SPLASH) {
                SplashScreen(
                    onFinish = {
                        // Con sesión guardada se entra directo, aunque no haya conexión.
                        val conSesion = authViewModel.estado.value.usuario != null
                        if (conSesion) syncViewModel.sincronizar(silencioso = true)
                        navController.navigate(if (conSesion) Rutas.HOME else Rutas.LOGIN) {
                            popUpTo(Rutas.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            composable(Rutas.LOGIN) {
                LoginScreen(
                    estado = auth,
                    onLogin = { correo, clave ->
                        authViewModel.login(correo, clave) {
                            // RF11: tras el login se descargan los envíos del cliente a Room.
                            syncViewModel.sincronizar(silencioso = true)
                            navController.navigate(Rutas.HOME) {
                                popUpTo(Rutas.LOGIN) { inclusive = true }
                            }
                        }
                    },
                    onLimpiarMensajes = authViewModel::limpiarMensajes
                )
            }

            composable(Rutas.HOME) {
                SesionRequerida(hayUsuario = haySesion, onSinSesion = cerrarSesion)
                val envios by envioViewModel.envios.collectAsState()
                val errorBusqueda by envioViewModel.errorBusqueda.collectAsState()
                HomeScreen(
                    nombreUsuario = nombreUsuario,
                    envios = envios,
                    errorBusqueda = errorBusqueda,
                    onBuscarGuia = { guia ->
                        envioViewModel.buscarPorGuia(guia) { envio ->
                            navController.navigate(Rutas.detalle(envio.id))
                        }
                    },
                    onLimpiarErrorBusqueda = envioViewModel::limpiarErrorBusqueda,
                    onRegistrar = { irAPestana(PestanaCliente.REGISTRAR) },
                    onVerEnvios = { irAPestana(PestanaCliente.MIS_ENVIOS) },
                    onEnvio = { navController.navigate(Rutas.detalle(it.id)) },
                    onPestana = irAPestana,
                    onCerrarSesion = cerrarSesion
                )
            }

            composable(Rutas.LISTA) {
                SesionRequerida(hayUsuario = haySesion, onSinSesion = cerrarSesion)
                val todos by envioViewModel.envios.collectAsState()
                val filtrados by envioViewModel.enviosFiltrados.collectAsState()
                val texto by envioViewModel.texto.collectAsState()
                val filtroEstado by envioViewModel.filtroEstado.collectAsState()
                ListScreen(
                    nombreUsuario = nombreUsuario,
                    envios = filtrados,
                    totalEnvios = todos.size,
                    texto = texto,
                    filtroEstado = filtroEstado,
                    onTexto = envioViewModel::buscarTexto,
                    onEstado = envioViewModel::filtrarPorEstado,
                    onEnvio = { navController.navigate(Rutas.detalle(it.id)) },
                    onRegistrar = { irAPestana(PestanaCliente.REGISTRAR) },
                    onPestana = irAPestana,
                    onCerrarSesion = cerrarSesion
                )
            }

            composable(Rutas.SINCRONIZAR) {
                SesionRequerida(hayUsuario = haySesion, onSinSesion = cerrarSesion)
                SyncScreen(
                    estado = sync,
                    nombreUsuario = nombreUsuario,
                    onSincronizar = { syncViewModel.sincronizar() },
                    onReintentar = syncViewModel::reintentar,
                    onDescartar = syncViewModel::descartar,
                    onSimularError = syncViewModel::cambiarSimulacionError,
                    onPestana = irAPestana,
                    onCerrarSesion = cerrarSesion
                )
            }

            composable(
                route = Rutas.DETALLE,
                arguments = listOf(navArgument("envioId") { type = NavType.IntType })
            ) { entrada ->
                SesionRequerida(hayUsuario = haySesion, onSinSesion = cerrarSesion)
                val id = entrada.arguments?.getInt("envioId") ?: 0
                val flujo = remember(id) { envioViewModel.obtenerPorId(id) }
                val envio by flujo.collectAsState(initial = null)
                DetailScreen(
                    envio = envio,
                    onVolver = { navController.popBackStack() },
                    onEditar = { navController.navigate(Rutas.formulario(it.id)) },
                    onEliminar = { aEliminar, onError ->
                        envioViewModel.eliminar(aEliminar, onError) {
                            navController.popBackStack()
                        }
                    }
                )
            }

            composable(
                route = Rutas.FORMULARIO,
                arguments = listOf(
                    navArgument("envioId") {
                        type = NavType.IntType
                        defaultValue = -1
                    }
                )
            ) { entrada ->
                SesionRequerida(hayUsuario = haySesion, onSinSesion = cerrarSesion)
                val id = entrada.arguments?.getInt("envioId") ?: -1
                val esEdicion = id != -1
                if (!esEdicion) {
                    // Registro nuevo: PantallaPrincipal (en MainActivity.kt), al estilo del ejemplo de clase.
                    PantallaPrincipal(
                        envioViewModel = envioViewModel,
                        nombreUsuario = nombreUsuario,
                        onEnvio = { navController.navigate(Rutas.detalle(it.id)) },
                        onPestana = irAPestana,
                        onCerrarSesion = cerrarSesion
                    )
                    return@composable
                }
                val flujo = remember(id) { envioViewModel.obtenerPorId(id) }
                val envioEditado by flujo.collectAsState(initial = null)
                FormScreen(
                    envioViewModel = envioViewModel,
                    envioEditado = envioEditado,
                    esEdicion = true,
                    nombreUsuario = nombreUsuario,
                    onVolver = { navController.popBackStack() },
                    onPestana = irAPestana,
                    onCerrarSesion = cerrarSesion,
                    onVerSeguimiento = { envio ->
                        navController.navigate(Rutas.detalle(envio.id)) {
                            popUpTo(Rutas.HOME)
                        }
                    }
                )
            }
        }
    }
}

/** Si se perdió la sesión, vuelve al Login en vez de mostrar datos sin usuario. */
@Composable
private fun SesionRequerida(hayUsuario: Boolean, onSinSesion: () -> Unit) {
    LaunchedEffect(hayUsuario) {
        if (!hayUsuario) onSinSesion()
    }
}
