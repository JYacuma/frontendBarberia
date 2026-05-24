package com.example.barberia.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import java.text.Normalizer

private val SuperAccent     = ColorDorado
private val SuperAccentSoft = ColorDorado.copy(alpha = 0.15f)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SuperAdminScreen(
    apiService: ApiService,
    nombre: String,
    idUsuario: Long = 0L,
    onLogout: () -> Unit,
    onNavigateToNotificaciones: () -> Unit = {},
    onNavigateToPerfil: (Long) -> Unit = {}
) {
    val viewModel: SuperAdminViewModel = viewModel(
        factory = SuperAdminViewModel.factory(apiService)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pagerState    = rememberPagerState(pageCount = { 5 })
    val scope         = rememberCoroutineScope()
    val snackbarState = remember { SnackbarHostState() }
    val colores = LocalBarberiaColores.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

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

    val initials = remember(nombre) {
        val ascii = java.text.Normalizer.normalize(nombre, java.text.Normalizer.Form.NFD)
            .replace(Regex("[^\\p{ASCII}]"), "")
        ascii.split(" ").take(2).joinToString("") { it.first().uppercase() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = colores.superficie) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(64.dp).clip(CircleShape)
                            .background(SuperAccent),
                            contentAlignment = Alignment.Center) {
                            Text(initials, color = Color.White,
                                fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(nombre, fontWeight = FontWeight.Bold, color = colores.texto, fontSize = 16.sp)
                    }
                }
                HorizontalDivider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Person, null, tint = SuperAccent) },
                    label = { Text("Perfil") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onNavigateToPerfil(idUsuario) }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = ColorError) },
                    label = { Text("Cerrar sesión") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onLogout() }
                )
                HorizontalDivider()
                Text("Tema:", modifier = Modifier.padding(16.dp, 8.dp),
                    color = colores.textoSub, fontSize = 12.sp)
                listOf("Claro" to false, "Sistema" to null, "Oscuro" to true).forEach { (label, mode) ->
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = when (mode) {
                                    null -> Icons.Filled.BrightnessMedium
                                    true -> Icons.Filled.DarkMode
                                    else -> Icons.Filled.LightMode
                                },
                                contentDescription = null,
                                tint = if (TemaManager.modoOscuro.value == mode) SuperAccent
                                else colores.textoSub
                            )
                        },
                        label = { Text(label) },
                        selected = TemaManager.modoOscuro.value == mode,
                        onClick = {
                            TemaManager.modoOscuro.value = mode
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        }
    ) {
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
                             onLogout, colores,
                             onOpenDrawer = { scope.launch { drawerState.open() } },
                             onNavigateToTab = { scope.launch { pagerState.animateScrollToPage(it) } },
                             onNavigateToNotificaciones = onNavigateToNotificaciones)
                        1 -> SuperUsuariosTab(uiState, viewModel, colores)
                        2 -> SuperBarberosTab(uiState, viewModel, colores)
                        3 -> SuperServiciosTab(uiState, viewModel, colores)
                        4 -> SuperCitasTab(uiState, viewModel, colores)
                    }
                }
        }
    }
}

