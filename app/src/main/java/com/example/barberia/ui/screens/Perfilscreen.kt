package com.example.barberia.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.barberia.model.*
import com.example.barberia.network.ApiService
import com.example.barberia.ui.auth.BarberiaBoton
import com.example.barberia.ui.auth.BarberiaTextField
import com.example.barberia.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable

// ── ViewModel del perfil ──────────────────────────────────────────────────────
data class PerfilUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val usuario: UsuarioDTO? = null,
    val citas: List<CitaDTO> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class PerfilViewModel(
    private val apiService: ApiService,
    private val idUsuario: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(PerfilUiState())
    val uiState: StateFlow<PerfilUiState> = _uiState

    init { cargar() }

    fun cargar() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val usuario = apiService.getUsuarioById(idUsuario)
                val citas   = apiService.getCitasByUsuario(idUsuario)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    usuario   = if (usuario.isSuccessful) usuario.body() else null,
                    citas     = if (citas.isSuccessful) citas.body() ?: emptyList()
                    else emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading    = false,
                    errorMessage = "Sin conexión."
                )
            }
        }
    }

    // PUT /api/usuarios/{id} — actualiza nombre y teléfono
    fun actualizarDatos(nombre: String, telefono: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                val actual      = _uiState.value.usuario ?: return@launch
                val actualizado = actual.copy(
                    nombre   = nombre.ifBlank { actual.nombre ?: "" },
                    telefono = telefono.ifBlank { null }
                )
                val response = apiService.updateUsuario(idUsuario, actualizado)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isSaving       = false,
                        usuario        = response.body(),
                        successMessage = "Datos actualizados correctamente"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSaving     = false,
                        errorMessage = when (response.code()) {
                            409  -> "El teléfono ya está en uso"
                            else -> "Error al actualizar (${response.code()})"
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false, errorMessage = "Sin conexión.")
            }
        }
    }

    // PUT /api/usuarios/{id} — actualiza solo la contraseña
    // Envía el usuario completo con la nueva contraseña
    // El backend la encripta con BCrypt antes de guardar
    fun cambiarPassword(passwordNueva: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                val actual      = _uiState.value.usuario ?: return@launch
                val actualizado = actual.copy(password = passwordNueva)
                val response    = apiService.updateUsuario(idUsuario, actualizado)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isSaving       = false,
                        successMessage = "Contraseña actualizada correctamente"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSaving     = false,
                        errorMessage = "Error al cambiar contraseña (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false, errorMessage = "Sin conexión.")
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null, successMessage = null)
    }

    companion object {
        fun factory(apiService: ApiService, idUsuario: Long) =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return PerfilViewModel(apiService, idUsuario) as T
                }
            }
    }
}

