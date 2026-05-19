package com.example.barberia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.barberia.model.RolEnum
import com.example.barberia.network.AuthRepository
import com.example.barberia.network.RetrofitClient
import com.example.barberia.ui.auth.ClienteScreen
import com.example.barberia.ui.auth.LoginScreen
import com.example.barberia.ui.auth.RegisterScreen
import com.example.barberia.ui.screens.AdminScreen
import com.example.barberia.ui.screens.BarberoDetalleScreen
import com.example.barberia.ui.screens.BarberoScreen
import com.example.barberia.ui.screens.PerfilScreen
import com.example.barberia.ui.screens.ServicioDetalleScreen
import com.example.barberia.ui.screens.SuperAdminScreen
import com.example.barberia.ui.theme.*
import com.example.barberia.utils.SessionManager
import kotlinx.coroutines.launch

// ── Rutas de navegación ───────────────────────────────────────────────────────
object Routes {
    const val LOGIN           = "login"
    const val REGISTER        = "register"
    const val CLIENTE_HOME    = "cliente_home"
    const val BARBERO_HOME    = "barbero_home"
    const val ADMIN_HOME      = "admin_home"
    const val SUPERADMIN_HOME = "superadmin_home"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sessionManager = SessionManager.getInstance(this)
        RetrofitClient.init(sessionManager)
        val authRepository = AuthRepository(RetrofitClient.apiService, sessionManager)

