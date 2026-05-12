package com.example.barberia.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.barberia.model.RolEnum
import com.example.barberia.network.AuthRepository
import com.example.barberia.viewmodel.AuthViewModel

// ── Colores del tema barbería ─────────────────────────────────────────────
val ColorFondo      = Color(0xFF0D0D0D)
val ColorSuperficie = Color(0xFF1A1A1A)
val ColorDorado     = Color(0xFFC9A84C)
val ColorTexto      = Color(0xFFF5F5F5)
val ColorTextoSub   = Color(0xFF9E9E9E)
val ColorError      = Color(0xFFCF6679)

@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    onLoginSuccess: (RolEnum) -> Unit
) {
    val viewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.factory(authRepository)
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    var isLoginMode     by remember { mutableStateOf(true) }
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var nombre          by remember { mutableStateOf("") }
    var confirmPass     by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Navega cuando el login es exitoso
    LaunchedEffect(uiState.loginSuccess) {
        uiState.loginSuccess?.let { onLoginSuccess(it.rol) }
    }

    // Cuando el registro es exitoso vuelve al login
    LaunchedEffect(uiState.registerSuccess) {
        uiState.registerSuccess?.let {
            isLoginMode = true
            email = it.correo
            password = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF111111), Color(0xFF0A0A0A))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(72.dp))

            // ── Logo ──────────────────────────────────────────────────────
            Icon(
                imageVector = Icons.Filled.ContentCut,
                contentDescription = "Logo",
                tint = ColorDorado,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "BARBERÍA",
                color = ColorDorado,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp
            )

            Text(
                text = "Sistema de gestión",
                color = ColorTextoSub,
                fontSize = 13.sp,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // ── Tarjeta formulario ────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ColorSuperficie),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // ── Tabs ──────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF252525))
                    ) {
                        listOf("Ingresar" to true, "Registrarse" to false).forEach { (label, esLogin) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isLoginMode == esLogin) ColorDorado
                                        else Color.Transparent
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                TextButton(
                                    onClick = {
                                        isLoginMode = esLogin
                                        viewModel.clearError()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isLoginMode == esLogin) Color.Black
                                        else ColorTextoSub,
                                        fontWeight = if (isLoginMode == esLogin) FontWeight.Bold
                                        else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // ── Campo Nombre (solo registro) ──────────────────────
                    AnimatedVisibility(
                        visible = !isLoginMode,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            BarberiaTextField(
                                value = nombre,
                                onValueChange = { nombre = it; viewModel.clearError() },
                                label = "Nombre completo",
                                leadingIcon = Icons.Filled.Person,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                )
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // ── Campo Email ───────────────────────────────────────
                    BarberiaTextField(
                        value = email,
                        onValueChange = { email = it; viewModel.clearError() },
                        label = "Correo electrónico",
                        leadingIcon = Icons.Filled.Email,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Campo Contraseña ──────────────────────────────────
                    BarberiaTextField(
                        value = password,
                        onValueChange = { password = it; viewModel.clearError() },
                        label = "Contraseña",
                        leadingIcon = Icons.Filled.Lock,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onPasswordToggle = { passwordVisible = !passwordVisible },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (isLoginMode) ImeAction.Done else ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (isLoginMode) viewModel.login(email, password)
                            },
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    // ── Campo Confirmar (solo registro) ───────────────────
                    AnimatedVisibility(
                        visible = !isLoginMode,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            BarberiaTextField(
                                value = confirmPass,
                                onValueChange = { confirmPass = it; viewModel.clearError() },
                                label = "Confirmar contraseña",
                                leadingIcon = Icons.Filled.LockOpen,
                                isPassword = true,
                                passwordVisible = passwordVisible,
                                onPasswordToggle = { passwordVisible = !passwordVisible },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        viewModel.register(nombre, email, password, confirmPass)
                                    }
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Error ─────────────────────────────────────────────
                    AnimatedVisibility(visible = uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = ColorError,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Botón principal ───────────────────────────────────
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (isLoginMode) viewModel.login(email, password)
                            else viewModel.register(nombre, email, password, confirmPass)
                        },
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorDorado,
                            disabledContainerColor = ColorDorado.copy(alpha = 0.5f)
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = if (isLoginMode) "Ingresar" else "Crear cuenta",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isLoginMode) {
                Text(
                    text = "¿Eres administrador? Usa las credenciales\nproporcionadas por tu barbería",
                    color = ColorTextoSub,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── TextField reutilizable con estilo barbería ────────────────────────────
@Composable
fun BarberiaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordToggle: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = ColorTextoSub, fontSize = 13.sp) },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = ColorDorado,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = if (isPassword && onPasswordToggle != null) {
            {
                IconButton(onClick = onPasswordToggle) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility
                        else Icons.Filled.VisibilityOff,
                        contentDescription = null,
                        tint = ColorTextoSub,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible)
            PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor     = ColorDorado,
            unfocusedBorderColor   = Color(0xFF3A3A3A),
            focusedTextColor       = ColorTexto,
            unfocusedTextColor     = ColorTexto,
            cursorColor            = ColorDorado,
            focusedContainerColor  = Color(0xFF202020),
            unfocusedContainerColor = Color(0xFF1C1C1C)
        ),
        shape = RoundedCornerShape(12.dp)
    )
}