// ── Bottom Bar — 5 tabs, acento dorado ───────────────────────────────────────
@Composable
fun SuperAdminBottomBar(
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
// TAB 0 — INICIO
// ══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperInicioTab(
    nombre: String,
    uiState: com.example.barberia.viewmodel.SuperAdminUiState,
    viewModel: SuperAdminViewModel,
    onLogout: () -> Unit,
    colores: BarberiaColores,
    onOpenDrawer: () -> Unit = {},
    onNavigateToTab: (Int) -> Unit,
    onNavigateToNotificaciones: () -> Unit = {}
) {
    LazyColumn {
        item {
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(
                        Brush.horizontalGradient(
                            listOf(SuperAccent, ColorBlanco, SuperAccent, ColorBlanco, SuperAccent)
                        ))) {}
                    Column(modifier = Modifier.padding(
                        start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                                    .background(SuperAccent.copy(0.15f))
                                    .border(1.5.dp, SuperAccent, CircleShape)
                                    .clickable { onOpenDrawer() },
                                    contentAlignment = Alignment.Center) {
                                    Text(getInitials(nombre), color = SuperAccent,
                                        fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Text(nombre, color = colores.texto,
                                    fontSize = 20.sp, fontWeight = FontWeight.Bold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = onNavigateToNotificaciones) {
                                BadgedBox(badge = {
                                    if (uiState.notificacionesCount > 0) {
                                        Badge(containerColor = ColorRojo) {
                                            Text(if (uiState.notificacionesCount > 99) "99+"
                                                else uiState.notificacionesCount.toString(),
                                                color = Color.White, fontSize = 10.sp)
                                        }
                                    }
                                }) {
                                    Icon(Icons.Filled.Notifications, null,
                                        tint = colores.textoSub, modifier = Modifier.size(22.dp))
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
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SuperStatCard(
                                numero = uiState.usuarios.size.toString(),
                                label  = "Usuarios",
                                color  = SuperAccent,
                                colores = colores,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToTab(1) }
                            )
                            SuperStatCard(
                                numero = uiState.todasLasCitas.size.toString(),
                                label  = "Citas totales",
                                color  = ColorAzulClaro,
                                colores = colores,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToTab(4) }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SuperStatCard(
                                numero = uiState.barberos.size.toString(),
                                label  = "Barberos",
                                color  = ColorVerde,
                                colores = colores,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToTab(2) }
                            )
                            SuperStatCard(
                                numero = uiState.servicios.size.toString(),
                                label  = "Servicios",
                                color  = ColorRojo,
                                colores = colores,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToTab(3) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SuperSeccionTitulo("Gestión del sistema", colores)
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SuperAccionCard(Icons.Filled.ManageAccounts,
                                "Usuarios", "Crear · editar · eliminar · roles",
                                SuperAccent, colores,
                                onClick = { onNavigateToTab(1) })
                            SuperAccionCard(Icons.Filled.People,
                                "Barberos", "${uiState.barberos.size} registrados · " +
                                        "${uiState.barberos.count { it.activo == true }} activos",
                                ColorVerde, colores,
                                onClick = { onNavigateToTab(2) })
                            SuperAccionCard(Icons.Filled.ContentCut,
                                "Servicios", "${uiState.servicios.size} servicios activos",
                                ColorAzulClaro, colores,
                                onClick = { onNavigateToTab(3) })
                            SuperAccionCard(Icons.Filled.Shield,
                                "Roles disponibles",
                                "SUPERADMIN · ADMINISTRADOR · BARBERO · CLIENTE",
                                ColorRojo, colores,
                                onClick = { onNavigateToTab(1) })
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 1 — USUARIOS
// ══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperUsuariosTab(
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
    var usuarioAEditar    by remember { mutableStateOf<UsuarioDTO?>(null) }
    var editNombre        by remember { mutableStateOf("") }
    var editTelefono      by remember { mutableStateOf("") }
    var editRol           by remember { mutableStateOf(RolEnum.CLIENTE) }
    var editActivo        by remember { mutableStateOf(true) }
    var filtroRol         by remember { mutableStateOf<RolEnum?>(null) }
    var isRefreshing      by remember { mutableStateOf(false) }

    val usuariosFiltrados = if (filtroRol == null) uiState.usuarios
    else uiState.usuarios.filter { it.rol == filtroRol }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(
            Brush.horizontalGradient(
                listOf(SuperAccent, ColorBlanco, SuperAccent, ColorBlanco, SuperAccent)
            ))) {}
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.cargarDatosIniciales()
                isRefreshing = false
            },
            modifier = Modifier.weight(1f)
        ) {
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

            item {
                Column {
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
                                    Brush.horizontalGradient(listOf(SuperAccent, SuperAccent.copy(0.3f))))) {}
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
                                    Text("Rol", color = colores.textoSub,
                                        fontSize = 12.sp, letterSpacing = 0.5.sp)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(RolEnum.entries.toTypedArray()) { rol ->
                                            val seleccionado = rolSeleccionado == rol
                                            Box(modifier = Modifier
                                                .width(110.dp)
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
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
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
            }

            item {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    Button(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colores.superficie2,
                            contentColor = colores.texto),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (filtroRol == null) "Filtrar" else filtroRol!!.name,
                            fontSize = 13.sp)
                        Spacer(Modifier.width(4.dp))
                        Text("▼", fontSize = 10.sp)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todos") },
                            onClick = { filtroRol = null; expanded = false }
                        )
                        RolEnum.entries.forEach { rol ->
                            DropdownMenuItem(
                                text = { Text(rol.name) },
                                onClick = { filtroRol = rol; expanded = false }
                            )
                        }
                    }
                }
            }

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
                        usuario   = usuario,
                        colores   = colores,
                        onToggle  = { viewModel.toggleActivoUsuario(usuario) },
                        onEditar  = {
                            usuarioAEditar = usuario
                            editNombre = usuario.nombre ?: ""
                            editTelefono = usuario.telefono ?: ""
                            editRol = usuario.rol ?: RolEnum.CLIENTE
                            editActivo = usuario.activo ?: true
                        },
                        onEliminar = { usuarioAEliminar = usuario }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
    }

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

    usuarioAEditar?.let { usuario ->
        var errorMsg by remember(usuario.idUsuario) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { usuarioAEditar = null },
            containerColor   = colores.superficie,
            shape            = RoundedCornerShape(20.dp),
            title = { Text("Editar usuario", color = colores.texto,
                fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BarberiaTextField(editNombre, { editNombre = it },
                        "Nombre", Icons.Filled.Person, SuperAccent, colores)
                    BarberiaTextField(editTelefono, { editTelefono = it },
                        "Teléfono", Icons.Filled.Phone, SuperAccent, colores,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    Text("Rol", color = colores.textoSub, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(RolEnum.entries.toTypedArray()) { rol ->
                            val seleccionado = editRol == rol
                            Box(modifier = Modifier
                                .width(110.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (seleccionado) SuperAccent else colores.superficie2)
                                .border(1.dp, if (seleccionado) SuperAccent else colores.borde,
                                    RoundedCornerShape(20.dp))
                                .clickable { editRol = rol }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(rol.name,
                                    color = if (seleccionado) Color.White else colores.textoSub,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Activo", color = colores.texto, fontSize = 14.sp)
                        Switch(checked = editActivo,
                            onCheckedChange = { editActivo = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SuperAccent,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = colores.borde))
                    }
                    errorMsg?.let {
                        Text(it, color = ColorError, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                BarberiaBoton("Guardar", onClick = {
                    if (editNombre.isNotBlank()) {
                        viewModel.editarUsuario(
                            usuario.idUsuario!!, editNombre,
                            editTelefono.ifBlank { null }, editRol, editActivo
                        )
                        usuarioAEditar = null
                    }
                })
            },
            dismissButton = {
                TextButton(onClick = { usuarioAEditar = null }) {
                    Text("Cancelar", color = colores.textoSub) }
            }
        )
        LaunchedEffect(uiState.errorMessage) {
            if (uiState.errorMessage == "Este correo o teléfono ya está en uso") {
                errorMsg = uiState.errorMessage
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 2 — BARBEROS
// ══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperBarberosTab(
    uiState: com.example.barberia.viewmodel.SuperAdminUiState,
    viewModel: SuperAdminViewModel,
    colores: BarberiaColores
) {
    var mostrarFormulario  by remember { mutableStateOf(false) }
    var nombreBarbero      by remember { mutableStateOf("") }
    var especialidad       by remember { mutableStateOf("") }
    var telefono           by remember { mutableStateOf("") }
    var barberoAEliminar   by remember { mutableStateOf<BarberoDTO?>(null) }
    var barberoAEditar     by remember { mutableStateOf<BarberoDTO?>(null) }
    var editNombre         by remember { mutableStateOf("") }
    var editEspecialidad   by remember { mutableStateOf("") }
    var editTelefono       by remember { mutableStateOf("") }
    var editIdUsuario      by remember { mutableStateOf<Long?>(null) }
    var usuarioVinculado   by remember { mutableStateOf<Long?>(null) }
    var isRefreshing       by remember { mutableStateOf(false) }
    val usuariosBarbero = uiState.usuarios.filter {
        it.rol == RolEnum.BARBERO && it.activo == true
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(
            Brush.horizontalGradient(
                listOf(SuperAccent, ColorBlanco, SuperAccent, ColorBlanco, SuperAccent)
            ))) {}
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.cargarDatosIniciales()
                isRefreshing = false
            },
            modifier = Modifier.weight(1f)
        ) {
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
                Column {
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
                                    Brush.horizontalGradient(listOf(SuperAccent, ColorVerde)))) {}
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
            }

            items(uiState.barberos) { barbero ->
                TarjetaBarberoSuper(barbero, colores,
                    onToggle   = { viewModel.toggleActivoBarbero(barbero) },
                    onEditar   = {
                        barberoAEditar = barbero
                        editNombre = barbero.nombre
                        editEspecialidad = barbero.especialidad ?: ""
                        editTelefono = barbero.telefono ?: ""
                        editIdUsuario = barbero.idUsuario
                    },
                    onEliminar = { barberoAEliminar = barbero })
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
    }

    barberoAEliminar?.let { barbero ->
        AlertDialog(
            onDismissRequest = { barberoAEliminar = null },
            containerColor   = colores.superficie,
            shape            = RoundedCornerShape(20.dp),
            title = { Text("Eliminar barbero", color = colores.texto,
                fontWeight = FontWeight.Bold) },
            text  = { Text("¿Eliminar a ${barbero.nombre}? Las citas pendientes serán canceladas y los clientes notificados.",
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

    barberoAEditar?.let { barbero ->
        var errorMsg by remember(barbero.idBarbero) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { barberoAEditar = null },
            containerColor   = colores.superficie,
            shape            = RoundedCornerShape(20.dp),
            title = { Text("Editar barbero", color = colores.texto,
                fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BarberiaTextField(editNombre, { editNombre = it },
                        "Nombre *", Icons.Filled.Person, SuperAccent, colores)
                    BarberiaTextField(editEspecialidad, { editEspecialidad = it },
                        "Especialidad", Icons.Filled.ContentCut, SuperAccent, colores)
                    BarberiaTextField(editTelefono, { editTelefono = it },
                        "Teléfono", Icons.Filled.Phone, SuperAccent, colores,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    if (usuariosBarbero.isNotEmpty()) {
                        Text("Usuario vinculado", color = colores.textoSub, fontSize = 12.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                Box(modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (editIdUsuario == null)
                                        SuperAccent else colores.superficie2)
                                    .border(1.dp, if (editIdUsuario == null)
                                        SuperAccent else colores.borde,
                                        RoundedCornerShape(8.dp))
                                    .clickable { editIdUsuario = null }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Text("Sin vínculo",
                                        color = if (editIdUsuario == null)
                                            Color.White else colores.texto,
                                        fontSize = 11.sp)
                                }
                            }
                            items(usuariosBarbero) { user ->
                                val sel = editIdUsuario == user.idUsuario
                                Box(modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (sel) SuperAccent else colores.superficie2)
                                    .border(1.dp, if (sel) SuperAccent else colores.borde,
                                        RoundedCornerShape(8.dp))
                                    .clickable { editIdUsuario = user.idUsuario }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Text(user.nombre?.take(15) ?: "User #${user.idUsuario}",
                                        color = if (sel) Color.White else colores.texto,
                                        fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    errorMsg?.let {
                        Text(it, color = ColorError, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                BarberiaBoton("Guardar", onClick = {
                    if (editNombre.isNotBlank()) {
                        viewModel.editarBarbero(
                            barbero.idBarbero!!, editNombre,
                            editEspecialidad, editTelefono, editIdUsuario
                        )
                        barberoAEditar = null
                    }
                })
            },
            dismissButton = {
                TextButton(onClick = { barberoAEditar = null }) {
                    Text("Cancelar", color = colores.textoSub) }
            }
        )
        LaunchedEffect(uiState.errorMessage) {
            if (uiState.errorMessage == "Este teléfono ya está registrado, usa otro") {
                errorMsg = uiState.errorMessage
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 3 — SERVICIOS
// ══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperServiciosTab(
    uiState: com.example.barberia.viewmodel.SuperAdminUiState,
    viewModel: SuperAdminViewModel,
    colores: BarberiaColores
) {
    var mostrarFormulario  by remember { mutableStateOf(false) }
    var nombreServicio     by remember { mutableStateOf("") }
    var descripcion        by remember { mutableStateOf("") }
    var precio             by remember { mutableStateOf("") }
    var duracion           by remember { mutableStateOf("") }
    var servicioAEliminar  by remember { mutableStateOf<ServicioDTO?>(null) }
    var servicioAEditar    by remember { mutableStateOf<ServicioDTO?>(null) }
    var servicioDetalle    by remember { mutableStateOf<ServicioConDetalle?>(null) }
    var editNombre         by remember { mutableStateOf("") }
    var editDesc           by remember { mutableStateOf("") }
    var editPrecio         by remember { mutableStateOf("") }
    var editDuracion       by remember { mutableStateOf("") }
    var filtroServicio     by remember { mutableStateOf(0) }
    var isRefreshing       by remember { mutableStateOf(false) }
    val serviciosFiltrados = when (filtroServicio) {
        1 -> uiState.serviciosConDetalle
            .filter { it.barberoDTOs.isNotEmpty() || it.clienteDTOs.isNotEmpty() }
            .sortedByDescending { it.barberoDTOs.size + it.clienteDTOs.size }
            .map { it.servicio }
        2 -> uiState.servicios.sortedBy { it.precio }
        3 -> uiState.servicios.sortedByDescending { it.precio }
        else -> uiState.servicios
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(
            Brush.horizontalGradient(
                listOf(SuperAccent, ColorBlanco, SuperAccent, ColorBlanco, SuperAccent)
            ))) {}
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.cargarDatosIniciales()
                isRefreshing = false
            },
            modifier = Modifier.weight(1f)
        ) {
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
                var expanded by remember { mutableStateOf(false) }
                Box {
                    Button(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colores.superficie2,
                            contentColor = colores.texto),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        val filtroLabels = listOf(
                            "Todos", "Más populares",
                            "Precio bajo→alto", "Precio alto→bajo", "Por tipo"
                        )
                        Text(filtroLabels.getOrElse(filtroServicio) { "Filtrar" },
                            fontSize = 13.sp)
                        Spacer(Modifier.width(4.dp))
                        Text("▼", fontSize = 10.sp)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf(
                            0 to "Todos", 1 to "Más populares",
                            2 to "Precio bajo→alto", 3 to "Precio alto→bajo",
                            4 to "Por tipo"
                        ).forEach { (idx, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { filtroServicio = idx; expanded = false }
                            )
                        }
                    }
                }
            }

            item {
                Column {
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
                                    Brush.horizontalGradient(listOf(SuperAccent, ColorRojo)))) {}
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
            }

            items(serviciosFiltrados) { servicio ->
                TarjetaServicioSuper(
                    servicio  = servicio,
                    colores   = colores,
                    onDetalle = {
                        val det = uiState.serviciosConDetalle.find {
                            it.servicio.idServicio == servicio.idServicio
                        }
                        servicioDetalle = det ?: ServicioConDetalle(servicio = servicio)
                    },
                    onEditar  = {
                        servicioAEditar = servicio
                        editNombre = servicio.nombre
                        editDesc = servicio.descripcion ?: ""
                        editPrecio = servicio.precio.toInt().toString()
                        editDuracion = servicio.duracionMinutos.toString()
                    },
                    onEliminar = { servicioAEliminar = servicio }
                )
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
    }

    servicioDetalle?.let { det ->
        AlertDialog(
            onDismissRequest = { servicioDetalle = null },
            containerColor   = colores.superficie,
            shape            = RoundedCornerShape(20.dp),
            title = { Text(det.servicio.nombre, color = colores.texto,
                fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (det.barberoDTOs.isNotEmpty()) {
                        Text("Barberos asociados:", color = colores.texto,
                            fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        det.barberoDTOs.forEach { b ->
                            Text("• ${b.nombre}", color = colores.textoSub, fontSize = 12.sp)
                        }
                    }
                    if (det.clienteDTOs.isNotEmpty()) {
                        Text("Clientes asociados:", color = colores.texto,
                            fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        det.clienteDTOs.forEach { c ->
                            Text("• ${c.nombre ?: ""}", color = colores.textoSub, fontSize = 12.sp)
                        }
                    }
                    if (det.barberoDTOs.isEmpty() && det.clienteDTOs.isEmpty()) {
                        Text("Sin datos de barberos o clientes asociados",
                            color = colores.textoSub, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                BarberiaBoton("Cerrar", colorFondo = SuperAccent,
                    onClick = { servicioDetalle = null })
            }
        )
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

    servicioAEditar?.let { servicio ->
        AlertDialog(
            onDismissRequest = { servicioAEditar = null },
            containerColor   = colores.superficie,
            shape            = RoundedCornerShape(20.dp),
            title = { Text("Editar servicio", color = colores.texto,
                fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BarberiaTextField(editNombre, { editNombre = it },
                        "Nombre *", Icons.Filled.ContentCut, SuperAccent, colores)
                    BarberiaTextField(editDesc, { editDesc = it },
                        "Descripción", Icons.Filled.Description, SuperAccent, colores)
                    BarberiaTextField(editPrecio, { editPrecio = it },
                        "Precio", Icons.Filled.AttachMoney, SuperAccent, colores,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    BarberiaTextField(editDuracion, { editDuracion = it },
                        "Duración (min)", Icons.Filled.Schedule, SuperAccent, colores,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            },
            confirmButton = {
                BarberiaBoton("Guardar", onClick = {
                    val p = editPrecio.toDoubleOrNull()
                    val d = editDuracion.toIntOrNull()
                    if (editNombre.isNotBlank() && p != null && d != null) {
                        viewModel.actualizarServicio(
                            servicio.copy(
                                nombre = editNombre,
                                descripcion = editDesc.ifBlank { null },
                                precio = p,
                                duracionMinutos = d
                            )
                        )
                        servicioAEditar = null
                    }
                })
            },
            dismissButton = {
                TextButton(onClick = { servicioAEditar = null }) {
                    Text("Cancelar", color = colores.textoSub) }
            }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 4 — CITAS
// ══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperCitasTab(
    uiState: com.example.barberia.viewmodel.SuperAdminUiState,
    viewModel: SuperAdminViewModel,
    colores: BarberiaColores
) {
    var citaACancelar by remember { mutableStateOf<CitaConDetalle?>(null) }
    var citaDetalle by remember { mutableStateOf<CitaConDetalle?>(null) }
    var filtroEstado  by remember { mutableStateOf<EstadoCitaEnum?>(null) }
    var isRefreshing  by remember { mutableStateOf(false) }

    val citasFiltradas = if (filtroEstado == null) uiState.citasConDetalles
    else uiState.citasConDetalles.filter { it.cita.estado == filtroEstado }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(
            Brush.horizontalGradient(
                listOf(SuperAccent, ColorBlanco, SuperAccent, ColorBlanco, SuperAccent)
            ))) {}
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.cargarDatosIniciales()
                isRefreshing = false
            },
            modifier = Modifier.weight(1f)
        ) {
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

            item {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    Button(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colores.superficie2,
                            contentColor = colores.texto),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (filtroEstado == null) "Filtrar"
                            else when (filtroEstado) {
                                EstadoCitaEnum.PENDIENTE -> "Pendientes"
                                EstadoCitaEnum.EN_CURSO -> "En curso"
                                EstadoCitaEnum.FINALIZADA -> "Finalizadas"
                                EstadoCitaEnum.CANCELADA -> "Canceladas"
                                EstadoCitaEnum.NO_PRESENTADO -> "No presentó"
                                null -> "Filtrar"
                            }, fontSize = 13.sp)
                        Spacer(Modifier.width(4.dp))
                        Text("▼", fontSize = 10.sp)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todas") },
                            onClick = { filtroEstado = null; expanded = false }
                        )
                        EstadoCitaEnum.entries.forEach { estado ->
                            DropdownMenuItem(
                                text = {
                                    Text(when (estado) {
                                        EstadoCitaEnum.PENDIENTE -> "Pendientes"
                                        EstadoCitaEnum.EN_CURSO -> "En curso"
                                        EstadoCitaEnum.FINALIZADA -> "Finalizadas"
                                        EstadoCitaEnum.CANCELADA -> "Canceladas"
                                        EstadoCitaEnum.NO_PRESENTADO -> "No presentó"
                                    })
                                },
                                onClick = { filtroEstado = estado; expanded = false }
                            )
                        }
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
            } else if (citasFiltradas.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center) {
                        Text("Sin citas", color = colores.textoSub, fontSize = 15.sp)
                    }
                }
            } else {
                items(citasFiltradas) { detalle ->
                    TarjetaCitaSuper(
                        detalle  = detalle,
                        colores  = colores,
                        onClick  = { citaDetalle = detalle },
                        onCancelar = {
                            if (detalle.cita.estado == EstadoCitaEnum.PENDIENTE ||
                                detalle.cita.estado == EstadoCitaEnum.EN_CURSO)
                                citaACancelar = detalle
                        }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
    }

    citaACancelar?.let { detalle ->
        AlertDialog(
            onDismissRequest = { citaACancelar = null },
            containerColor   = colores.superficie,
            shape            = RoundedCornerShape(20.dp),
            title = { Text("Cancelar cita", color = colores.texto,
                fontWeight = FontWeight.Bold) },
            text  = { Text("¿Cancelar la cita #${detalle.cita.idCita} con ${detalle.clienteNombre}?",
                color = colores.textoSub, fontSize = 14.sp) },
            confirmButton = {
                BarberiaBoton("Sí, cancelar", colorFondo = ColorError, onClick = {
                    viewModel.cancelarCita(detalle.cita.idCita!!)
                    citaACancelar = null
                })
            },
            dismissButton = {
                TextButton(onClick = { citaACancelar = null }) {
                    Text("No", color = colores.textoSub) }
            }
        )
    }

    citaDetalle?.let { detalle ->
        AlertDialog(
            onDismissRequest = { citaDetalle = null },
            containerColor   = colores.superficie,
            shape            = RoundedCornerShape(20.dp),
            title = { Text("Cita #${detalle.cita.idCita}", color = colores.texto,
                fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetalleCitaItem("Fecha", detalle.cita.fecha, Icons.Filled.CalendarMonth, colores)
                    DetalleCitaItem("Hora inicio", detalle.cita.horaInicio.take(5), Icons.Filled.Schedule, colores)
                    DetalleCitaItem("Hora fin", detalle.cita.horaFin?.take(5) ?: "—", Icons.Filled.Schedule, colores)
                    DetalleCitaItem("Cliente", detalle.clienteNombre, Icons.Filled.Person, colores)
                    DetalleCitaItem("Barbero", detalle.barberoNombre, Icons.Filled.ContentCut, colores)
                    DetalleCitaItem("Servicio", detalle.servicioNombre, Icons.Filled.ShoppingBag, colores)
                    DetalleCitaItem("Estado", detalle.cita.estado?.name ?: "", Icons.Filled.Info, colores)
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (detalle.cita.estado == EstadoCitaEnum.PENDIENTE ||
                        detalle.cita.estado == EstadoCitaEnum.EN_CURSO) {
                        BarberiaBoton("Cancelar", colorFondo = ColorError, onClick = {
                            citaDetalle = null
                            citaACancelar = detalle
                        })
                    }
                    BarberiaBoton("Cerrar", colorFondo = SuperAccent, onClick = {
                        citaDetalle = null
                    })
                }
            }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// COMPONENTES
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun SuperStatCard(
    numero: String, label: String, color: Color,
    colores: BarberiaColores, modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(modifier = modifier
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(14.dp),
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
fun SuperSeccionTitulo(texto: String, colores: BarberiaColores) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.width(3.dp).height(18.dp)
            .clip(RoundedCornerShape(2.dp)).background(SuperAccent))
        Text(texto, color = colores.texto, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SuperAccionCard(
    icono: ImageVector,
    titulo: String, subtitulo: String, color: Color, colores: BarberiaColores,
    onClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()
        .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
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
fun TarjetaUsuarioSuper(
    usuario: UsuarioDTO, colores: BarberiaColores,
    onToggle: () -> Unit, onEditar: () -> Unit, onEliminar: () -> Unit
) {
    val colorRol = when (usuario.rol) {
        RolEnum.SUPERADMIN    -> SuperAccent
        RolEnum.ADMINISTRADOR -> ColorAzulClaro
        RolEnum.BARBERO       -> ColorAzul
        RolEnum.CLIENTE       -> ColorRojo
        null                  -> colores.textoSub
    }
    val textoRol = when (usuario.rol) {
        RolEnum.SUPERADMIN    -> "Super Administrador"
        RolEnum.ADMINISTRADOR -> "Administrador"
        RolEnum.BARBERO       -> "Barbero"
        RolEnum.CLIENTE       -> "Cliente"
        null                  -> "Desconocido"
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colores.superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = colores.sombra.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(colorRol)
                .padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text(textoRol, color = Color.White,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(usuario.nombre ?: "Sin nombre", color = colores.texto,
                fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            Text(usuario.correo ?: "", color = colores.textoSub,
                fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onEditar,
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuperAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Edit, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Editar", fontSize = 11.sp)
                    }
                    Button(
                        onClick = onEliminar,
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorError),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.DeleteOutline, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Eliminar", fontSize = 11.sp)
                    }
                }
                Switch(checked = usuario.activo ?: false,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor   = Color.White,
                        checkedTrackColor   = SuperAccent,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = colores.borde))
            }
        }
    }
}

@Composable
fun TarjetaBarberoSuper(
    barbero: BarberoDTO, colores: BarberiaColores,
    onToggle: () -> Unit, onEditar: () -> Unit, onEliminar: () -> Unit
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
                Text(getInitials(barbero.nombre).take(2),
                    color = if (barbero.activo == true) SuperAccent else colores.textoSub,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(barbero.nombre, color = colores.texto,
                    fontWeight = FontWeight.Medium, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(barbero.especialidad ?: "Sin especialidad",
                    color = colores.textoSub, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Switch(checked = barbero.activo ?: false,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor   = Color.White,
                    checkedTrackColor   = SuperAccent,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = colores.borde))
            IconButton(onClick = onEditar, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Edit, null,
                    tint = SuperAccent, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onEliminar, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.DeleteOutline, null,
                    tint = ColorError, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun TarjetaServicioSuper(
    servicio: ServicioDTO, colores: BarberiaColores,
    onDetalle: () -> Unit, onEditar: () -> Unit, onEliminar: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()
        .clickable { onDetalle() },
        shape = RoundedCornerShape(14.dp),
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
                    fontWeight = FontWeight.Medium, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${servicio.duracionMinutos} min",
                    color = colores.textoSub, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("\$${servicio.precio.toInt()}", color = SuperAccent,
                fontWeight = FontWeight.Bold, fontSize = 16.sp)
            IconButton(onClick = onEditar, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Edit, null,
                    tint = SuperAccent, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onEliminar, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.DeleteOutline, null,
                    tint = ColorError, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun TarjetaCitaSuper(
    detalle: CitaConDetalle,
    colores: BarberiaColores,
    onClick: () -> Unit = {},
    onCancelar: () -> Unit
) {
    val colorEstado = when (detalle.cita.estado) {
        EstadoCitaEnum.PENDIENTE     -> ColorDorado
        EstadoCitaEnum.EN_CURSO      -> ColorAzulClaro
        EstadoCitaEnum.FINALIZADA    -> ColorVerde
        EstadoCitaEnum.CANCELADA     -> ColorError
        EstadoCitaEnum.NO_PRESENTADO -> colores.textoSub
        null                         -> colores.textoSub
    }
    Card(modifier = Modifier.fillMaxWidth()
        .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colores.superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = colores.sombra.dp)) {
        Row {
            Box(modifier = Modifier.width(4.dp).height(100.dp).background(colorEstado))
            Column(modifier = Modifier.weight(1f)
                .padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column {
                    Text(detalle.clienteNombre, color = colores.texto,
                            fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${detalle.cita.fecha} · ${detalle.cita.horaInicio?.take(5)}",
                            color = colores.textoSub, fontSize = 12.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(colorEstado.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text(
                                text = when (detalle.cita.estado) {
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
                        if (detalle.cita.estado == EstadoCitaEnum.PENDIENTE ||
                            detalle.cita.estado == EstadoCitaEnum.EN_CURSO) {
                            IconButton(onClick = onCancelar,
                                modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Cancel, null,
                                    tint = ColorError, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(detalle.barberoNombre, color = colores.textoSub, fontSize = 11.sp)
                    Text(detalle.servicioNombre, color = colores.textoSub, fontSize = 11.sp)
                    Text("\$${detalle.precio.toInt()}", color = SuperAccent,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SuperFiltroChip(
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
@Composable
private fun DetalleCitaItem(label: String, value: String, icono: ImageVector, colores: BarberiaColores) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icono, null, tint = SuperAccent, modifier = Modifier.size(16.dp))
        Column {
            Text(label, color = colores.textoSub, fontSize = 11.sp)
            Text(value, color = colores.texto, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