        setContent {
            BarberiaTheme {
                val colores = LocalBarberiaColores.current
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()

                // listo = false mientras limpia la sesión al arrancar
                // Así evitamos parpadeo o navegación incorrecta
                var listo by remember { mutableStateOf(false) }

                // Al abrir la app SIEMPRE limpiamos la sesión y vamos al Login
                // Esto resuelve el problema de Render dormido:
                // si el token expiró o el servidor está frío,
                // el usuario hace login fresco y obtiene un token válido
                LaunchedEffect(Unit) {
                    authRepository.logout()
                    listo = true
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colores.fondo)
                ) {
                    if (!listo) {
                        // Spinner mínimo mientras limpia la sesión (~100ms)
                        CircularProgressIndicator(
                            color    = ColorRojo,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        NavHost(
                            navController    = navController,
                            startDestination = Routes.LOGIN,
                            enterTransition = {
                                fadeIn(tween(300)) + slideInHorizontally(
                                    tween(300)) { it / 4 }
                            },
                            exitTransition = {
                                fadeOut(tween(200))
                            },
                            popEnterTransition = {
                                fadeIn(tween(300)) + slideInHorizontally(
                                    tween(300)) { -it / 4 }
                            },
                            popExitTransition = {
                                fadeOut(tween(200)) + slideOutHorizontally(
                                    tween(300)) { it / 4 }
                            }
                        ) {

                            // ── Login ─────────────────────────────────────
                            composable(Routes.LOGIN) {
                                LoginScreen(
                                    authRepository       = authRepository,
                                    onLoginSuccess       = { rol ->
                                        val dest = when (rol) {
                                            RolEnum.CLIENTE       -> Routes.CLIENTE_HOME
                                            RolEnum.BARBERO       -> Routes.BARBERO_HOME
                                            RolEnum.ADMINISTRADOR -> Routes.ADMIN_HOME
                                            RolEnum.SUPERADMIN    -> Routes.SUPERADMIN_HOME
                                        }
                                        navController.navigate(dest) {
                                            popUpTo(Routes.LOGIN) { inclusive = true }
                                        }
                                    },
                                    onNavigateToRegister = {
                                        navController.navigate(Routes.REGISTER)
                                    }
                                )
                            }

                            // ── Registro ──────────────────────────────────
                            composable(Routes.REGISTER) {
                                RegisterScreen(
                                    authRepository    = authRepository,
                                    onRegistroExitoso = { _ ->
                                        navController.navigate(Routes.LOGIN) {
                                            popUpTo(Routes.LOGIN) { inclusive = true }
                                        }
                                    },
                                    onVolver = { navController.popBackStack() }
                                )
                            }

                            // ── Detalle Barbero ───────────────────────────────────────
                            composable("barbero_detalle/{idBarbero}") { backStackEntry ->
                                val idBarbero = backStackEntry.arguments?.getString("idBarbero")?.toLongOrNull() ?: 0L
                                // Nota: Asegúrate de tener creada la pantalla BarberoDetalleScreen
                                BarberoDetalleScreen(
                                    apiService = RetrofitClient.apiService,
                                    idBarbero  = idBarbero,
                                    onVolver   = { navController.popBackStack() }
                                )
                            }

                            // ── Detalle Servicio ──────────────────────────────────────
                            composable("servicio_detalle/{idServicio}/{nombre}/{precio}/{duracion}/{descripcion}") { backStackEntry ->
                                val args = backStackEntry.arguments
                                // Nota: Asegúrate de tener creada la pantalla ServicioDetalleScreen
                                ServicioDetalleScreen(
                                    servicio = com.example.barberia.model.ServicioDTO(
                                        idServicio      = args?.getString("idServicio")?.toLongOrNull(),
                                        nombre          = args?.getString("nombre") ?: "",
                                        precio          = args?.getString("precio")?.toDoubleOrNull() ?: 0.0,
                                        duracionMinutos = args?.getString("duracion")?.toIntOrNull() ?: 0,
                                        descripcion     = args?.getString("descripcion")?.replace("_", " ")?.takeIf { it != "null" }
                                    ),
                                    onVolver = { navController.popBackStack() }
                                )
                            }

                            // ── Cliente ───────────────────────────────────
                            composable(Routes.CLIENTE_HOME) {
                                val idUsuario by sessionManager.id
                                    .collectAsStateWithLifecycle(initialValue = 0L)
                                val nombre by sessionManager.nombre
                                    .collectAsStateWithLifecycle(initialValue = "Cliente")

                                ClienteScreen(
                                    apiService = RetrofitClient.apiService,
                                    idUsuario  = idUsuario ?: 0L,
                                    nombre     = nombre ?: "Cliente",
                                    navController = navController,
                                    onLogout   = {
                                        scope.launch {
                                            authRepository.logout()
                                            navController.navigate(Routes.LOGIN) {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        }
                                    }
                                )
                            }

                            // ── Barbero ───────────────────────────────────
                            composable(Routes.BARBERO_HOME) {
                                val idUsuario by sessionManager.id
                                    .collectAsStateWithLifecycle(initialValue = 0L)
                                val idBarbero by sessionManager.idBarbero
                                    .collectAsStateWithLifecycle(initialValue = 0L)
                                val nombre by sessionManager.nombre
                                    .collectAsStateWithLifecycle(initialValue = "Barbero")

                                BarberoScreen(
                                    apiService = RetrofitClient.apiService,
                                    idBarbero  = idBarbero ?: 0L,
                                    idUsuario  = idUsuario ?: 0L,
                                    nombre     = nombre ?: "Barbero",
                                    onLogout   = {
                                        scope.launch {
                                            authRepository.logout()
                                            navController.navigate(Routes.LOGIN) {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        }
                                    }
                                )
                            }

                            // ── Admin ─────────────────────────────────────
                            composable(Routes.ADMIN_HOME) {
                                val nombre by sessionManager.nombre
                                    .collectAsStateWithLifecycle(
                                        initialValue = "Administrador")

                                AdminScreen(
                                    apiService = RetrofitClient.apiService,
                                    nombre     = nombre ?: "Administrador",
                                    onLogout   = {
                                        scope.launch {
                                            authRepository.logout()
                                            navController.navigate(Routes.LOGIN) {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        }
                                    }
                                )
                            }

                            // ── SuperAdmin ────────────────────────────────
                            composable(Routes.SUPERADMIN_HOME) {
                                val nombre by sessionManager.nombre
                                    .collectAsStateWithLifecycle(
                                        initialValue = "SuperAdmin")

                                SuperAdminScreen(
                                    apiService = RetrofitClient.apiService,
                                    nombre     = nombre ?: "Admin Barbería",
                                    onLogout   = {
                                        scope.launch {
                                            authRepository.logout()
                                            navController.navigate(Routes.LOGIN) {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        }
                                    }
                                )
                            }

                            composable("perfil/{idUsuario}") { backStackEntry ->
                                val idUsuario = backStackEntry.arguments
                                    ?.getString("idUsuario")?.toLongOrNull() ?: 0L
                                PerfilScreen(
                                    apiService = RetrofitClient.apiService,
                                    idUsuario  = idUsuario,
                                    onVolver   = { navController.popBackStack() },
                                    onLogout   = {
                                        scope.launch {
                                            authRepository.logout()
                                            navController.navigate(Routes.LOGIN) {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}