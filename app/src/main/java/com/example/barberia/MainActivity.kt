package com.example.barberia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.barberia.model.RolEnum
import com.example.barberia.network.AuthRepository
import com.example.barberia.network.RetrofitClient
import com.example.barberia.ui.auth.ClienteScreen
import com.example.barberia.ui.auth.ColorDorado
import com.example.barberia.ui.auth.ColorFondo
import com.example.barberia.ui.auth.LoginScreen
import com.example.barberia.ui.theme.BarberiaTheme
import com.example.barberia.utils.SessionManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

object Routes {
    const val LOGIN           = "login"
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
                var startDestination by remember { mutableStateOf<String?>(null) }

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
                        CircularProgressIndicator(
                            color = ColorDorado,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        NavHost(
                            navController = navController,
                            startDestination = startDestination!!
                        ) {
                            composable(Routes.LOGIN) {
                                LoginScreen(
                                    authRepository = authRepository,
                                    onLoginSuccess = { rol ->
                                        val dest = when (rol) {
                                            RolEnum.CLIENTE       -> Routes.CLIENTE_HOME
                                            RolEnum.BARBERO       -> Routes.BARBERO_HOME
                                            RolEnum.ADMINISTRADOR -> Routes.ADMIN_HOME
                                            RolEnum.SUPERADMIN    -> Routes.SUPERADMIN_HOME
                                        }
                                        navController.navigate(dest) {
                                            popUpTo(Routes.LOGIN) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(Routes.CLIENTE_HOME) {
                                val scope = rememberCoroutineScope()
                                val idUsuario by sessionManager.id.collectAsStateWithLifecycle(initialValue = 0L)
                                val nombre by sessionManager.nombre.collectAsStateWithLifecycle(initialValue = "Cliente")

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

                            // Placeholders — siguiente bloque
                            composable(Routes.BARBERO_HOME) {
                                PlaceholderScreen("Barbero")
                            }
                            composable(Routes.ADMIN_HOME) {
                                PlaceholderScreen("Administrador")
                            }
                            composable(Routes.SUPERADMIN_HOME) {
                                PlaceholderScreen("SuperAdmin")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(rol: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = "Dashboard $rol",
            color = Color(0xFFC9A84C),
            fontSize = androidx.compose.ui.unit.TextUnit(
                20f, androidx.compose.ui.unit.TextUnitType.Sp
            )
        )
    }
}