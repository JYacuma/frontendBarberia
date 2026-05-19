package com.example.barberia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.barberia.ui.screens.SuperAdminScreen
import com.example.barberia.ui.theme.*
import com.example.barberia.utils.SessionManager
import kotlinx.coroutines.flow.firstOrNull
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
            // BarberiaTheme aplica el esquema Material3 + provee LocalBarberiaColores
            // a todos los composables hijo — así cualquier pantalla puede leer
            // LocalBarberiaColores.current para adaptarse al modo claro u oscuro
            BarberiaTheme {
                val colores = LocalBarberiaColores.current
                val navController = rememberNavController()

                // null = verificando sesión guardada en disco
                var startDestination by remember { mutableStateOf<String?>(null) }

                // Al arrancar lee el rol del DataStore
                // Si hay sesión activa navega directo al dashboard del rol
                LaunchedEffect(Unit) {
                    val rol = sessionManager.rol.firstOrNull()
                    startDestination = when (rol) {
                        RolEnum.CLIENTE.name       -> Routes.CLIENTE_HOME
                        RolEnum.BARBERO.name       -> Routes.BARBERO_HOME
                        RolEnum.ADMINISTRADOR.name -> Routes.ADMIN_HOME
                        RolEnum.SUPERADMIN.name    -> Routes.SUPERADMIN_HOME
                        else                       -> Routes.LOGIN
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colores.fondo)
                ) {
                    if (startDestination == null) {
                        // Spinner mínimo mientras verifica la sesión guardada
                        CircularProgressIndicator(
                            color    = ColorRojo,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        NavHost(
                            navController      = navController,
                            startDestination   = startDestination!!,
                            // Animación de transición entre pantallas:
                            // fade + slide suave al navegar hacia adelante
                            // fade + slide inverso al volver atrás
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
                                    authRepository    = authRepository,
                                    onLoginSuccess    = { rol ->
                                        // Navega al dashboard y limpia el backstack
                                        // El usuario no puede volver al Login con atrás
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

                            // ── Registro — pantalla completa separada ──────
                            composable(Routes.REGISTER) {
                                RegisterScreen(
                                    authRepository    = authRepository,
                                    onRegistroExitoso = { _ ->
                                        // Vuelve al Login después de registrarse
                                        navController.navigate(Routes.LOGIN) {
                                            popUpTo(Routes.LOGIN) { inclusive = true }
                                        }
                                    },
                                    onVolver = { navController.popBackStack() }
                                )
                            }

                            // ── Dashboard Cliente — rojo ───────────────────
                            composable(Routes.CLIENTE_HOME) {
                                val scope = rememberCoroutineScope()
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
                                    }
                                )
                            }

                            // ── Dashboard Barbero — azul ───────────────────
                            // Placeholder temporal hasta construir BarberoScreen
                            composable(Routes.BARBERO_HOME) {
                                val scope = rememberCoroutineScope()
                                val nombre by sessionManager.nombre
                                    .collectAsStateWithLifecycle(initialValue = "Barbero")
                                val idUsuario by sessionManager.id
                                    .collectAsStateWithLifecycle(initialValue = 0L)
                                // idBarbero real guardado en el login — ya no es temporal
                                val idBarbero by sessionManager.idBarbero
                                    .collectAsStateWithLifecycle(initialValue = 0L)

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

                            // ── Dashboard Administrador — azul claro ────────
                            composable(Routes.ADMIN_HOME) {
                                val scope = rememberCoroutineScope()
                                val nombre by sessionManager.nombre
                                    .collectAsStateWithLifecycle(initialValue = "Administrador")

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
                            // ── Dashboard SuperAdmin — dorado ───────────────
                            composable(Routes.SUPERADMIN_HOME) {
                                val scope = rememberCoroutineScope()
                                val nombre by sessionManager.nombre
                                    .collectAsStateWithLifecycle(initialValue = "SuperAdmin")

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
                        }
                    }
                }
            }
        }
    }
}

// ── Placeholder temporal con diseño adaptado al tema ─────────────────────────
// Muestra el rol con su color de acento, respeta modo claro/oscuro
// Se reemplaza con el dashboard real en el siguiente paso
@Composable
fun PlaceholderDashboard(
    titulo: String,
    subtitulo: String,
    colorAccento: Color,
    colores: BarberiaColores,
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colores.fondo),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Ícono con fondo del color del rol
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .then(
                        Modifier.background(
                            color = colorAccento.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector    = Icons.Filled.ContentCut,
                    contentDescription = null,
                    tint           = colorAccento,
                    modifier       = Modifier.size(40.dp)
                )
            }

            Text(
                text       = "Dashboard $titulo",
                color      = colorAccento,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text     = subtitulo,
                color    = colores.textoSub,
                fontSize = 14.sp
            )
            Text(
                text     = "En construcción...",
                color    = colores.textoSub.copy(alpha = 0.5f),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onLogout,
                shape   = RoundedCornerShape(12.dp),
                colors  = ButtonDefaults.outlinedButtonColors(
                    contentColor = colores.textoSub
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, colores.borde
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar sesión")
            }
        }
    }
}