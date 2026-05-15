package com.example.barberia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import com.example.barberia.ui.auth.*
import com.example.barberia.ui.theme.BarberiaTheme
import com.example.barberia.utils.SessionManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

// ── Rutas de navegación ──────────────────────────────────────────────────────
// Nunca escribas strings de navegación sueltos en el código — siempre usa esto
object Routes {
    const val LOGIN           = "login"
    const val REGISTER        = "register"         // ← nueva ruta pantalla completa
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
                val navController = rememberNavController()

                // null = verificando sesión, valor = destino decidido
                var startDestination by remember { mutableStateOf<String?>(null) }

                // Al arrancar la app lee el rol guardado en disco
                // Si hay sesión activa va directo al dashboard del rol
                // Si no hay sesión va al Login
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
                        .background(ColorFondo)
                ) {
                    if (startDestination == null) {
                        // Spinner mínimo mientras verifica sesión en disco
                        CircularProgressIndicator(
                            color = ColorRojo,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        NavHost(
                            navController = navController,
                            startDestination = startDestination!!,
                            // Animación de transición entre pantallas
                            enterTransition = {
                                fadeIn(animationSpec = tween(300)) +
                                        slideInHorizontally(
                                            animationSpec = tween(300),
                                            initialOffsetX = { it / 4 }
                                        )
                            },
                            exitTransition = {
                                fadeOut(animationSpec = tween(200))
                            },
                            popEnterTransition = {
                                fadeIn(animationSpec = tween(300)) +
                                        slideInHorizontally(
                                            animationSpec = tween(300),
                                            initialOffsetX = { -it / 4 }
                                        )
                            },
                            popExitTransition = {
                                fadeOut(animationSpec = tween(200)) +
                                        slideOutHorizontally(
                                            animationSpec = tween(300),
                                            targetOffsetX = { it / 4 }
                                        )
                            }
                        ) {

                            // ── Login ────────────────────────────────────
                            composable(Routes.LOGIN) {
                                LoginScreen(
                                    authRepository = authRepository,
                                    onLoginSuccess = { rol ->
                                        // Navega al dashboard y limpia el backstack
                                        // (no puede volver al Login con el botón atrás)
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
                                        // Navega a la pantalla completa de registro
                                        navController.navigate(Routes.REGISTER)
                                    }
                                )
                            }

                            // ── Registro ─────────────────────────────────
                            // Pantalla completa separada del Login
                            composable(Routes.REGISTER) {
                                RegisterScreen(
                                    authRepository = authRepository,
                                    onRegistroExitoso = { correo ->
                                        // Vuelve al Login con el correo prellenado
                                        // (el usuario solo necesita poner la contraseña)
                                        navController.navigate(Routes.LOGIN) {
                                            popUpTo(Routes.LOGIN) { inclusive = true }
                                        }
                                    },
                                    onVolver = {
                                        navController.popBackStack()
                                    }
                                )
                            }

                            // ── Dashboard Cliente ─────────────────────────
                            composable(Routes.CLIENTE_HOME) {
                                val scope = rememberCoroutineScope()
                                val idUsuario by sessionManager.id
                                    .collectAsStateWithLifecycle(initialValue = 0L)
                                val nombre by sessionManager.nombre
                                    .collectAsStateWithLifecycle(initialValue = "Cliente")

                                ClienteScreen(
                                    apiService = RetrofitClient.apiService,
                                    idUsuario = idUsuario ?: 0L,
                                    nombre = nombre ?: "Cliente",
                                    onLogout = {
                                        scope.launch {
                                            authRepository.logout()
                                            navController.navigate(Routes.LOGIN) {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        }
                                    }
                                )
                            }

                            // ── Dashboard Barbero ─────────────────────────
                            // Por implementar en el siguiente bloque
                            composable(Routes.BARBERO_HOME) {
                                val scope = rememberCoroutineScope()
                                val nombre by sessionManager.nombre
                                    .collectAsStateWithLifecycle(initialValue = "Barbero")
                                val idUsuario by sessionManager.id
                                    .collectAsStateWithLifecycle(initialValue = 0L)

                                PlaceholderDashboard(
                                    titulo = "Barbero",
                                    subtitulo = nombre ?: "Barbero",
                                    colorAccento = ColorAzul,
                                    onLogout = {
                                        scope.launch {
                                            authRepository.logout()
                                            navController.navigate(Routes.LOGIN) {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        }
                                    }
                                )
                            }

                            // ── Dashboard Administrador ───────────────────
                            composable(Routes.ADMIN_HOME) {
                                val scope = rememberCoroutineScope()
                                val nombre by sessionManager.nombre
                                    .collectAsStateWithLifecycle(initialValue = "Administrador")

                                PlaceholderDashboard(
                                    titulo = "Administrador",
                                    subtitulo = nombre ?: "Administrador",
                                    colorAccento = ColorAzulClaro,
                                    onLogout = {
                                        scope.launch {
                                            authRepository.logout()
                                            navController.navigate(Routes.LOGIN) {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        }
                                    }
                                )
                            }

                            // ── Dashboard SuperAdmin ──────────────────────
                            composable(Routes.SUPERADMIN_HOME) {
                                val scope = rememberCoroutineScope()
                                val nombre by sessionManager.nombre
                                    .collectAsStateWithLifecycle(initialValue = "SuperAdmin")

                                PlaceholderDashboard(
                                    titulo = "SuperAdmin",
                                    subtitulo = nombre ?: "Admin Barbería",
                                    colorAccento = Color(0xFFD4A017), // dorado
                                    onLogout = {
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

// ── Placeholder temporal mientras se construyen los dashboards ───────────────
// Muestra el rol con su color de acento y un botón de logout funcional
// Se reemplaza en los siguientes bloques con el dashboard real
@Composable
fun PlaceholderDashboard(
    titulo: String,
    subtitulo: String,
    colorAccento: androidx.compose.ui.graphics.Color,
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorFondo),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Filled.ContentCut,
                contentDescription = null,
                tint = colorAccento,
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = "Dashboard $titulo",
                color = colorAccento,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitulo,
                color = ColorTextoSub,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onLogout,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorTextoSub),
                border = androidx.compose.foundation.BorderStroke(1.dp, ColorBorde)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar sesión")
            }
        }
    }
}