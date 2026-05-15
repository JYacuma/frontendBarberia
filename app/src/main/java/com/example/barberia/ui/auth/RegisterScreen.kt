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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.barberia.network.AuthRepository
import com.example.barberia.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    authRepository: AuthRepository,
    onRegistroExitoso: (correo: String) -> Unit,
    onVolver: () -> Unit
) {
    val viewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.factory(authRepository)
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    var nombre          by remember { mutableStateOf("") }
    var correo          by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Cuando el registro es exitoso vuelve al login con el correo prellenado
    LaunchedEffect(uiState.registerSuccess) {
        uiState.registerSuccess?.let {
            onRegistroExitoso(it.correo)
        }
    }

    // Indicador de pasos: nombre=1, correo=2, contraseña=3
    // Se activa conforme el usuario llena los campos
    val paso1 = nombre.isNotBlank()
    val paso2 = correo.isNotBlank()
    val paso3 = password.length >= 6

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorFondo)
    ) {
        // Franja decorativa superior con gradiente azul
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(ColorAzul, ColorAzulClaro, ColorBlanco, ColorAzulClaro, ColorAzul)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // ── Header con botón volver ──────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorSuperficie)
            ) {
                // Borde inferior azul
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.BottomStart)
                        .background(
                            Brush.horizontalGradient(listOf(ColorAzul, ColorAzulClaro))
                        )
                )

                Column(
                    modifier = Modifier.padding(
                        top = 52.dp,
                        start = 20.dp,
                        end = 20.dp,
                        bottom = 20.dp
                    )
                ) {
                    // Botón volver con animación
                    val interactionSource = remember {
                        androidx.compose.foundation.interaction.MutableInteractionSource()
                    }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val escalaVolver by animateFloatAsState(
                        targetValue = if (isPressed) 0.92f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy
                        ),
                        label = "volverScale"
                    )

                    IconButton(
                        onClick = onVolver,
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .size(40.dp)
                            .scale(escalaVolver)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ColorSuperficie2)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = ColorTexto,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Crear cuenta",
                        color = ColorTexto,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Completa tus datos para registrarte",
                        color = ColorTextoSub,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Barra de progreso de 3 pasos ─────────────────────
                    // Cada segmento se activa conforme el usuario llena el campo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(paso1, paso2, paso3).forEachIndexed { index, activo ->
                            val colorSegmento by animateColorAsState(
                                targetValue = when {
                                    activo -> ColorAzul
                                    index == 0 -> ColorAzul.copy(alpha = 0.3f)
                                    else -> ColorBorde
                                },
                                animationSpec = tween(300),
                                label = "stepColor$index"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(colorSegmento)
                            )
                        }
                    }
                }
            }

            // ── Formulario ───────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Campo Nombre
                Column {
                    LabelCampo(texto = "Nombre completo", completado = paso1)
                    Spacer(modifier = Modifier.height(6.dp))
                    BarberiaTextField(
                        value = nombre,
                        onValueChange = { nombre = it; viewModel.clearError() },
                        label = "Tu nombre completo",
                        leadingIcon = Icons.Filled.Person,
                        accentColor = ColorAzul,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )
                }

                // Campo Correo
                Column {
                    LabelCampo(texto = "Correo electrónico", completado = paso2)
                    Spacer(modifier = Modifier.height(6.dp))
                    BarberiaTextField(
                        value = correo,
                        onValueChange = { correo = it; viewModel.clearError() },
                        label = "tu@correo.com",
                        leadingIcon = Icons.Filled.Email,
                        accentColor = ColorAzul,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )
                }

                // Campo Contraseña
                Column {
                    LabelCampo(texto = "Contraseña (mín. 6 caracteres)", completado = paso3)
                    Spacer(modifier = Modifier.height(6.dp))
                    BarberiaTextField(
                        value = password,
                        onValueChange = { password = it; viewModel.clearError() },
                        label = "Contraseña",
                        leadingIcon = Icons.Filled.Lock,
                        accentColor = ColorAzul,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onPasswordToggle = { passwordVisible = !passwordVisible },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    // Indicador visual de fuerza de contraseña
                    AnimatedVisibility(visible = password.isNotBlank()) {
                        val fuerzaColor = when {
                            password.length >= 10 -> Color(0xFF3CB86A)
                            password.length >= 6  -> ColorAzulClaro
                            else                  -> ColorError
                        }
                        val fuerzaTexto = when {
                            password.length >= 10 -> "Contraseña segura"
                            password.length >= 6  -> "Contraseña aceptable"
                            else                  -> "Contraseña débil"
                        }
                        Row(
                            modifier = Modifier.padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(fuerzaColor)
                            )
                            Text(
                                text = fuerzaTexto,
                                color = fuerzaColor,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Campo Confirmar Contraseña
                Column {
                    val coinciden = confirmPassword.isNotBlank() && confirmPassword == password
                    LabelCampo(texto = "Confirmar contraseña", completado = coinciden)
                    Spacer(modifier = Modifier.height(6.dp))
                    BarberiaTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; viewModel.clearError() },
                        label = "Repite tu contraseña",
                        leadingIcon = Icons.Filled.LockOpen,
                        accentColor = if (confirmPassword.isNotBlank() && confirmPassword != password)
                            ColorError else ColorAzul,
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
                                viewModel.register(nombre, correo, password, confirmPassword)
                            }
                        )
                    )

                    // Aviso si las contraseñas no coinciden
                    AnimatedVisibility(
                        visible = confirmPassword.isNotBlank() && confirmPassword != password
                    ) {
                        Text(
                            text = "Las contraseñas no coinciden",
                            color = ColorError,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Mensaje de error del backend
                AnimatedVisibility(
                    visible = uiState.errorMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = ColorError.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = ColorError,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Botón Crear cuenta — azul, activado solo cuando los datos son válidos
                val formListo = nombre.isNotBlank() &&
                        correo.isNotBlank() &&
                        password.length >= 6 &&
                        password == confirmPassword

                BarberiaBoton(
                    texto = "Crear cuenta",
                    icono = Icons.Filled.PersonAdd,
                    isLoading = uiState.isLoading,
                    colorFondo = if (formListo) ColorAzul else ColorBorde,
                    onClick = {
                        if (formListo) {
                            focusManager.clearFocus()
                            viewModel.register(nombre, correo, password, confirmPassword)
                        }
                    }
                )

                Text(
                    text = "Al registrarte aceptas nuestros términos de uso.\nTu cuenta será de tipo CLIENTE.",
                    color = ColorTextoSub,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// Label de campo con check animado cuando el campo está completo
@Composable
private fun LabelCampo(texto: String, completado: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = texto,
            color = ColorTextoSub,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
        AnimatedVisibility(
            visible = completado,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF3CB86A),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}