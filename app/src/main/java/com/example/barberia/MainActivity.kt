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
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.example.barberia.ui.screens.BarberoScreen
import com.example.barberia.ui.screens.NotificacionesScreen
import com.example.barberia.ui.screens.SuperAdminScreen
import com.example.barberia.ui.theme.*
import com.example.barberia.utils.SessionManager
import kotlinx.coroutines.launch

object Routes {
    const val LOGIN           = "login"
    const val REGISTER        = "register"
    const val CLIENTE_HOME    = "cliente_home"
    const val BARBERO_HOME    = "barbero_home"
    const val ADMIN_HOME      = "admin_home"
    const val SUPERADMIN_HOME = "superadmin_home"
    const val NOTIFICACIONES  = "notificaciones"
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

                SideEffect {
                    val wic = WindowInsetsControllerCompat(window, window.decorView)
                    if (colores.esModoOscuro) {
                        window.statusBarColor = Color(0xFF1A1A2E).hashCode()
                        wic.isAppearanceLightStatusBars = false
                    } else {
                        window.statusBarColor = Color(0xFFF8F9FA).hashCode()
                        wic.isAppearanceLightStatusBars = true
                    }
                }

                var listo by remember { mutableStateOf(false) }

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

                            composable(Routes.CLIENTE_HOME) {
                                val idUsuario by sessionManager.id
                                    .collectAsStateWithLifecycle(initialValue = 0L)
                                val nombre by sessionManager.nombre
                                    .collectAsStateWithLifecycle(initialValue = "Cliente")

                                ClienteScreen(
                                    apiService = RetrofitClient.apiService,
                                    idUsuario  = idUsuario ?: 0L,
                                    nombre     = nombre ?: "Cliente",
                                    onLogout   = {
                                        scope.launch {
                                            authRepository.logout()
                                            navController.navigate(Routes.LOGIN) {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        }
                                    },
                                    onNavigateToNotificaciones = {
                                        navController.navigate(Routes.NOTIFICACIONES)
                                    }
                                )
                            }

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
                                    },
                                    onNavigateToNotificaciones = {
                                        navController.navigate(Routes.NOTIFICACIONES)
                                    }
                                )
                            }

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
                                    },
                                    onNavigateToNotificaciones = {
                                        navController.navigate(Routes.NOTIFICACIONES)
                                    }
                                )
                            }

                            composable(Routes.SUPERADMIN_HOME) {
                                val nombre by sessionManager.nombre
                                    .collectAsStateWithLifecycle(
                                        initialValue = "SuperAdmin")

                                SuperAdminScreen(
                                    apiService = RetrofitClient.apiService,
                                    nombre     = nombre ?: "Admin Barberia",
                                    onLogout   = {
                                        scope.launch {
                                            authRepository.logout()
                                            navController.navigate(Routes.LOGIN) {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        }
                                    },
                                    onNavigateToNotificaciones = {
                                        navController.navigate(Routes.NOTIFICACIONES)
                                    }
                                )
                            }

                            composable(Routes.NOTIFICACIONES) {
                                NotificacionesScreen(
                                    apiService = RetrofitClient.apiService,
                                    onVolver   = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── v1.6 CHANGELOG ──────────────────────────────────────────────────────────
// [GENERAL] Status bar adaptativa: fondo oscuro (#1A1A2E) con iconos claros
//           en modo oscuro, fondo claro (#F8F9FA) con iconos oscuros en claro
// [GENERAL] Pull-to-refresh (PullToRefreshBox) reemplaza botones "Actualizar"
//           en todas las tabs de todos los roles
// [GENERAL] Animaciones spring (DampingRatioMediumBouncy) en botones (scale
//           0.97f) y tarjetas clickables via MutableInteractionSource
// [GENERAL] Menu lateral eliminado del header: avatar + nombre + subtitulo
//           + campana de notificaciones con badge rojo
// [GENERAL] Colores modo oscuro: fondo #1A1A2E, superficie #16213E,
//           superficie2 #0F3460, borde #2A2A4A
// [GENERAL] Colores modo claro: fondo #F8F9FA, superficie #FFFFFF,
//           superficie2 #F0F2F5, borde #E0E4E8
// [GENERAL] Botones con shadow(4.dp, RoundedCornerShape(14.dp), ambientColor)
// [GENERAL] Tarjetas listas horizontales: width=110.dp fijo,
//           maxLines=1, TextOverflow.Ellipsis
// [GENERAL] GsonBuilder().setLenient() en RetrofitClient para UTF-8
// [GENERAL] Normalizer para iniciales de avatar (soporta tildes)
// [LOGIN] Eliminado texto "Sistema de gestion de citas"
// [REGISTER] Telefono como campo opcional, se envia null si vacio
// [CLIENTE] 3 tabs: Inicio (barberos/servicios clickables), Agendar (filtros
//           especialidad, dialogo confirmacion barbero, orden servicio, fecha
//           hoy+8, bloques 30min, dialogo "Cita agendada"), Mis Citas (filtros
//           estado, motivos cancelacion predeterminados, dialogo reseña 1-5)
// [CLIENTE] Tab Perfil eliminada
// [BARBERO] Tab Hoy: stats clickables filtran citas, tarjetas detalle dialogo
// [BARBERO] Tab Agenda: chips visuales para dia descanso + bloque descanso 1h
// [BARBERO] Tab Resenas: agrupadas por cliente con promedio general
// [BARBERO] Tab Horarios: semana con bloques ocupados (cliente+servicio)
// [ADMIN] Tab Barberos: boton editar con dialog, manejo error 409
// [ADMIN] Tab Servicios: filtros pop/precio, detalle barberos+clientes
// [ADMIN] Tab Horarios: lista barberos, semana con detalle ocupados,
//         eliminar cita con notificacion
// [ADMIN] Tab Resenas: promedio general, filtro por barbero, detalle dialogo
// [SUPERADMIN] Tab Usuarios: boton editar, manejo error 409
// [SUPERADMIN] Tab Barberos: eliminar barbero cancela citas pendientes y
//              notifica clientes "Tu cita fue cancelada por motivos mayores"
// [SUPERADMIN] Tab Servicios: filtros y detalle como Admin
// [SUPERADMIN] Tab Citas: vista detallada con filtros estado
// ────────────────────────────────────────────────────────────────────────────
