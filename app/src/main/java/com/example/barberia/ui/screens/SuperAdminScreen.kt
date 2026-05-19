package com.example.barberia.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.barberia.model.*
import com.example.barberia.network.ApiService
import com.example.barberia.ui.auth.BarberiaBoton
import com.example.barberia.ui.auth.BarberiaTextField
import com.example.barberia.ui.theme.*
import com.example.barberia.viewmodel.SuperAdminViewModel
import kotlinx.coroutines.launch

// Acento del SuperAdmin — dorado exclusivo
private val SuperAccent     = ColorDorado
private val SuperAccentSoft = ColorDorado.copy(alpha = 0.15f)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SuperAdminScreen(
    apiService: ApiService,
    nombre: String,
    onLogout: () -> Unit
) {
    val colores       = LocalBarberiaColores.current
    val sistemaOscuro = isSystemInDarkTheme()

    val viewModel: SuperAdminViewModel = viewModel(
        factory = SuperAdminViewModel.factory(apiService)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 5 tabs: Inicio, Usuarios, Barberos, Servicios, Citas
    val pagerState    = rememberPagerState(pageCount = { 5 })
    val scope         = rememberCoroutineScope()
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

    Scaffold(
        containerColor = colores.fondo,
        snackbarHost = {
            SnackbarHost(snackbarState) { data ->
                Snackbar(snackbarData = data,
                    containerColor = colores.superficie,
                    contentColor   = colores.texto,
                    actionColor    = SuperAccent,
                    shape          = RoundedCornerShape(12.dp))
            }
        },
        bottomBar = {
            SuperAdminBottomBar(pagerState.currentPage, colores) { index ->
                scope.launch { pagerState.animateScrollToPage(index) }
            }
        }
    ) { padding ->
        HorizontalPager(
            state             = pagerState,
            modifier          = Modifier.padding(padding).fillMaxSize(),
            userScrollEnabled = true
        ) { pagina ->
            when (pagina) {
                0 -> SuperInicioTab(nombre, uiState, viewModel,
                    onLogout, colores, sistemaOscuro)
                1 -> SuperUsuariosTab(uiState, viewModel, colores)
                2 -> SuperBarberosTab(uiState, viewModel, colores)
                3 -> SuperServiciosTab(uiState, viewModel, colores)
                4 -> SuperCitasTab(uiState, viewModel, colores)
            }
        }
    }
}

// ── Bottom Bar — 5 tabs, acento dorado ───────────────────────────────────────
@Composable
private fun SuperAdminBottomBar(
    paginaActual: Int,
    colores: BarberiaColores,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = colores.superficie,
        tonalElevation = 0.dp,
        modifier = if (!colores.esModoOscuro) Modifier.shadow(4.dp)
        else Modifier.border(1.dp, colores.borde,
            RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp))
    ) {
        listOf(
            Triple("Inicio",    Icons.Filled.Home,          0),
            Triple("Usuarios",  Icons.Filled.ManageAccounts,1),
            Triple("Barberos",  Icons.Filled.People,        2),
            Triple("Servicios", Icons.Filled.ContentCut,    3),
            Triple("Citas",     Icons.Filled.CalendarMonth, 4)
        ).forEach { (label, icon, index) ->
            val activo = paginaActual == index
            NavigationBarItem(
                selected = activo,
                onClick  = { onTabSelected(index) },
                icon = { Icon(icon, label,
                    tint = if (activo) SuperAccent else colores.textoSub,
                    modifier = Modifier.size(22.dp)) },
                label = { Text(label,
                    color = if (activo) SuperAccent else colores.textoSub,
                    fontSize = 10.sp,
                    fontWeight = if (activo) FontWeight.SemiBold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = SuperAccentSoft)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 1 — INICIO (Control total)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun SuperInicioTab(
    nombre: String,
    uiState: com.example.barberia.viewmodel.SuperAdminUiState,
    viewModel: SuperAdminViewModel,
    onLogout: () -> Unit,
    colores: BarberiaColores,
    sistemaOscuro: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colores.fondo)
    ) {
        // ── Header dorado ─────────────────────────────────────────────────
        item {
            Box(modifier = Modifier.fillMaxWidth().background(
                Brush.verticalGradient(
                    if (colores.esModoOscuro)
                        listOf(Color(0xFF120E02), colores.fondo)
                    else
                        listOf(Color(0xFFFFFAF0), colores.fondo)
                )
            )) {
                // Franja polo dorada para SuperAdmin
                Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(
                    Brush.horizontalGradient(
                        listOf(SuperAccent, ColorBlanco, SuperAccent,
                            ColorBlanco, SuperAccent)
                    )))

                Column(modifier = Modifier.padding(
                    start = 20.dp, end = 20.dp, top = 52.dp, bottom = 20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.AdminPanelSettings, null,
                                    tint = SuperAccent,
                                    modifier = Modifier.size(14.dp))
                                Text("SUPERADMIN", color = SuperAccent,
                                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Control total", color = colores.texto,
                                fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("$nombre · Acceso completo",
                                color = colores.textoSub, fontSize = 13.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Avatar corona dorada
                            Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                                .background(SuperAccentSoft)
                                .border(1.5.dp, SuperAccent, CircleShape),
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.AdminPanelSettings, null,
                                    tint = SuperAccent,
                                    modifier = Modifier.size(22.dp))
                            }
                            IconButton(onClick = {
                                TemaManager.modoOscuro.value = when (TemaManager.modoOscuro.value) {
                                    null  -> !colores.esModoOscuro
                                    true  -> false
                                    false -> null
                                }
                            }) {
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
                            IconButton(onClick = onLogout) {
                                Icon(Icons.AutoMirrored.Filled.Logout, null,
                                    tint = colores.textoSub,
                                    modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }

        if (uiState.isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SuperAccent,
                        strokeWidth = 2.5.dp, modifier = Modifier.size(36.dp))
                }
            }
        } else {
            // Stats 2x2
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SuperStatCard(uiState.usuarios.size.toString(),
                            "Usuarios", SuperAccent, colores, Modifier.weight(1f))
                        SuperStatCard(uiState.todasLasCitas.size.toString(),
                            "Citas totales", ColorAzulClaro, colores, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SuperStatCard(uiState.barberos.size.toString(),
                            "Barberos", ColorVerde, colores, Modifier.weight(1f))
                        SuperStatCard(uiState.servicios.size.toString(),
                            "Servicios", ColorRojo, colores, Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Gestión del sistema
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SuperSeccionTitulo("Gestión del sistema", colores)
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SuperAccionCard(Icons.Filled.ManageAccounts,
                            "Usuarios", "Crear · editar · eliminar · roles",
                            SuperAccent, colores)
                        SuperAccionCard(Icons.Filled.People,
                            "Barberos", "${uiState.barberos.size} registrados · " +
                                    "${uiState.barberos.count { it.activo == true }} activos",
                            ColorVerde, colores)
                        SuperAccionCard(Icons.Filled.ContentCut,
                            "Servicios", "${uiState.servicios.size} servicios activos",
                            ColorAzulClaro, colores)
                        SuperAccionCard(Icons.Filled.Shield,
                            "Roles disponibles",
                            "SUPERADMIN · ADMINISTRADOR · BARBERO · CLIENTE",
                            ColorRojo, colores)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 2 — USUARIOS (exclusivo SuperAdmin)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun SuperUsuariosTab(
    uiState: com.example.barberia.viewmodel.SuperAdminUiState,
    viewModel: SuperAdminViewModel,
    colores: BarberiaColores
) {
    var mostrarFormulario by remember { mutableStateOf(false) }
    var nombreUsuario     by remember { mutableStateOf("") }
    var correoUsuario     by remember { mutableStateOf("") }
    var passwordUsuario   by remember { mutableStateOf("") }
    var rolSeleccionado   by remember { mutableStateOf(RolEnum.CLIENTE) }
    var usuarioAEliminar  by remember { mutableStateOf<UsuarioDTO?>(null) }
    var filtroRol         by remember { mutableStateOf<RolEnum?>(null) }

    val usuariosFiltrados = if (filtroRol == null) uiState.usuarios
    else uiState.usuarios.filter { it.rol == filtroRol }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colores.fondo)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Usuarios", color = colores.texto,
                        fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("${uiState.usuarios.size} usuarios registrados",
                        color = colores.textoSub, fontSize = 13.sp)
                }
                FloatingActionButton(
                    onClick = { mostrarFormulario = !mostrarFormulario },
                    containerColor = SuperAccent,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(if (mostrarFormulario) Icons.Filled.Close else Icons.Filled.Add,
                        null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Formulario nuevo usuario
        item {
            AnimatedVisibility(mostrarFormulario,
                enter = expandVertically() + fadeIn(),
                exit  = shrinkVertically() + fadeOut()) {
                Card(modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colores.superficie),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = colores.sombra.dp)) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(
                            Brush.horizontalGradient(
                                listOf(SuperAccent, SuperAccent.copy(0.3f)))))
                        Column(modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Nuevo usuario", color = colores.texto,
                                fontWeight = FontWeight.Bold, fontSize = 15.sp)

                            BarberiaTextField(nombreUsuario,
                                { nombreUsuario = it }, "Nombre *",
                                Icons.Filled.Person, SuperAccent, colores)
                            BarberiaTextField(correoUsuario,
                                { correoUsuario = it }, "Correo *",
                                Icons.Filled.Email, SuperAccent, colores,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email))
                            BarberiaTextField(passwordUsuario,
                                { passwordUsuario = it }, "Contraseña *",
                                Icons.Filled.Lock, SuperAccent, colores,
                                isPassword = true)

                            // Selector de rol
                            Text("Rol", color = colores.textoSub,
                                fontSize = 12.sp, letterSpacing = 0.5.sp)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(RolEnum.entries.toTypedArray()) { rol ->
                                    val seleccionado = rolSeleccionado == rol
                                    Box(modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (seleccionado) SuperAccent
                                            else colores.superficie2)
                                        .border(1.dp,
                                            if (seleccionado) SuperAccent else colores.borde,
                                            RoundedCornerShape(20.dp))
                                        .clickable { rolSeleccionado = rol }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(rol.name,
                                            color = if (seleccionado) Color.White
                                            else colores.textoSub,
                                            fontSize = 12.sp,
                                            fontWeight = if (seleccionado)
                                                FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }

                            val formListo = nombreUsuario.isNotBlank() &&
                                    correoUsuario.isNotBlank() &&
                                    passwordUsuario.length >= 6

                            BarberiaBoton(
                                texto      = "Crear usuario",
                                icono      = Icons.Filled.PersonAdd,
                                isLoading  = uiState.isLoading,
                                colorFondo = if (formListo) SuperAccent else colores.borde,
                                onClick    = {
                                    if (formListo) {
                                        viewModel.crearUsuario(
                                            nombreUsuario, correoUsuario,
                                            passwordUsuario, rolSeleccionado)
                                        nombreUsuario = ""; correoUsuario = ""
                                        passwordUsuario = ""
                                        rolSeleccionado = RolEnum.CLIENTE
                                        mostrarFormulario = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Filtros por rol
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    SuperFiltroChip("Todos", filtroRol == null, colores) {
                        filtroRol = null }
                }
                items(RolEnum.entries.toTypedArray()) { rol ->
                    SuperFiltroChip(rol.name, filtroRol == rol, colores) {
                        filtroRol = rol }
                }
            }
        }

        // Lista de usuarios
        if (usuariosFiltrados.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center) {
                    Text("Sin usuarios", color = colores.textoSub, fontSize = 14.sp)
                }
            }
        } else {
            items(usuariosFiltrados) { usuario ->
                TarjetaUsuarioSuper(
                    usuario    = usuario,
                    colores    = colores,
                    onToggle   = { viewModel.toggleActivoUsuario(usuario) },
                    onEliminar = { usuarioAEliminar = usuario }
                )
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    // Dialog eliminar usuario
    usuarioAEliminar?.let { usuario ->
        AlertDialog(
            onDismissRequest = { usuarioAEliminar = null },
            containerColor   = colores.superficie,
            shape            = RoundedCornerShape(20.dp),
            title = { Text("Eliminar usuario", color = colores.texto,
                fontWeight = FontWeight.Bold) },
            text  = { Text(
                "¿Eliminar a ${usuario.nombre}? Esta acción no se puede deshacer.",
                color = colores.textoSub, fontSize = 14.sp) },
            confirmButton = {
                BarberiaBoton("Eliminar", colorFondo = ColorError, onClick = {
                    viewModel.eliminarUsuario(usuario.idUsuario!!)
                    usuarioAEliminar = null
                })
            },
            dismissButton = {
                TextButton(onClick = { usuarioAEliminar = null }) {
                    Text("Cancelar", color = colores.textoSub) }
            }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 3 — BARBEROS
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun SuperBarberosTab(
    uiState: com.example.barberia.viewmodel.SuperAdminUiState,
    viewModel: SuperAdminViewModel,
    colores: BarberiaColores
) {
    var mostrarFormulario  by remember { mutableStateOf(false) }
    var nombreBarbero      by remember { mutableStateOf("") }
    var especialidad       by remember { mutableStateOf("") }
    var telefono           by remember { mutableStateOf("") }
    var barberoAEliminar   by remember { mutableStateOf<BarberoDTO?>(null) }
    var usuarioVinculado   by remember { mutableStateOf<Long?>(null) }
    val usuariosBarbero = uiState.usuarios.filter {
        it.rol == RolEnum.BARBERO && it.activo == true
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colores.fondo)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Barberos", color = colores.texto,
                        fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("${uiState.barberos.size} registrados",
                        color = colores.textoSub, fontSize = 13.sp)
                }
                FloatingActionButton(onClick = { mostrarFormulario = !mostrarFormulario },
                    containerColor = SuperAccent, modifier = Modifier.size(44.dp)) {
                    Icon(if (mostrarFormulario) Icons.Filled.Close else Icons.Filled.Add,
                        null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        item {
            AnimatedVisibility(mostrarFormulario,
                enter = expandVertically() + fadeIn(),
                exit  = shrinkVertically() + fadeOut()) {
                Card(modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colores.superficie),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = colores.sombra.dp)) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(
                            Brush.horizontalGradient(
                                listOf(SuperAccent, ColorVerde))))
                        Column(modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Nuevo barbero", color = colores.texto,
                                fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            BarberiaTextField(nombreBarbero,
                                { nombreBarbero = it }, "Nombre *",
                                Icons.Filled.Person, SuperAccent, colores)
                            BarberiaTextField(especialidad,
                                { especialidad = it }, "Especialidad",
                                Icons.Filled.ContentCut, SuperAccent, colores)
                            BarberiaTextField(telefono,
                                { telefono = it }, "Teléfono",
                                Icons.Filled.Phone, SuperAccent, colores,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone))

                            if (usuariosBarbero.isNotEmpty()) {
                                Text("Vincular con usuario BARBERO",
                                    color = colores.textoSub, fontSize = 12.sp)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    item {
                                        Box(modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (usuarioVinculado == null)
                                                SuperAccent else colores.superficie2)
                                            .border(1.dp, if (usuarioVinculado == null)
                                                SuperAccent else colores.borde,
                                                RoundedCornerShape(8.dp))
                                            .clickable { usuarioVinculado = null }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)) {
                                            Text("Sin vínculo",
                                                color = if (usuarioVinculado == null)
                                                    Color.White else colores.texto,
                                                fontSize = 11.sp)
                                        }
                                    }
                                    items(usuariosBarbero) { user ->
                                        val sel = usuarioVinculado == user.idUsuario
                                        Box(modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (sel) SuperAccent else colores.superficie2)
                                            .border(1.dp, if (sel) SuperAccent else colores.borde,
                                                RoundedCornerShape(8.dp))
                                            .clickable { usuarioVinculado = user.idUsuario }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)) {
                                            Text(user.nombre?.take(15) ?: "User #${user.idUsuario}",
                                                color = if (sel) Color.White else colores.texto,
                                                fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            BarberiaBoton(
                                texto      = "Crear barbero",
                                icono      = Icons.Filled.PersonAdd,
                                isLoading  = uiState.isLoading,
                                colorFondo = if (nombreBarbero.isNotBlank())
                                    SuperAccent else colores.borde,
                                onClick    = {
                                    if (nombreBarbero.isNotBlank()) {
                                        viewModel.crearBarbero(
                                            nombreBarbero, especialidad, telefono,
                                            usuarioVinculado)
                                        nombreBarbero = ""; especialidad = ""
                                        telefono = ""; usuarioVinculado = null
                                        mostrarFormulario = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        items(uiState.barberos) { barbero ->
            TarjetaBarberoSuper(barbero, colores,
                onToggle   = { viewModel.toggleActivoBarbero(barbero) },
                onEliminar = { barberoAEliminar = barbero })
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    barberoAEliminar?.let { barbero ->
        AlertDialog(
            onDismissRequest = { barberoAEliminar = null },
            containerColor   = colores.superficie,
            shape            = RoundedCornerShape(20.dp),
            title = { Text("Eliminar barbero", color = colores.texto,
                fontWeight = FontWeight.Bold) },
            text  = { Text("¿Eliminar a ${barbero.nombre}?",
                color = colores.textoSub, fontSize = 14.sp) },
            confirmButton = {
                BarberiaBoton("Eliminar", colorFondo = ColorError, onClick = {
                    viewModel.eliminarBarbero(barbero.idBarbero!!)
                    barberoAEliminar = null
                })
            },
            dismissButton = {
                TextButton(onClick = { barberoAEliminar = null }) {
                    Text("Cancelar", color = colores.textoSub) }
            }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 4 — SERVICIOS
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun SuperServiciosTab(
    uiState: com.example.barberia.viewmodel.SuperAdminUiState,
    viewModel: SuperAdminViewModel,
    colores: BarberiaColores
) {
    var mostrarFormulario by remember { mutableStateOf(false) }
    var nombreServicio    by remember { mutableStateOf("") }
    var descripcion       by remember { mutableStateOf("") }
    var precio            by remember { mutableStateOf("") }
    var duracion          by remember { mutableStateOf("") }
    var servicioAEliminar by remember { mutableStateOf<ServicioDTO?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colores.fondo)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Servicios", color = colores.texto,
                        fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("${uiState.servicios.size} servicios",
                        color = colores.textoSub, fontSize = 13.sp)
                }
                FloatingActionButton(onClick = { mostrarFormulario = !mostrarFormulario },
                    containerColor = SuperAccent, modifier = Modifier.size(44.dp)) {
                    Icon(if (mostrarFormulario) Icons.Filled.Close else Icons.Filled.Add,
                        null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        item {
            AnimatedVisibility(mostrarFormulario,
                enter = expandVertically() + fadeIn(),
                exit  = shrinkVertically() + fadeOut()) {
                Card(modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colores.superficie),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = colores.sombra.dp)) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(
                            Brush.horizontalGradient(listOf(SuperAccent, ColorRojo))))
                        Column(modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Nuevo servicio", color = colores.texto,
                                fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            BarberiaTextField(nombreServicio,
                                { nombreServicio = it }, "Nombre *",
                                Icons.Filled.ContentCut, SuperAccent, colores)
                            BarberiaTextField(descripcion,
                                { descripcion = it }, "Descripción",
                                Icons.Filled.Description, SuperAccent, colores)
                            BarberiaTextField(precio,
                                { precio = it }, "Precio",
                                Icons.Filled.AttachMoney, SuperAccent, colores,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number))
                            BarberiaTextField(duracion,
                                { duracion = it }, "Duración (minutos)",
                                Icons.Filled.Schedule, SuperAccent, colores,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number))
                            val formListo = nombreServicio.isNotBlank() &&
                                    precio.toDoubleOrNull() != null &&
                                    duracion.toIntOrNull() != null
                            BarberiaBoton(
                                texto      = "Crear servicio",
                                icono      = Icons.Filled.Add,
                                isLoading  = uiState.isLoading,
                                colorFondo = if (formListo) SuperAccent else colores.borde,
                                onClick    = {
                                    if (formListo) {
                                        viewModel.crearServicio(
                                            nombreServicio, descripcion,
                                            precio.toDouble(), duracion.toInt())
                                        nombreServicio = ""; descripcion = ""
                                        precio = ""; duracion = ""
                                        mostrarFormulario = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        items(uiState.servicios) { servicio ->
            TarjetaServicioSuper(servicio, colores) {
                servicioAEliminar = servicio }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    servicioAEliminar?.let { servicio ->
        AlertDialog(
            onDismissRequest = { servicioAEliminar = null },
            containerColor   = colores.superficie,
            shape            = RoundedCornerShape(20.dp),
            title = { Text("Eliminar servicio", color = colores.texto,
                fontWeight = FontWeight.Bold) },
            text  = { Text("¿Eliminar '${servicio.nombre}'?",
                color = colores.textoSub, fontSize = 14.sp) },
            confirmButton = {
                BarberiaBoton("Eliminar", colorFondo = ColorError, onClick = {
                    viewModel.eliminarServicio(servicio.idServicio!!)
                    servicioAEliminar = null
                })
            },
            dismissButton = {
                TextButton(onClick = { servicioAEliminar = null }) {
                    Text("Cancelar", color = colores.textoSub) }
            }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 5 — CITAS (todas)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun SuperCitasTab(
    uiState: com.example.barberia.viewmodel.SuperAdminUiState,
    viewModel: SuperAdminViewModel,
    colores: BarberiaColores
) {
    var citaACancelar by remember { mutableStateOf<CitaDTO?>(null) }
    var filtroEstado  by remember { mutableStateOf<EstadoCitaEnum?>(null) }

    val citasFiltradas = if (filtroEstado == null) uiState.todasLasCitas
    else uiState.todasLasCitas.filter { it.estado == filtroEstado }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colores.fondo)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Todas las citas", color = colores.texto,
                fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("${uiState.todasLasCitas.size} citas · ${uiState.citasHoy.size} hoy",
                color = colores.textoSub, fontSize = 13.sp)
        }

        // Filtros
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    SuperFiltroChip("Todas", filtroEstado == null, colores) {
                        filtroEstado = null }
                }
                items(EstadoCitaEnum.entries.toTypedArray()) { estado ->
                    SuperFiltroChip(
                        texto = when (estado) {
                            EstadoCitaEnum.PENDIENTE     -> "Pendiente"
                            EstadoCitaEnum.EN_CURSO      -> "En curso"
                            EstadoCitaEnum.FINALIZADA    -> "Finalizada"
                            EstadoCitaEnum.CANCELADA     -> "Cancelada"
                            EstadoCitaEnum.NO_PRESENTADO -> "No presentó"
                        },
                        seleccionado = filtroEstado == estado,
                        colores      = colores,
                        onClick      = { filtroEstado = estado }
                    )
                }
            }
        }

        if (uiState.isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SuperAccent,
                        modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
                }
            }
        } else {
            items(citasFiltradas) { cita ->
                TarjetaCitaSuper(cita, colores) {
                    if (cita.estado == EstadoCitaEnum.PENDIENTE ||
                        cita.estado == EstadoCitaEnum.EN_CURSO)
                        citaACancelar = cita
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    citaACancelar?.let { cita ->
        AlertDialog(
            onDismissRequest = { citaACancelar = null },
            containerColor   = colores.superficie,
            shape            = RoundedCornerShape(20.dp),
            title = { Text("Cancelar cita", color = colores.texto,
                fontWeight = FontWeight.Bold) },
            text  = { Text("¿Cancelar la cita #${cita.idCita}?",
                color = colores.textoSub, fontSize = 14.sp) },
            confirmButton = {
                BarberiaBoton("Sí, cancelar", colorFondo = ColorError, onClick = {
                    viewModel.cancelarCita(cita.idCita!!)
                    citaACancelar = null
                })
            },
            dismissButton = {
                TextButton(onClick = { citaACancelar = null }) {
                    Text("No", color = colores.textoSub) }
            }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// COMPONENTES
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SuperStatCard(
    numero: String, label: String, color: Color,
    colores: BarberiaColores, modifier: Modifier = Modifier
) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colores.superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = colores.sombra.dp)) {
        Column(modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(numero, color = color, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(label, color = colores.textoSub, fontSize = 11.sp,
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SuperSeccionTitulo(texto: String, colores: BarberiaColores) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.width(3.dp).height(18.dp)
            .clip(RoundedCornerShape(2.dp)).background(SuperAccent))
        Text(texto, color = colores.texto, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SuperAccionCard(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String, subtitulo: String, color: Color, colores: BarberiaColores
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colores.superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = colores.sombra.dp)) {
        Row(modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center) {
                Icon(icono, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, color = colores.texto,
                    fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(subtitulo, color = colores.textoSub, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Filled.ChevronRight, null,
                tint = colores.textoSub, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun TarjetaUsuarioSuper(
    usuario: UsuarioDTO, colores: BarberiaColores,
    onToggle: () -> Unit, onEliminar: () -> Unit
) {
    val colorRol = when (usuario.rol) {
        RolEnum.SUPERADMIN    -> SuperAccent
        RolEnum.ADMINISTRADOR -> ColorAzulClaro
        RolEnum.BARBERO       -> ColorAzul
        RolEnum.CLIENTE       -> ColorRojo
        null                  -> colores.textoSub
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colores.superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = colores.sombra.dp)) {
        Row(modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Avatar con color del rol
            Box(modifier = Modifier.size(42.dp).clip(CircleShape)
                .background(colorRol.copy(0.15f)),
                contentAlignment = Alignment.Center) {
                Text(usuario.nombre?.take(2)?.uppercase() ?: "??",
                    color = colorRol, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(usuario.nombre ?: "Sin nombre", color = colores.texto,
                        fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    // Badge del rol
                    Box(modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colorRol.copy(0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(usuario.rol?.name ?: "", color = colorRol,
                            fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(usuario.correo ?: "", color = colores.textoSub,
                    fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Switch(checked = usuario.activo ?: false,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor   = Color.White,
                    checkedTrackColor   = SuperAccent,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = colores.borde))
            IconButton(onClick = onEliminar, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.DeleteOutline, null,
                    tint = ColorError, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun TarjetaBarberoSuper(
    barbero: BarberoDTO, colores: BarberiaColores,
    onToggle: () -> Unit, onEliminar: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colores.superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = colores.sombra.dp)) {
        Row(modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(42.dp).clip(CircleShape)
                .background(if (barbero.activo == true)
                    SuperAccent.copy(0.15f) else colores.borde),
                contentAlignment = Alignment.Center) {
                Text(barbero.nombre.take(2).uppercase(),
                    color = if (barbero.activo == true) SuperAccent else colores.textoSub,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(barbero.nombre, color = colores.texto,
                    fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(barbero.especialidad ?: "Sin especialidad",
                    color = colores.textoSub, fontSize = 12.sp)
            }
            Switch(checked = barbero.activo ?: false,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor   = Color.White,
                    checkedTrackColor   = SuperAccent,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = colores.borde))
            IconButton(onClick = onEliminar, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.DeleteOutline, null,
                    tint = ColorError, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun TarjetaServicioSuper(
    servicio: ServicioDTO, colores: BarberiaColores, onEliminar: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colores.superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = colores.sombra.dp)) {
        Row(modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                .background(SuperAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.ContentCut, null,
                    tint = SuperAccent, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(servicio.nombre, color = colores.texto,
                    fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text("${servicio.duracionMinutos} min",
                    color = colores.textoSub, fontSize = 12.sp)
            }
            Text("\$${servicio.precio.toInt()}", color = SuperAccent,
                fontWeight = FontWeight.Bold, fontSize = 16.sp)
            IconButton(onClick = onEliminar, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.DeleteOutline, null,
                    tint = ColorError, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun TarjetaCitaSuper(
    cita: CitaDTO, colores: BarberiaColores, onCancelar: () -> Unit
) {
    val colorEstado = when (cita.estado) {
        EstadoCitaEnum.PENDIENTE     -> ColorDorado
        EstadoCitaEnum.EN_CURSO      -> ColorAzulClaro
        EstadoCitaEnum.FINALIZADA    -> ColorVerde
        EstadoCitaEnum.CANCELADA     -> ColorError
        EstadoCitaEnum.NO_PRESENTADO -> colores.textoSub
        null                         -> colores.textoSub
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colores.superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = colores.sombra.dp)) {
        Row {
            Box(modifier = Modifier.width(4.dp).height(72.dp).background(colorEstado))
            Column(modifier = Modifier.weight(1f)
                .padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Cita #${cita.idCita}", color = colores.texto,
                            fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${cita.fecha} · ${cita.horaInicio?.take(5)}",
                            color = colores.textoSub, fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(colorEstado.copy(alpha = 0.15f))
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
                        if (cita.estado == EstadoCitaEnum.PENDIENTE ||
                            cita.estado == EstadoCitaEnum.EN_CURSO) {
                            IconButton(onClick = onCancelar,
                                modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Cancel, null,
                                    tint = ColorError, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuperFiltroChip(
    texto: String, seleccionado: Boolean,
    colores: BarberiaColores, onClick: () -> Unit
) {
    Box(modifier = Modifier
        .clip(RoundedCornerShape(20.dp))
        .background(if (seleccionado) SuperAccent else colores.superficie2)
        .border(1.dp,
            if (seleccionado) SuperAccent else colores.borde, RoundedCornerShape(20.dp))
        .clickable { onClick() }
        .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(texto,
            color = if (seleccionado) Color.White else colores.textoSub,
            fontSize = 12.sp,
            fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Normal)
    }
}