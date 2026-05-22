package com.example.barberia.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.barberia.model.UsuarioDTO
import com.example.barberia.network.ApiService
import com.example.barberia.ui.auth.BarberiaBoton
import com.example.barberia.ui.auth.BarberiaTextField
import com.example.barberia.ui.theme.LocalBarberiaColores
import kotlinx.coroutines.launch

private val AzulPerfil = Color(0xFF1E88E5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    apiService: ApiService,
    idUsuario: Long,
    onVolver: () -> Unit
) {
    val colores = LocalBarberiaColores.current
    val scope = rememberCoroutineScope()
    val snackbarState = remember { SnackbarHostState() }

    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var cargadoOk by remember { mutableStateOf(false) }

    LaunchedEffect(idUsuario) {
        try {
            val response = apiService.getUsuarioById(idUsuario)
            if (response.isSuccessful) {
                val user = response.body()
                if (user != null) {
                    nombre = user.nombre
                    telefono = user.telefono ?: ""
                    cargadoOk = true
                }
            } else {
                snackbarState.showSnackbar("Error al cargar perfil")
            }
        } catch (_: Exception) {
            snackbarState.showSnackbar("Error de conexión")
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        containerColor = colores.fondo,
        snackbarHost = {
            SnackbarHost(snackbarState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = colores.superficie,
                    contentColor = colores.texto,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text("Mi Perfil", fontWeight = FontWeight.Bold, color = colores.texto)
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = colores.texto
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colores.superficie
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AzulPerfil, strokeWidth = 2.5.dp)
            }
        } else if (!cargadoOk) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No se pudo cargar el perfil", color = colores.textoSub)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Edita tu información",
                    color = colores.textoSub,
                    fontSize = 13.sp
                )

                BarberiaTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = "Nombre",
                    leadingIcon = Icons.Filled.Person,
                    accentColor = AzulPerfil,
                    colores = colores
                )

                BarberiaTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = "Teléfono",
                    leadingIcon = Icons.Filled.Phone,
                    accentColor = AzulPerfil,
                    colores = colores,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Spacer(modifier = Modifier.height(8.dp))

                BarberiaBoton(
                    texto = "Guardar cambios",
                    icono = Icons.Filled.Save,
                    isLoading = isSaving,
                    colorFondo = AzulPerfil,
                    onClick = {
                        if (nombre.isNotBlank()) {
                            scope.launch {
                                isSaving = true
                                try {
                                    val updated = UsuarioDTO(
                                        idUsuario = idUsuario,
                                        nombre = nombre,
                                        correo = "",
                                        telefono = telefono.ifBlank { null },
                                        rol = com.example.barberia.model.RolEnum.CLIENTE,
                                        activo = true
                                    )
                                    val res = apiService.updateUsuario(idUsuario, updated)
                                    if (res.isSuccessful) {
                                        snackbarState.showSnackbar("Perfil actualizado")
                                    } else {
                                        snackbarState.showSnackbar("Error al guardar")
                                    }
                                } catch (_: Exception) {
                                    snackbarState.showSnackbar("Error de conexión")
                                } finally {
                                    isSaving = false
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
