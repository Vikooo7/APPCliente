package com.example.rutalogcliente.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
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
import com.example.rutalogcliente.ui.components.PestanaCliente
import com.example.rutalogcliente.ui.screens.DetailScreen
import com.example.rutalogcliente.ui.screens.FormScreen
import com.example.rutalogcliente.ui.screens.HomeScreen
import com.example.rutalogcliente.ui.screens.ListScreen
import com.example.rutalogcliente.ui.screens.LoginScreen
import com.example.rutalogcliente.ui.screens.RegistroScreen
import com.example.rutalogcliente.ui.screens.SplashScreen
import com.example.rutalogcliente.viewmodel.AuthViewModel
import com.example.rutalogcliente.viewmodel.EnvioViewModel

object Rutas {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val HOME = "home"
    const val LISTA = "lista"
    const val DETALLE = "detalle/{envioId}"
    const val FORMULARIO = "formulario?envioId={envioId}"

    fun detalle(id: Int) = "detalle/$id"
    fun formulario(id: Int? = null) = if (id == null) "formulario" else "formulario?envioId=$id"
}

/** Splash → Login / Registro → Inicio del cliente → Mis envíos / Registrar → Seguimiento. */
@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    envioViewModel: EnvioViewModel,
    navController: NavHostController = rememberNavController()
) {
    val auth by authViewModel.estado.collectAsState()
    val nombreUsuario = auth.usuario?.nombre.orEmpty()

    val irAPestana: (PestanaCliente) -> Unit = { pestana ->
        val destino = when (pestana) {
            PestanaCliente.INICIO -> Rutas.HOME
            PestanaCliente.REGISTRAR -> Rutas.formulario()
            PestanaCliente.MIS_ENVIOS -> Rutas.LISTA
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
                    navController.navigate(Rutas.LOGIN) {
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
                        // RFA05: tras el login va a la pantalla principal del cliente.
                        navController.navigate(Rutas.HOME) {
                            popUpTo(Rutas.LOGIN) { inclusive = true }
                        }
                    }
                },
                onIrARegistro = {
                    authViewModel.limpiarMensajes()
                    navController.navigate(Rutas.REGISTRO)
                },
                onLimpiarMensajes = authViewModel::limpiarMensajes
            )
        }

        composable(Rutas.REGISTRO) {
            RegistroScreen(
                estado = auth,
                onRegistrar = { nombre, correo, clave, confirmacion ->
                    authViewModel.registrar(nombre, correo, clave, confirmacion) {
                        navController.popBackStack()
                    }
                },
                onVolver = {
                    authViewModel.limpiarMensajes()
                    navController.popBackStack()
                },
                onLimpiarMensajes = authViewModel::limpiarMensajes
            )
        }

        composable(Rutas.HOME) {
            SesionRequerida(hayUsuario = auth.usuario != null, onSinSesion = cerrarSesion)
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
            SesionRequerida(hayUsuario = auth.usuario != null, onSinSesion = cerrarSesion)
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

        composable(
            route = Rutas.DETALLE,
            arguments = listOf(navArgument("envioId") { type = NavType.IntType })
        ) { entrada ->
            SesionRequerida(hayUsuario = auth.usuario != null, onSinSesion = cerrarSesion)
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
            SesionRequerida(hayUsuario = auth.usuario != null, onSinSesion = cerrarSesion)
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
                envioEditado = if (esEdicion) envioEditado else null,
                esEdicion = esEdicion,
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

/** Si Android cerró la app y se perdió la sesión, vuelve al Login en vez de mostrar datos sin usuario. */
@Composable
private fun SesionRequerida(hayUsuario: Boolean, onSinSesion: () -> Unit) {
    LaunchedEffect(hayUsuario) {
        if (!hayUsuario) onSinSesion()
    }
}
