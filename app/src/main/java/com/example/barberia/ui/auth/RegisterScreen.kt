package com.example.barberia.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.example.barberia.ui.theme.*
import com.example.barberia.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    authRepository: AuthRepository,
    onRegistroExitoso: (correo: String) -> Unit,
    onVolver: () -> Unit
) {
    // ── Lee el tema actual ────────────────────────────────────────────────
    val colores = LocalBarberiaColores.current

    val viewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.factory(authRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    var nombre          by remember { mutableStateOf("") }
    var correo          by remember { mutableStateOf("") }
    var telefono        by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.registerSuccess) {
        uiState.registerSuccess?.let { onRegistroExitoso(it.correo) }
    }

    // Indicadores de progreso — se activan conforme se llenan los campos
    val paso1 = nombre.isNotBlank()
    val paso2 = correo.isNotBlank()
    val paso3 = password.length >= 6

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colores.fondo)   // cambia según el modo
    ) {
        // Franja superior azul (diferencia el registro del login que es rojo)
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

            // ── Header ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colores.superficie)
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
                        top = 52.dp, start = 20.dp, end = 20.dp, bottom = 20.dp
                    )
                ) {
                    // Fila superior: botón volver + toggle modo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botón volver con animación spring
                        val interSrc = remember { MutableInteractionSource() }
                        val isPressed by interSrc.collectIsPressedAsState()
                        val escala by animateFloatAsState(
                            targetValue = if (isPressed) 0.92f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "volverScale"
                        )
                        IconButton(
                            onClick = onVolver,
                            interactionSource = interSrc,
                            modifier = Modifier
                                .size(40.dp)
                                .scale(escala)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colores.superficie2)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = colores.texto,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Botón modo claro/oscuro
                        IconButton(onClick = { TemaManager.toggleModo() }) {
                            Icon(
                                imageVector = when (TemaManager.modoOscuro.value) {
                                    null  -> Icons.Filled.BrightnessMedium
                                    true  -> Icons.Filled.DarkMode
                                    false -> Icons.Filled.LightMode
                                },
                                contentDescription = "Modo",
                                tint = colores.textoSub,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Crear cuenta",
                        color = colores.texto,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Completa tus datos para registrarte",
                        color = colores.textoSub,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Barra de 3 pasos — cada segmento se activa al llenar el campo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(paso1, paso2, paso3).forEachIndexed { index, activo ->
                            val colorSeg by animateColorAsState(
                                targetValue = when {
                                    activo   -> ColorAzul
                                    index == 0 -> ColorAzul.copy(alpha = 0.3f)
                                    else     -> colores.borde
                                },
                                animationSpec = tween(300),
                                label = "step$index"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(colorSeg)
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

                // Nombre
                Column {
                    LabelCampo("Nombre completo", paso1)
                    Spacer(modifier = Modifier.height(6.dp))
                    BarberiaTextField(
                        value         = nombre,
                        onValueChange = { nombre = it; viewModel.clearError() },
                        label         = "Tu nombre completo",
                        leadingIcon   = Icons.Filled.Person,
                        accentColor   = ColorAzul,
                        colores       = colores,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )
                }

                // Correo
                Column {
                    LabelCampo("Correo electrónico", paso2)
                    Spacer(modifier = Modifier.height(6.dp))
                    BarberiaTextField(
                        value         = correo,
                        onValueChange = { correo = it; viewModel.clearError() },
                        label         = "tu@correo.com",
                        leadingIcon   = Icons.Filled.Email,
                        accentColor   = ColorAzul,
                        colores       = colores,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction    = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )
                }

                // Contraseña
                Column {
                    LabelCampo("Contraseña (mín. 6 caracteres)", paso3)
                    Spacer(modifier = Modifier.height(6.dp))
                    BarberiaTextField(
                        value           = password,
                        onValueChange   = { password = it; viewModel.clearError() },
                        label           = "Contraseña",
                        leadingIcon     = Icons.Filled.Lock,
                        accentColor     = ColorAzul,
                        colores         = colores,
                        isPassword      = true,
                        passwordVisible = passwordVisible,
                        onPasswordToggle = { passwordVisible = !passwordVisible },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction    = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    // Indicador de fuerza de contraseña
                    AnimatedVisibility(visible = password.isNotBlank()) {
                        val fuerzaColor = when {
                            password.length >= 10 -> ColorVerde
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
                            Text(fuerzaTexto, color = fuerzaColor, fontSize = 11.sp)
                        }
                    }
                }

                // Confirmar contraseña
                Column {
                    val coinciden = confirmPassword.isNotBlank() &&
                            confirmPassword == password
                    LabelCampo("Confirmar contraseña", coinciden)
                    Spacer(modifier = Modifier.height(6.dp))
                    BarberiaTextField(
                        value           = confirmPassword,
                        onValueChange   = { confirmPassword = it; viewModel.clearError() },
                        label           = "Repite tu contraseña",
                        leadingIcon     = Icons.Filled.LockOpen,
                        accentColor     = if (confirmPassword.isNotBlank() &&
                            confirmPassword != password) ColorError else ColorAzul,
                        colores         = colores,
                        isPassword      = true,
                        passwordVisible = passwordVisible,
                        onPasswordToggle = { passwordVisible = !passwordVisible },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction    = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

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

                // Teléfono (opcional)
                Column {
                    LabelCampo("Teléfono (opcional)", telefono.length >= 7)
                    Spacer(modifier = Modifier.height(6.dp))
                    BarberiaTextField(
                        value         = telefono,
                        onValueChange = { telefono = it; viewModel.clearError() },
                        label         = "Ej: 3001234567",
                        leadingIcon   = Icons.Filled.Phone,
                        accentColor   = ColorAzul,
                        colores       = colores,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction    = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        )
                    )
                }

                // Error del backend
                AnimatedVisibility(
                    visible = uiState.errorMessage != null,
                    enter   = fadeIn() + expandVertically(),
                    exit    = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        shape  = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = ColorError.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = ColorError,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val formListo = nombre.isNotBlank() &&
                        correo.isNotBlank() &&
                        password.length >= 6 &&
                        password == confirmPassword

                BarberiaBoton(
                    texto      = "Crear cuenta",
                    icono      = Icons.Filled.PersonAdd,
                    isLoading  = uiState.isLoading,
                    colorFondo = if (formListo) ColorAzul else colores.borde,
                    onClick    = {
                        if (formListo) {
                            focusManager.clearFocus()
                            viewModel.register(
                                nombre, correo, password, confirmPassword,
                                telefono.ifBlank { null }
                            )
                        }
                    }
                )

                Text(
                    text = "Al registrarte aceptas nuestros términos de uso.\nTu cuenta será de tipo CLIENTE.",
                    color = colores.textoSub,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// Label de campo con check verde animado cuando está completo
@Composable
private fun LabelCampo(texto: String, completado: Boolean) {
    val colores = LocalBarberiaColores.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = texto,
            color = colores.textoSub,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
        AnimatedVisibility(
            visible = completado,
            enter   = fadeIn() + scaleIn(),
            exit    = fadeOut() + scaleOut()
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = ColorVerde,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}