// ── Pantalla ──────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    apiService: ApiService,
    idUsuario: Long,
    onVolver: () -> Unit,
    onLogout: () -> Unit
) {
    val colores       = LocalBarberiaColores.current

    val viewModel: PerfilViewModel = viewModel(
        factory = PerfilViewModel.factory(apiService, idUsuario)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Color del acento según el rol del usuario
    val colorRol = when (uiState.usuario?.rol) {
        RolEnum.SUPERADMIN    -> ColorDorado
        RolEnum.ADMINISTRADOR -> ColorAzulClaro
        RolEnum.BARBERO       -> ColorAzul
        RolEnum.CLIENTE       -> ColorRojo
        null                  -> ColorRojo
    }

    val snackbarState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    // Tabs del perfil
    var tabSeleccionada by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = colores.fondo,
        snackbarHost = {
            SnackbarHost(snackbarState) { data ->
                Snackbar(snackbarData = data,
                    containerColor = colores.superficie,
                    contentColor   = colores.texto,
                    actionColor    = colorRol,
                    shape          = RoundedCornerShape(12.dp))
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text("Mi Perfil", color = colores.texto,
                        fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                            tint = colores.texto)
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null,
                            tint = colores.textoSub)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colores.superficie)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)
            .background(colores.fondo)) {

            // ── Tabs: Perfil · Ajustes · Historial ────────────────────────
            TabRow(
                selectedTabIndex = tabSeleccionada,
                containerColor   = colores.superficie,
                contentColor     = colorRol,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.then(
                            with(TabRowDefaults) {
                                Modifier.tabIndicatorOffset(tabPositions[tabSeleccionada])
                            }
                        ),
                        color = colorRol
                    )
                }
            ) {
                listOf(
                    Pair("Perfil",    Icons.Filled.Person),
                    Pair("Ajustes",   Icons.Filled.Settings),
                    Pair("Historial", Icons.Filled.History)
                ).forEachIndexed { index, (label, icon) ->
                    Tab(
                        selected = tabSeleccionada == index,
                        onClick  = { tabSeleccionada = index },
                        text = {
                            Text(label, fontSize = 12.sp,
                                color = if (tabSeleccionada == index)
                                    colorRol else colores.textoSub)
                        },
                        icon = {
                            Icon(icon, null, modifier = Modifier.size(18.dp),
                                tint = if (tabSeleccionada == index)
                                    colorRol else colores.textoSub)
                        }
                    )
                }
            }

            when (tabSeleccionada) {
                0 -> PerfilInfoTab(uiState, colorRol, colores)
                1 -> PerfilAjustesTab(uiState, viewModel, colorRol, colores)
                2 -> PerfilHistorialTab(uiState, colorRol, colores)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 1 — INFO (solo lectura)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun PerfilInfoTab(
    uiState: PerfilUiState,
    colorRol: Color,
    colores: BarberiaColores
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colores.fondo),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Avatar centrado con gradiente de fondo
            Box(modifier = Modifier.fillMaxWidth().background(
                Brush.verticalGradient(
                    listOf(colorRol.copy(0.08f), colores.fondo)
                )
            )) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(90.dp).clip(CircleShape)
                        .background(colorRol.copy(0.15f))
                        .border(2.dp, colorRol, CircleShape),
                        contentAlignment = Alignment.Center) {
                        Text(
                            uiState.usuario?.nombre
                                ?.split(" ")?.take(2)
                                ?.joinToString("") { it.take(1).uppercase() }
                                ?: "?",
                            color = colorRol, fontWeight = FontWeight.Bold,
                            fontSize = 30.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(uiState.usuario?.nombre ?: "Cargando...",
                        color = colores.texto, fontSize = 20.sp,
                        fontWeight = FontWeight.Bold)
                    // Badge del rol
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(colorRol.copy(0.15f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Text(uiState.usuario?.rol?.name ?: "",
                            color = colorRol, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }
        }

        // Datos del perfil
        item {
            Card(modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colores.superficie),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = colores.sombra.dp)) {
                Column(modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    DetalleSeccionTitulo("Información personal", colorRol, colores)
                    PerfilFilaInfo(Icons.Filled.Person,
                        "Nombre", uiState.usuario?.nombre ?: "—",
                        colorRol, colores)
                    HorizontalDivider(color = colores.borde)
                    PerfilFilaInfo(Icons.Filled.Email,
                        "Correo", uiState.usuario?.correo ?: "—",
                        colorRol, colores)
                    HorizontalDivider(color = colores.borde)
                    PerfilFilaInfo(Icons.Filled.Phone,
                        "Teléfono", uiState.usuario?.telefono ?: "No registrado",
                        colorRol, colores)
                    HorizontalDivider(color = colores.borde)
                    PerfilFilaInfo(Icons.Filled.Circle,
                        "Estado",
                        if (uiState.usuario?.activo == true) "Activo" else "Inactivo",
                        if (uiState.usuario?.activo == true) ColorVerde else ColorError,
                        colores)
                }
            }
        }

        // Stats del usuario
        item {
            Card(modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colores.superficie),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = colores.sombra.dp)) {
                Column(modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetalleSeccionTitulo("Mis estadísticas", colorRol, colores)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PerfilStatCard(uiState.citas.size.toString(),
                            "Total citas", colorRol, colores, Modifier.weight(1f))
                        PerfilStatCard(
                            uiState.citas.count {
                                it.estado == EstadoCitaEnum.FINALIZADA }.toString(),
                            "Completadas", ColorVerde, colores, Modifier.weight(1f))
                        PerfilStatCard(
                            uiState.citas.count {
                                it.estado == EstadoCitaEnum.PENDIENTE }.toString(),
                            "Pendientes", ColorDorado, colores, Modifier.weight(1f))
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 2 — AJUSTES (edición + tema)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun PerfilAjustesTab(
    uiState: PerfilUiState,
    viewModel: PerfilViewModel,
    colorRol: Color,
    colores: BarberiaColores
) {
    var nombreEdit        by remember { mutableStateOf("") }
    var telefonoEdit      by remember { mutableStateOf("") }
    var passwordNueva     by remember { mutableStateOf("") }
    var passwordConfirmar by remember { mutableStateOf("") }
    var mostrarPassword   by remember { mutableStateOf(false) }

    // Inicializa con los datos actuales
    LaunchedEffect(uiState.usuario) {
        uiState.usuario?.let {
            nombreEdit   = it.nombre ?: ""
            telefonoEdit = it.telefono ?: ""
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colores.fondo)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // ── Apariencia ────────────────────────────────────────────────────
        item {
            Card(modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colores.superficie),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = colores.sombra.dp)) {
                Column(modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    DetalleSeccionTitulo("Apariencia", colorRol, colores)

                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colorRol.copy(0.1f)),
                                contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (TemaManager.modoOscuro.value) {
                                        null  -> Icons.Filled.BrightnessMedium
                                        true  -> Icons.Filled.DarkMode
                                        false -> Icons.Filled.LightMode
                                    },
                                    contentDescription = null,
                                    tint = colorRol,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text("Modo de pantalla", color = colores.texto,
                                    fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text(
                                    when (TemaManager.modoOscuro.value) {
                                        null  -> "Siguiendo el sistema"
                                        true  -> "Oscuro (forzado)"
                                        false -> "Claro (forzado)"
                                    },
                                    color = colores.textoSub, fontSize = 12.sp
                                )
                            }
                        }
                        // Cicla entre: sistema → oscuro → claro → sistema
                        IconButton(onClick = {
                            TemaManager.modoOscuro.value = when (TemaManager.modoOscuro.value) {
                                null  -> !colores.esModoOscuro
                                true  -> false
                                false -> null
                            }
                        }) {
                            Icon(Icons.Filled.SwapHoriz, null,
                                tint = colorRol, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }

        // ── Datos personales ──────────────────────────────────────────────
        item {
            Card(modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colores.superficie),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = colores.sombra.dp)) {
                Column(modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    DetalleSeccionTitulo("Datos personales", colorRol, colores)

                    BarberiaTextField(nombreEdit, { nombreEdit = it },
                        "Nombre completo", Icons.Filled.Person, colorRol, colores)

                    BarberiaTextField(telefonoEdit, { telefonoEdit = it },
                        "Teléfono", Icons.Filled.Phone, colorRol, colores,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone))

                    // Correo — solo lectura
                    OutlinedTextField(
                        value = uiState.usuario?.correo ?: "",
                        onValueChange = {},
                        label = { Text("Correo (no editable)",
                            color = colores.textoSub, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Filled.Email, null,
                                tint = colores.textoSub,
                                modifier = Modifier.size(20.dp))
                        },
                        enabled  = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor    = colores.borde,
                            disabledTextColor      = colores.textoSub,
                            disabledContainerColor = colores.superficie2
                        )
                    )

                    BarberiaBoton(
                        texto      = "Guardar datos",
                        icono      = Icons.Filled.Save,
                        isLoading  = uiState.isSaving,
                        colorFondo = if (nombreEdit.isNotBlank()) colorRol
                        else colores.borde,
                        onClick    = {
                            if (nombreEdit.isNotBlank())
                                viewModel.actualizarDatos(nombreEdit, telefonoEdit)
                        }
                    )
                }
            }
        }

        // ── Cambiar contraseña ────────────────────────────────────────────
        item {
            Card(modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colores.superficie),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = colores.sombra.dp)) {
                Column(modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    DetalleSeccionTitulo("Cambiar contraseña", colorRol, colores)

                    BarberiaTextField(passwordNueva, { passwordNueva = it },
                        "Nueva contraseña", Icons.Filled.Lock, colorRol, colores,
                        isPassword = true)

                    BarberiaTextField(passwordConfirmar, { passwordConfirmar = it },
                        "Confirmar contraseña", Icons.Filled.Lock, colorRol, colores,
                        isPassword = true)

                    // Validaciones visibles
                    if (passwordNueva.isNotBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ValidacionItem("Mínimo 6 caracteres",
                                passwordNueva.length >= 6, colores)
                            ValidacionItem("Las contraseñas coinciden",
                                passwordNueva == passwordConfirmar &&
                                        passwordConfirmar.isNotBlank(), colores)
                        }
                    }

                    val passwordValida = passwordNueva.length >= 6 &&
                            passwordNueva == passwordConfirmar

                    BarberiaBoton(
                        texto      = "Cambiar contraseña",
                        icono      = Icons.Filled.LockReset,
                        isLoading  = uiState.isSaving,
                        colorFondo = if (passwordValida) colorRol else colores.borde,
                        onClick    = {
                            if (passwordValida) {
                                viewModel.cambiarPassword(passwordNueva)
                                passwordNueva = ""; passwordConfirmar = ""
                            }
                        }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 3 — HISTORIAL DE CITAS
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun PerfilHistorialTab(
    uiState: PerfilUiState,
    colorRol: Color,
    colores: BarberiaColores
) {
    var filtroEstado by remember { mutableStateOf<EstadoCitaEnum?>(null) }

    val citasFiltradas = if (filtroEstado == null) uiState.citas
    else uiState.citas.filter { it.estado == filtroEstado }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colores.fondo)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Historial de citas", color = colores.texto,
                fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("${uiState.citas.size} citas en total",
                color = colores.textoSub, fontSize = 13.sp)
        }

        // Filtros
        item {
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    HistorialFiltroChip("Todas", filtroEstado == null,
                        colorRol, colores) { filtroEstado = null }
                }
                items(EstadoCitaEnum.entries.toTypedArray()) { estado ->
                    HistorialFiltroChip(
                        texto = when (estado) {
                            EstadoCitaEnum.PENDIENTE     -> "Pendiente"
                            EstadoCitaEnum.EN_CURSO      -> "En curso"
                            EstadoCitaEnum.FINALIZADA    -> "Finalizada"
                            EstadoCitaEnum.CANCELADA     -> "Cancelada"
                            EstadoCitaEnum.NO_PRESENTADO -> "No presentó"
                        },
                        seleccionado = filtroEstado == estado,
                        color        = colorRol,
                        colores      = colores,
                        onClick      = { filtroEstado = estado }
                    )
                }
            }
        }

        if (uiState.isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colorRol,
                        modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
                }
            }
        } else if (citasFiltradas.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.EventBusy, null,
                            tint = colores.textoSub, modifier = Modifier.size(44.dp))
                        Text("Sin citas", color = colores.textoSub, fontSize = 14.sp)
                    }
                }
            }
        } else {
            items(citasFiltradas) { cita ->
                TarjetaCitaHistorial(cita, colorRol, colores)
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

// ── Componentes ───────────────────────────────────────────────────────────────

@Composable
private fun PerfilFilaInfo(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, valor: String,
    colorIcono: Color, colores: BarberiaColores
) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
            .background(colorIcono.copy(0.1f)),
            contentAlignment = Alignment.Center) {
            Icon(icono, null, tint = colorIcono, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(label, color = colores.textoSub, fontSize = 11.sp,
                letterSpacing = 0.5.sp)
            Text(valor, color = colores.texto, fontSize = 14.sp,
                fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun PerfilStatCard(
    numero: String, label: String, color: Color,
    colores: BarberiaColores, modifier: Modifier = Modifier
) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f))) {
        Column(modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(numero, color = color, fontSize = 22.sp,
                fontWeight = FontWeight.Bold)
            Text(label, color = colores.textoSub, fontSize = 10.sp,
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ValidacionItem(texto: String, cumple: Boolean, colores: BarberiaColores) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(
            imageVector = if (cumple) Icons.Filled.CheckCircle
            else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint     = if (cumple) ColorVerde else colores.textoSub,
            modifier = Modifier.size(14.dp)
        )
        Text(texto,
            color    = if (cumple) ColorVerde else colores.textoSub,
            fontSize = 12.sp)
    }
}

@Composable
private fun TarjetaCitaHistorial(
    cita: CitaDTO, colorRol: Color, colores: BarberiaColores
) {
    val colorEstado = when (cita.estado) {
        EstadoCitaEnum.PENDIENTE     -> ColorDorado
        EstadoCitaEnum.EN_CURSO      -> ColorAzulClaro
        EstadoCitaEnum.FINALIZADA    -> ColorVerde
        EstadoCitaEnum.CANCELADA     -> ColorError
        EstadoCitaEnum.NO_PRESENTADO -> colores.textoSub
        null                         -> colores.textoSub
    }
    Card(modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colores.superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = colores.sombra.dp)) {
        Row {
            Box(modifier = Modifier.width(4.dp).height(68.dp)
                .background(colorEstado))
            Column(modifier = Modifier.weight(1f)
                .padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Cita #${cita.idCita}", color = colores.texto,
                            fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("${cita.fecha} · ${cita.horaInicio?.take(5)}",
                            color = colores.textoSub, fontSize = 12.sp)
                    }
                    Box(modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorEstado.copy(0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text(
                            text = when (cita.estado) {
                                EstadoCitaEnum.PENDIENTE     -> "Pendiente"
                                EstadoCitaEnum.EN_CURSO      -> "En curso"
                                EstadoCitaEnum.FINALIZADA    -> "Finalizada"
                                EstadoCitaEnum.CANCELADA     -> "Cancelada"
                                EstadoCitaEnum.NO_PRESENTADO -> "No presentó"
                                null -> ""
                            },
                            color = colorEstado, fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistorialFiltroChip(
    texto: String, seleccionado: Boolean, color: Color,
    colores: BarberiaColores, onClick: () -> Unit
) {
    Box(modifier = Modifier
        .clip(RoundedCornerShape(20.dp))
        .background(if (seleccionado) color else colores.superficie2)
        .border(1.dp,
            if (seleccionado) color else colores.borde, RoundedCornerShape(20.dp))
        .clickable { onClick() }
        .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(texto,
            color = if (seleccionado) Color.White else colores.textoSub,
            fontSize = 12.sp,
            fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Normal)
    }
}
