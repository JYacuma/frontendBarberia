package com.example.barberia.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

// ── Paleta de colores inspirada en el logo de barbería ──────────────────────
// Rojo clásico de barbería
val ColorRojo        = Color(0xFFC0272D)
val ColorRojoClaro   = Color(0xFFE05555)
val ColorRojoOscuro  = Color(0xFF8B1A1E)
// Azul clásico de barbería
val ColorAzul        = Color(0xFF1B4F9B)
val ColorAzulClaro   = Color(0xFF4A80D4)
// Fondos oscuros
val ColorFondo       = Color(0xFF0A0A0A)
val ColorSuperficie  = Color(0xFF141414)
val ColorSuperficie2 = Color(0xFF1E1E1E)
val ColorBorde       = Color(0xFF2A2A2A)
// Textos
val ColorTexto       = Color(0xFFF0F0F0)
val ColorTextoSub    = Color(0xFF888888)
val ColorError       = Color(0xFFCF6679)
// Blanco para el polo
val ColorBlanco      = Color(0xFFFFFFFF)

@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    onLoginSuccess: (RolEnum) -> Unit,
    onNavigateToRegister: () -> Unit   // ← nueva pantalla completa de registro
) {
    val viewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.factory(authRepository)
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Animación de entrada del logo al cargar pantalla
    var logoVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { logoVisible = true }

    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(600),
        label = "logoAlpha"
    )

    // Navega al dashboard correcto cuando el login es exitoso
    LaunchedEffect(uiState.loginSuccess) {
        uiState.loginSuccess?.let { onLoginSuccess(it.rol) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorFondo)
    ) {
        // Franja decorativa roja superior (evoca el polo de barbería)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(ColorRojo, ColorAzul, ColorBlanco, ColorRojo, ColorAzul)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // ── Logo con animación de entrada ────────────────────────────
            Box(
                modifier = Modifier
                    .scale(logoScale)
                    .then(Modifier.then(Modifier.graphicsLayer { alpha = logoAlpha }))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Círculo con ícono de tijeras + borde rojo
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(50))
                            .background(ColorSuperficie2),
                        contentAlignment = Alignment.Center
                    ) {
                        // Franja tipo polo de barbería como borde
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            ColorRojo.copy(alpha = 0.3f),
                                            Color.Transparent,
                                            ColorAzul.copy(alpha = 0.3f)
                                        )
                                    )
                                )
                        )
                        Icon(
                            imageVector = Icons.Filled.ContentCut,
                            contentDescription = "Logo Barbería",
                            tint = ColorRojo,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "BARBERÍA",
                        color = ColorTexto,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 5.sp
                    )

                    // Franja decorativa tipo polo bajo el título
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.width(120.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).height(3.dp)
                            .clip(RoundedCornerShape(2.dp)).background(ColorRojo))
                        Box(modifier = Modifier.weight(1f).height(3.dp)
                            .clip(RoundedCornerShape(2.dp)).background(ColorBlanco))
                        Box(modifier = Modifier.weight(1f).height(3.dp)
                            .clip(RoundedCornerShape(2.dp)).background(ColorAzul))
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sistema de gestión de citas",
                        color = ColorTextoSub,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(44.dp))

            // ── Tarjeta del formulario ───────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ColorSuperficie),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                // Borde rojo en la parte superior de la tarjeta
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(ColorRojo, ColorAzul)
                                )
                            )
                    )

                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Iniciar sesión",
                            color = ColorTexto,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ingresa con tu correo y contraseña",
                            color = ColorTextoSub,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Campo Email
                        BarberiaTextField(
                            value = email,
                            onValueChange = { email = it; viewModel.clearError() },
                            label = "Correo electrónico",
                            leadingIcon = Icons.Filled.Email,
                            accentColor = ColorRojo,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Campo Contraseña
                        BarberiaTextField(
                            value = password,
                            onValueChange = { password = it; viewModel.clearError() },
                            label = "Contraseña",
                            leadingIcon = Icons.Filled.Lock,
                            accentColor = ColorRojo,
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
                                    viewModel.login(email, password)
                                }
                            )
                        )

                        // Mensaje de error con animación
                        AnimatedVisibility(
                            visible = uiState.errorMessage != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Text(
                                text = uiState.errorMessage ?: "",
                                color = ColorError,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Botón Ingresar con animación de escala al presionar
                        BarberiaBoton(
                            texto = "Ingresar",
                            icono = Icons.Filled.Login,
                            isLoading = uiState.isLoading,
                            colorFondo = ColorRojo,
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.login(email, password)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Separador ────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = ColorBorde
                )
                Text(
                    text = "  ¿No tienes cuenta?  ",
                    color = ColorTextoSub,
                    fontSize = 12.sp
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = ColorBorde
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón Registrarse — lleva a pantalla completa
            OutlinedButton(
                onClick = onNavigateToRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ColorAzulClaro
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(listOf(ColorAzul, ColorAzulClaro))
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Crear cuenta nueva",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "¿Eres administrador o barbero?\nUsa las credenciales proporcionadas",
                color = ColorTextoSub,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── Botón principal reutilizable con animación de escala ─────────────────────
// Se usa en Login, Registro y en futuros dashboards
// accentColor define el color del botón según el rol/pantalla
@Composable
fun BarberiaBoton(
    texto: String,
    icono: ImageVector? = null,
    isLoading: Boolean = false,
    colorFondo: Color = ColorRojo,
    onClick: () -> Unit
) {
    // Animación de escala al presionar (efecto "spring" profesional)
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "botonEscala"
    )

    Button(
        onClick = onClick,
        enabled = !isLoading,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(escala),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorFondo,
            disabledContainerColor = colorFondo.copy(alpha = 0.5f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 1.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.5.dp
            )
        } else {
            if (icono != null) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = texto,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ── TextField reutilizable con estilo barbería y acento de color ─────────────
// accentColor cambia según la pantalla: rojo=login, azul=registro, dorado=admin
@Composable
fun BarberiaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    accentColor: Color = ColorRojo,
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
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = if (isPassword && onPasswordToggle != null) {
            {
                IconButton(onClick = onPasswordToggle) {
                    Icon(
                        imageVector = if (passwordVisible)
                            Icons.Filled.Visibility
                        else
                            Icons.Filled.VisibilityOff,
                        contentDescription = null,
                        tint = ColorTextoSub,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible)
            PasswordVisualTransformation()
        else
            VisualTransformation.None,
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = accentColor,
            unfocusedBorderColor    = ColorBorde,
            focusedTextColor        = ColorTexto,
            unfocusedTextColor      = ColorTexto,
            cursorColor             = accentColor,
            focusedContainerColor   = ColorSuperficie2,
            unfocusedContainerColor = ColorSuperficie
        ),
        shape = RoundedCornerShape(12.dp)
    )
}