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
import androidx.compose.ui.draw.scale
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
import com.example.barberia.viewmodel.AdminViewModel
import kotlinx.coroutines.launch

private val AdminAccent     = ColorAzul
private val AdminAccentSoft = ColorAzul.copy(alpha = 0.15f)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdminScreen(
    apiService: ApiService,
    nombre: String,
    idUsuario: Long = 0L,
    onLogout: () -> Unit,
    onNavigateToPerfil: (Long) -> Unit = {},
    onNavigateToNotificaciones: () -> Unit = {}
) {
    val colores       = LocalBarberiaColores.current

    val viewModel: AdminViewModel = viewModel(
        factory = AdminViewModel.factory(apiService)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pagerState    = rememberPagerState(pageCount = { 6 })
    val scope         = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
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
                            .background(AdminAccent),
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
                    icon = { Icon(Icons.Filled.Person, null, tint = AdminAccent) },
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
                                tint = if (TemaManager.modoOscuro.value == mode) AdminAccent
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
                            actionColor    = AdminAccent,
                            shape          = RoundedCornerShape(12.dp))
                    }
                },
                bottomBar = {
                    AdminBottomBar(pagerState.currentPage, colores) { index ->
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
                         0 -> AdminInicioTab(nombre, uiState, viewModel,
                             onLogout, colores,
                             onOpenDrawer = { scope.launch { drawerState.open() } },
                             onNavigateToTab = { scope.launch { pagerState.animateScrollToPage(it) } },
                             onNavigateToNotificaciones = onNavigateToNotificaciones)
                        1 -> AdminCitasTab(uiState, viewModel, colores)
                        2 -> AdminBarberosTab(uiState, viewModel, colores)
                        3 -> AdminServiciosTab(uiState, viewModel, colores)
                        4 -> AdminHorariosTab(uiState, viewModel, colores)
                        5 -> AdminResenasTab(uiState, viewModel, colores)
                    }
                }
        }
    }
}

// ── Bottom Bar ──────────────────────────────────────────────────────────────
@Composable
private fun AdminBottomBar(
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
            Triple("Citas",     Icons.Filled.CalendarMonth, 1),
            Triple("Barberos",  Icons.Filled.People,        2),
            Triple("Servicios", Icons.Filled.ContentCut,    3),
            Triple("Horarios",  Icons.Filled.Schedule,      4),
            Triple("Reseñas",   Icons.Filled.Star,          5)
        ).forEach { (label, icon, index) ->
            val activo = paginaActual == index
            NavigationBarItem(
                selected = activo,
                onClick  = { onTabSelected(index) },
                icon = { Icon(icon, label,
                    tint = if (activo) AdminAccent else colores.textoSub,
                    modifier = Modifier.size(22.dp)) },
                label = { Text(label,
                    color = if (activo) AdminAccent else colores.textoSub,
                    fontSize = 11.sp,
                    fontWeight = if (activo) FontWeight.SemiBold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = AdminAccentSoft)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 0 — INICIO
// ══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminInicioTab(
    nombre: String,
    uiState: com.example.barberia.viewmodel.AdminUiState,
    viewModel: AdminViewModel,
    onLogout: () -> Unit,
    colores: BarberiaColores,
    onOpenDrawer: () -> Unit = {},
    onNavigateToTab: (Int) -> Unit,
    onNavigateToNotificaciones: () -> Unit = {}
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            viewModel.cargarDatosIniciales()
            isRefreshing = false
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(colores.fondo)
        ) {
            item {
                Box(modifier = Modifier.fillMaxWidth().background(
                    Brush.verticalGradient(
                        if (colores.esModoOscuro)
                            listOf(Color(0xFF0A0E14), colores.fondo)
                        else
                            listOf(Color(0xFFF0F4FF), colores.fondo)
                    )
                )) {
                    Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(
                        Brush.horizontalGradient(
                            listOf(AdminAccent, ColorBlanco, ColorRojo, ColorBlanco, AdminAccent)
                        )))
                    Column(modifier = Modifier.padding(
                        start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                                    .background(AdminAccent.copy(0.15f))
                                    .border(1.5.dp, AdminAccent, CircleShape)
                                    .clickable { onOpenDrawer() },
                                    contentAlignment = Alignment.Center) {
                                    Text(getInitials(nombre), color = AdminAccent,
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
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }

            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AdminAccent,
                            strokeWidth = 2.5.dp, modifier = Modifier.size(36.dp))
                    }
                }
            } else {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AdminStatCard(
                                numero = uiState.citasHoy.size.toString(),
                                label  = "Citas hoy",
                                color  = AdminAccent,
                                colores = colores,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToTab(1) }
                            )
                            AdminStatCard(
                                numero = uiState.barberos.count { it.activo == true }.toString(),
                                label  = "Barberos activos",
                                color  = ColorVerde,
                                colores = colores,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToTab(2) }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AdminStatCard(
                                numero = uiState.citasHoy.count {
                                    it.estado == EstadoCitaEnum.CANCELADA }.toString(),
                                label  = "Cancelaciones hoy",
                                color  = ColorError,
                                colores = colores,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToTab(1) }
                            )
                            AdminStatCard(
                                numero = uiState.servicios.size.toString(),
                                label  = "Servicios",
                                color  = ColorDorado,
                                colores = colores,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToTab(3) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 1 — CITAS
// ══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminCitasTab(
    uiState: com.example.barberia.viewmodel.AdminUiState,
    viewModel: AdminViewModel,
    colores: BarberiaColores
) {
    var citaACancelar by remember { mutableStateOf<CitaConDetalle?>(null) }
    var filtroEstado  by remember { mutableStateOf<EstadoCitaEnum?>(null) }
    var citaDetalle   by remember { mutableStateOf<CitaConDetalle?>(null) }
    var isRefreshing  by remember { mutableStateOf(false) }

    val citasFiltradas = if (filtroEstado == null) uiState.citasConDetalles
    else uiState.citasConDetalles.filter { it.cita.estado == filtroEstado }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(
            Brush.horizontalGradient(
                listOf(AdminAccent, ColorBlanco, ColorRojo, ColorBlanco, AdminAccent)
            )))
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
                    Text("${uiState.todasLasCitas.size} citas en total",
                        color = colores.textoSub, fontSize = 13.sp)
                }

            item {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }) {
                        Text(filtroEstado?.let {
                            when (it) {
                                EstadoCitaEnum.PENDIENTE -> "Filtrar · Pendientes"
                                EstadoCitaEnum.EN_CURSO -> "Filtrar · En curso"
                                EstadoCitaEnum.FINALIZADA -> "Filtrar · Finalizadas"
                                EstadoCitaEnum.CANCELADA -> "Filtrar · Canceladas"
                                EstadoCitaEnum.NO_PRESENTADO -> "Filtrar · No presentó"
                            }
                        } ?: "Filtrar", color = colores.texto, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Filled.ArrowDropDown, null, tint = colores.textoSub)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todas",
                                color = if (filtroEstado == null) AdminAccent else colores.texto) },
                            onClick = { filtroEstado = null; expanded = false }
                        )
                        EstadoCitaEnum.entries.forEach { estado ->
                            DropdownMenuItem(
                                text = { Text(
                                    when (estado) {
                                        EstadoCitaEnum.PENDIENTE -> "Pendientes"
                                        EstadoCitaEnum.EN_CURSO -> "En curso"
                                        EstadoCitaEnum.FINALIZADA -> "Finalizadas"
                                        EstadoCitaEnum.CANCELADA -> "Canceladas"
                                        EstadoCitaEnum.NO_PRESENTADO -> "No presentó"
                                    },
                                    color = if (filtroEstado == estado) AdminAccent else colores.texto
                                )},
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
                        CircularProgressIndicator(color = AdminAccent,
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
                    TarjetaCitaAdmin(
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
                    DetalleItem("Fecha", detalle.cita.fecha ?: "", Icons.Filled.CalendarMonth, colores)
                    DetalleItem("Inicio", detalle.cita.horaInicio?.take(5) ?: "", Icons.Filled.Schedule, colores)
                    DetalleItem("Fin", detalle.cita.horaFin?.take(5) ?: "", Icons.Filled.Schedule, colores)
                    DetalleItem("Cliente", detalle.clienteNombre, Icons.Filled.Person, colores)
                    DetalleItem("Barbero", detalle.barberoNombre, Icons.Filled.ContentCut, colores)
                    DetalleItem("Servicio", detalle.servicioNombre, Icons.Filled.ShoppingBag, colores)
                    DetalleItem("Estado", detalle.cita.estado?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "",
                        Icons.Filled.Info, colores)
                }
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (detalle.cita.estado) {
                        EstadoCitaEnum.PENDIENTE -> {
                            BarberiaBoton("Cancelar", colorFondo = ColorError, onClick = {
                                viewModel.cancelarCita(detalle.cita.idCita!!)
                                citaDetalle = null
                            })
                        }
                        EstadoCitaEnum.EN_CURSO -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BarberiaBoton("Finalizar", colorFondo = ColorVerde, onClick = {
                                    viewModel.finalizarCita(detalle.cita.idCita!!)
                                    citaDetalle = null
                                })
                                BarberiaBoton("No presentó", colorFondo = colores.textoSub, onClick = {
                                    viewModel.noPresentoCita(detalle.cita.idCita!!)
                                    citaDetalle = null
                                })
                            }
                        }
                        else -> {}
                    }
                    BarberiaBoton("Cerrar", colorFondo = AdminAccent, onClick = { citaDetalle = null })
                }
            }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 2 — BARBEROS
// ══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminBarberosTab(
    uiState: com.example.barberia.viewmodel.AdminUiState,
    viewModel: AdminViewModel,
    colores: BarberiaColores
) {
    var mostrarFormulario  by remember { mutableStateOf(false) }
    var nombreBarbero      by remember { mutableStateOf("") }
    var especialidad       by remember { mutableStateOf("") }
    var telefono           by remember { mutableStateOf("") }
    var usuarioVinculado   by remember { mutableStateOf<Long?>(null) }
    var barberoAEliminar   by remember { mutableStateOf<BarberoDTO?>(null) }
    var barberoAEditar     by remember { mutableStateOf<BarberoDTO?>(null) }
    var editNombre         by remember { mutableStateOf("") }
    var editEspecialidad   by remember { mutableStateOf("") }
    var editTelefono       by remember { mutableStateOf("") }
    var editIdUsuario      by remember { mutableStateOf<Long?>(null) }
    var isRefreshing       by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(
            Brush.horizontalGradient(
                listOf(AdminAccent, ColorBlanco, ColorRojo, ColorBlanco, AdminAccent)
            )))
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
                        FloatingActionButton(
                            onClick = { mostrarFormulario = !mostrarFormulario },
                            containerColor = AdminAccent,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                if (mostrarFormulario) Icons.Filled.Close else Icons.Filled.Add,
                                null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }

            item {
                Column {
                    AnimatedVisibility(
                        visible = mostrarFormulario,
                        enter   = expandVertically() + fadeIn(),
                        exit    = shrinkVertically() + fadeOut()
                    ) {
                        Card(modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = colores.superficie),
                            elevation = CardDefaults.cardElevation(defaultElevation = colores.sombra.dp)) {
                            Column {
                                Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(
                                    Brush.horizontalGradient(listOf(AdminAccent, ColorAzul))))
                            Column(modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Nuevo barbero", color = colores.texto,
                                    fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                BarberiaTextField(nombreBarbero,
                                    { nombreBarbero = it }, "Nombre *",
                                    Icons.Filled.Person, AdminAccent, colores)
                                BarberiaTextField(especialidad,
                                    { especialidad = it }, "Especialidad",
                                    Icons.Filled.ContentCut, AdminAccent, colores)
                                BarberiaTextField(telefono,
                                    { telefono = it }, "Teléfono",
                                    Icons.Filled.Phone, AdminAccent, colores,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Phone))

                                if (uiState.usuariosBarbero.isNotEmpty()) {
                                    Text("Vincular con usuario",
                                        color = colores.textoSub, fontSize = 12.sp)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        item {
                                            Box(modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (usuarioVinculado == null)
                                                    AdminAccent else colores.superficie2)
                                                .border(1.dp, if (usuarioVinculado == null)
                                                    AdminAccent else colores.borde,
                                                    RoundedCornerShape(8.dp))
                                                .clickable { usuarioVinculado = null }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)) {
                                                Text("Sin vínculo",
                                                    color = if (usuarioVinculado == null)
                                                        Color.White else colores.texto,
                                                    fontSize = 11.sp)
                                            }
                                        }
                                        items(uiState.usuariosBarbero) { user ->
                                            val sel = usuarioVinculado == user.idUsuario
                                            Box(modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (sel) AdminAccent else colores.superficie2)
                                                .border(1.dp, if (sel) AdminAccent else colores.borde,
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
                                        AdminAccent else colores.borde,
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
                TarjetaBarberoAdmin(
                    barbero   = barbero,
                    colores   = colores,
                    onToggle  = { viewModel.toggleActivoBarbero(barbero) },
                    onEditar  = {
                        barberoAEditar = barbero
                        editNombre = barbero.nombre
                        editEspecialidad = barbero.especialidad ?: ""
                        editTelefono = barbero.telefono ?: ""
                        editIdUsuario = barbero.idUsuario
                    },
                    onEliminar = { barberoAEliminar = barbero }
                )
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
            text  = { Text("¿Eliminar a ${barbero.nombre}? Esta acción no se puede deshacer.",
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
                        "Nombre *", Icons.Filled.Person, AdminAccent, colores)
                    BarberiaTextField(editEspecialidad, { editEspecialidad = it },
                        "Especialidad", Icons.Filled.ContentCut, AdminAccent, colores)
                    BarberiaTextField(editTelefono, { editTelefono = it },
                        "Teléfono", Icons.Filled.Phone, AdminAccent, colores,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    if (uiState.usuariosBarbero.isNotEmpty()) {
                        Text("Usuario vinculado", color = colores.textoSub, fontSize = 12.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                Box(modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (editIdUsuario == null)
                                        AdminAccent else colores.superficie2)
                                    .border(1.dp, if (editIdUsuario == null)
                                        AdminAccent else colores.borde,
                                        RoundedCornerShape(8.dp))
                                    .clickable { editIdUsuario = null }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Text("Sin vínculo",
                                        color = if (editIdUsuario == null)
                                            Color.White else colores.texto,
                                        fontSize = 11.sp)
                                }
                            }
                            items(uiState.usuariosBarbero) { user ->
                                val sel = editIdUsuario == user.idUsuario
                                Box(modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (sel) AdminAccent else colores.superficie2)
                                    .border(1.dp, if (sel) AdminAccent else colores.borde,
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
private fun AdminServiciosTab(
    uiState: com.example.barberia.viewmodel.AdminUiState,
    viewModel: AdminViewModel,
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

    LaunchedEffect(uiState.serviciosConDetalle) { }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(
            Brush.horizontalGradient(
                listOf(AdminAccent, ColorBlanco, ColorRojo, ColorBlanco, AdminAccent)
            )))
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
                        FloatingActionButton(
                            onClick = { mostrarFormulario = !mostrarFormulario },
                            containerColor = ColorVerde,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                if (mostrarFormulario) Icons.Filled.Close else Icons.Filled.Add,
                                null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }

            item {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }) {
                        Text("Ordenar", color = colores.texto, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Filled.ArrowDropDown, null, tint = colores.textoSub)
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
                                text = { Text(label,
                                    color = if (filtroServicio == idx) ColorVerde else colores.texto) },
                                onClick = { filtroServicio = idx; expanded = false }
                            )
                        }
                    }
                }
            }

            item {
                Column {
                AnimatedVisibility(
                    visible = mostrarFormulario,
                    enter   = expandVertically() + fadeIn(),
                    exit    = shrinkVertically() + fadeOut()
                ) {
                    Card(modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colores.superficie),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = colores.sombra.dp)) {
                        Column {
                            Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(
                                Brush.horizontalGradient(listOf(ColorVerde, AdminAccent))))
                            Column(modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Nuevo servicio", color = colores.texto,
                                    fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                BarberiaTextField(nombreServicio,
                                    { nombreServicio = it }, "Nombre *",
                                    Icons.Filled.ContentCut, ColorVerde, colores)
                                BarberiaTextField(descripcion,
                                    { descripcion = it }, "Descripción",
                                    Icons.Filled.Description, ColorVerde, colores)
                                BarberiaTextField(precio,
                                    { precio = it }, "Precio (ej: 15000)",
                                    Icons.Filled.AttachMoney, ColorVerde, colores,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number))
                                BarberiaTextField(duracion,
                                    { duracion = it }, "Duración en minutos",
                                    Icons.Filled.Schedule, ColorVerde, colores,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number))
                                val formListo = nombreServicio.isNotBlank() &&
                                        precio.toDoubleOrNull() != null &&
                                        duracion.toIntOrNull() != null
                                BarberiaBoton(
                                    texto      = "Crear servicio",
                                    icono      = Icons.Filled.Add,
                                    isLoading  = uiState.isLoading,
                                    colorFondo = if (formListo) ColorVerde else colores.borde,
                                    onClick    = {
                                        if (formListo) {
                                            viewModel.crearServicio(
                                                nombreServicio, descripcion,
                                                precio.toDouble(), duracion.toInt()
                                            )
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
                TarjetaServicioAdmin(
                    servicio   = servicio,
                    colores    = colores,
                    onDetalle  = {
                        val det = uiState.serviciosConDetalle.find {
                            it.servicio.idServicio == servicio.idServicio
                        }
                        servicioDetalle = det ?: ServicioConDetalle(servicio = servicio)
                    },
                    onEditar   = {
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
                        det.barberoDTOs.forEach { barbero ->
                            DetalleItem("Barbero", barbero.nombre, Icons.Filled.Person, colores)
                        }
                    }
                    if (det.clienteDTOs.isNotEmpty()) {
                        det.clienteDTOs.forEach { cliente ->
                            DetalleItem("Cliente", cliente.nombre, Icons.Filled.Person, colores)
                        }
                    }
                    if (det.barberoDTOs.isEmpty() && det.clienteDTOs.isEmpty()) {
                        Text("Sin datos de barberos o clientes asociados",
                            color = colores.textoSub, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                BarberiaBoton("Cerrar", colorFondo = AdminAccent,
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
            text  = { Text("¿Eliminar el servicio '${servicio.nombre}'?",
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
                        "Nombre *", Icons.Filled.ContentCut, ColorVerde, colores)
                    BarberiaTextField(editDesc, { editDesc = it },
                        "Descripción", Icons.Filled.Description, ColorVerde, colores)
                    BarberiaTextField(editPrecio, { editPrecio = it },
                        "Precio", Icons.Filled.AttachMoney, ColorVerde, colores,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    BarberiaTextField(editDuracion, { editDuracion = it },
                        "Duración (min)", Icons.Filled.Schedule, ColorVerde, colores,
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
// TAB 4 — HORARIOS
// ══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminHorariosTab(
    uiState: com.example.barberia.viewmodel.AdminUiState,
    viewModel: AdminViewModel,
    colores: BarberiaColores
) {
    var diaSeleccionado   by remember { mutableStateOf<DiaSemanaEnum?>(null) }
    var turnoSeleccionado by remember { mutableStateOf<String?>(null) }
    var mostrarFormulario by remember { mutableStateOf(false) }
    var horarioAEliminar  by remember { mutableStateOf<HorarioBarberoDTO?>(null) }
    var citaSeleccionada  by remember { mutableStateOf<CitaConDetalle?>(null) }
    var mostrarConfirmEliminarCita by remember { mutableStateOf(false) }
    var citaEliminadaOk   by remember { mutableStateOf(false) }
    var isRefreshing      by remember { mutableStateOf(false) }
    var horaInicioCustom  by remember { mutableStateOf("09:00") }
    var horaFinCustom     by remember { mutableStateOf("19:00") }
    var usarHorarioCustom by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(
            Brush.horizontalGradient(
                listOf(AdminAccent, ColorBlanco, ColorRojo, ColorBlanco, AdminAccent)
            )))
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                uiState.barberoSeleccionado?.let {
                    viewModel.cargarHorarios(it.idBarbero!!)
                }
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
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Horarios", color = colores.texto,
                        fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Selecciona un barbero para gestionar sus horarios",
                        color = colores.textoSub, fontSize = 13.sp)
                }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.barberos.forEach { barbero ->
                        val sel = uiState.barberoSeleccionado?.idBarbero == barbero.idBarbero
                        Card(
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    viewModel.seleccionarBarbero(barbero)
                                    diaSeleccionado = null
                                    mostrarFormulario = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (sel) AdminAccent else colores.superficie),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = colores.sombra.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                                    .background(if (sel) Color.White.copy(0.2f) else AdminAccentSoft),
                                    contentAlignment = Alignment.Center) {
                                    Text(barbero.nombre.take(2).uppercase(),
                                        color = if (sel) Color.White else AdminAccent,
                                        fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(barbero.nombre,
                                        color = if (sel) Color.White else colores.texto,
                                        fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text(barbero.especialidad ?: "Sin especialidad",
                                        color = if (sel) Color.White.copy(0.7f) else colores.textoSub,
                                        fontSize = 12.sp)
                                }
                                Icon(Icons.Filled.ChevronRight, null,
                                    tint = if (sel) Color.White else colores.textoSub,
                                    modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            if (uiState.barberoSeleccionado != null) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Horarios de ${uiState.barberoSeleccionado!!.nombre}",
                                color = colores.texto, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Toca un día para agregar horario",
                                color = colores.textoSub, fontSize = 12.sp)
                        }
                        FloatingActionButton(
                            onClick = { mostrarFormulario = !mostrarFormulario },
                            containerColor = AdminAccent,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                if (mostrarFormulario) Icons.Filled.Close
                                else Icons.Filled.Add,
                                null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                if (mostrarFormulario) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = colores.superficie),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = colores.sombra.dp)) {
                            Column(modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Nuevo horario", color = colores.texto,
                                    fontWeight = FontWeight.Bold, fontSize = 14.sp)

                                Text("Día de la semana", color = colores.textoSub,
                                    fontSize = 12.sp)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(DiaSemanaEnum.entries.toTypedArray()) { dia ->
                                        val sel = diaSeleccionado == dia
                                        Box(modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (sel) AdminAccent else colores.superficie2)
                                            .border(1.dp, if (sel) AdminAccent else colores.borde,
                                                RoundedCornerShape(8.dp))
                                            .clickable { diaSeleccionado = dia }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)) {
                                            Text(dia.name.lowercase().replaceFirstChar { it.uppercase() },
                                                color = if (sel) Color.White else colores.texto,
                                                fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text("Horario personalizado", color = colores.textoSub, fontSize = 12.sp)
                                    Switch(checked = usarHorarioCustom,
                                        onCheckedChange = { usarHorarioCustom = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = AdminAccent,
                                            uncheckedThumbColor = Color.White,
                                            uncheckedTrackColor = colores.borde))
                                }

                                if (usarHorarioCustom) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = horaInicioCustom,
                                            onValueChange = { horaInicioCustom = it },
                                            label = { Text("Inicio", fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                                        )
                                        OutlinedTextField(
                                            value = horaFinCustom,
                                            onValueChange = { horaFinCustom = it },
                                            label = { Text("Fin", fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                                        )
                                    }
                                } else {
                                    val turnos = remember {
                                        listOf(
                                            "09:00-19:00" to "Completo (10h)",
                                            "09:00-17:00" to "Mañana (8h)",
                                            "10:00-19:00" to "Tarde (9h)",
                                            "09:00-13:00" to "Media mañana (4h)",
                                            "14:00-19:00" to "Media tarde (5h)",
                                            "12:00-19:00" to "Tarde reducida (7h)"
                                        )
                                    }
                                    Text("Turnos predefinidos", color = colores.textoSub, fontSize = 12.sp)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(turnos) { (bloque, label) ->
                                            val sel = turnoSeleccionado == bloque
                                            FilterChip(
                                                selected = sel,
                                                onClick = { turnoSeleccionado = if (sel) null else bloque },
                                                label = { Text(label, fontSize = 10.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = AdminAccent,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }

                                val puedeGuardar = diaSeleccionado != null &&
                                        (turnoSeleccionado != null || (usarHorarioCustom &&
                                                horaInicioCustom.isNotBlank() && horaFinCustom.isNotBlank()))
                                BarberiaBoton(
                                    texto      = "Agregar horario",
                                    icono      = Icons.Filled.Add,
                                    isLoading  = uiState.isLoading,
                                    colorFondo = if (puedeGuardar) AdminAccent else colores.borde,
                                    onClick    = {
                                        if (usarHorarioCustom) {
                                            viewModel.crearHorario(
                                                idBarbero  = uiState.barberoSeleccionado!!.idBarbero!!,
                                                diaSemana  = diaSeleccionado!!,
                                                horaInicio = horaInicioCustom,
                                                horaFin    = horaFinCustom
                                            )
                                        } else {
                                            turnoSeleccionado?.let { bloque ->
                                                val parts = bloque.split("-")
                                                if (parts.size == 2) {
                                                    viewModel.crearHorario(
                                                        idBarbero  = uiState.barberoSeleccionado!!.idBarbero!!,
                                                        diaSemana  = diaSeleccionado!!,
                                                        horaInicio = parts[0],
                                                        horaFin    = parts[1]
                                                    )
                                                }
                                            }
                                        }
                                        turnoSeleccionado = null
                                        diaSeleccionado = null
                                        mostrarFormulario = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        items(DiaSemanaEnum.entries.toTypedArray()) { dia ->
                            val horariosDia = uiState.horarios.filter { it.diaSemana == dia }
                            val tieneHorario = horariosDia.isNotEmpty()
                            val seleccionado = diaSeleccionado == dia
                            Box(modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (seleccionado) AdminAccent else colores.superficie)
                                .border(1.dp, if (tieneHorario) AdminAccent else colores.borde,
                                    RoundedCornerShape(12.dp))
                                .clickable { diaSeleccionado = dia }
                                .padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(dia.name.take(3),
                                        color = if (seleccionado) Color.White else colores.texto,
                                        fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    if (tieneHorario) {
                                        Text("${horariosDia.size}",
                                            color = if (seleccionado) Color.White.copy(0.8f) else AdminAccent,
                                            fontSize = 10.sp)
                                    } else {
                                        Text("Libre",
                                            color = if (seleccionado) Color.White.copy(0.8f) else ColorRojo.copy(0.6f),
                                            fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                if (diaSeleccionado != null) {
                    val horariosDelDia = uiState.horarios.filter { it.diaSemana == diaSeleccionado }
                    if (horariosDelDia.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp),
                                contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.Schedule, null,
                                        tint = colores.textoSub, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Sin horario para ${diaSeleccionado!!.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                        color = colores.textoSub, fontSize = 13.sp)
                                    Text("Toca + para agregar",
                                        color = colores.textoSub, fontSize = 11.sp)
                                }
                            }
                        }
                    } else {
                        items(horariosDelDia) { horario ->
                            Card(modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = colores.superficie),
                                elevation = CardDefaults.cardElevation(defaultElevation = colores.sombra.dp)) {
                                Row(modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                                            .background(AdminAccentSoft),
                                            contentAlignment = Alignment.Center) {
                                            Icon(Icons.Filled.Schedule, null,
                                                tint = AdminAccent, modifier = Modifier.size(18.dp))
                                        }
                                        Column {
                                            Text("${horario.horaInicio?.take(5)} - ${horario.horaFin?.take(5)}",
                                                color = colores.texto, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(diaSeleccionado!!.name.lowercase().replaceFirstChar { it.uppercase() },
                                                color = colores.textoSub, fontSize = 11.sp)
                                        }
                                    }
                                    IconButton(onClick = { horarioAEliminar = horario }) {
                                        Icon(Icons.Filled.DeleteOutline, null,
                                            tint = ColorError, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
    }

    horarioAEliminar?.let { horario ->
        AlertDialog(
            onDismissRequest = { horarioAEliminar = null },
            containerColor   = colores.superficie,
            shape            = RoundedCornerShape(20.dp),
            title = { Text("Eliminar horario", color = colores.texto,
                fontWeight = FontWeight.Bold) },
            text  = { Text("¿Eliminar el horario de ${horario.horaInicio?.take(5)} a ${horario.horaFin?.take(5)}?",
                color = colores.textoSub, fontSize = 14.sp) },
            confirmButton = {
                BarberiaBoton("Eliminar", colorFondo = ColorError, onClick = {
                    viewModel.eliminarHorario(
                        horario.idHorario!!,
                        uiState.barberoSeleccionado!!.idBarbero!!
                    )
                    horarioAEliminar = null
                })
            },
            dismissButton = {
                TextButton(onClick = { horarioAEliminar = null }) {
                    Text("Cancelar", color = colores.textoSub) }
            }
        )
    }

    citaSeleccionada?.let { detalle ->
        if (mostrarConfirmEliminarCita) {
            AlertDialog(
                onDismissRequest = { mostrarConfirmEliminarCita = false },
                containerColor   = colores.superficie,
                shape            = RoundedCornerShape(20.dp),
                title = { Text("Eliminar cita", color = colores.texto,
                    fontWeight = FontWeight.Bold) },
                text = { Text("¿Eliminar la cita de ${detalle.clienteNombre} con ${detalle.barberoNombre}? Se notificará a ambas partes.",
                    color = colores.textoSub, fontSize = 14.sp) },
                confirmButton = {
                    BarberiaBoton("Eliminar", colorFondo = ColorError, onClick = {
                        viewModel.cancelarCita(detalle.cita.idCita!!)
                        mostrarConfirmEliminarCita = false
                        citaEliminadaOk = true
                    })
                },
                dismissButton = {
                    TextButton(onClick = { mostrarConfirmEliminarCita = false }) {
                        Text("Cancelar", color = colores.textoSub) }
                }
            )
        } else if (citaEliminadaOk) {
            AlertDialog(
                onDismissRequest = { citaEliminadaOk = false; citaSeleccionada = null },
                containerColor   = colores.superficie,
                shape            = RoundedCornerShape(20.dp),
                title = { Text("Cita eliminada", color = colores.texto,
                    fontWeight = FontWeight.Bold) },
                text = { Text("La cita ha sido cancelada. Se ha notificado al barbero y al cliente.",
                    color = colores.textoSub, fontSize = 14.sp) },
                confirmButton = {
                    BarberiaBoton("Aceptar", colorFondo = AdminAccent, onClick = {
                        citaEliminadaOk = false; citaSeleccionada = null
                    })
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { citaSeleccionada = null },
                containerColor   = colores.superficie,
                shape            = RoundedCornerShape(20.dp),
                title = { Text("Detalle de cita", color = colores.texto,
                    fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetalleItem("Cliente", detalle.clienteNombre, Icons.Filled.Person, colores)
                        DetalleItem("Barbero", detalle.barberoNombre, Icons.Filled.ContentCut, colores)
                        DetalleItem("Servicio", detalle.servicioNombre, Icons.Filled.ShoppingBag, colores)
                        DetalleItem("Fecha", detalle.cita.fecha ?: "", Icons.Filled.CalendarMonth, colores)
                        DetalleItem("Hora", detalle.cita.horaInicio?.take(5) ?: "", Icons.Filled.Schedule, colores)
                        DetalleItem("Precio", "\$${detalle.precio.toInt()}", Icons.Filled.AttachMoney, colores)
                    }
                },
                confirmButton = {
                    Column {
                        BarberiaBoton("Eliminar cita", colorFondo = ColorError, onClick = {
                            mostrarConfirmEliminarCita = true
                        })
                        Spacer(modifier = Modifier.height(8.dp))
                        BarberiaBoton("Cerrar", colorFondo = AdminAccent, onClick = {
                            citaSeleccionada = null
                        })
                    }
                }
            )
        }
    }
}

@Composable
private fun DetalleItem(label: String, value: String, icono: ImageVector, colores: BarberiaColores) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icono, null, tint = AdminAccent, modifier = Modifier.size(16.dp))
        Column {
            Text(label, color = colores.textoSub, fontSize = 11.sp)
            Text(value, color = colores.texto, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 5 — RESEÑAS
// ══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminResenasTab(
    uiState: com.example.barberia.viewmodel.AdminUiState,
    viewModel: AdminViewModel,
    colores: BarberiaColores
) {
    var resenaAEliminar        by remember { mutableStateOf<ResenaDTO?>(null) }
    var barberoFiltroResena    by remember { mutableStateOf<BarberoDTO?>(null) }
    var resenaSeleccionada     by remember { mutableStateOf<ResenaDTO?>(null) }
    var isRefreshing           by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.cargarResenas() }

    val promedioGeneral = remember(uiState.resenas) {
        if (uiState.resenas.isEmpty()) 0.0
        else uiState.resenas.map { it.calificacion }.average()
    }

    val resenasFiltradas = if (barberoFiltroResena == null) uiState.resenas
    else uiState.resenas.filter { it.idBarbero == barberoFiltroResena!!.idBarbero }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(
            Brush.horizontalGradient(
                listOf(AdminAccent, ColorBlanco, ColorRojo, ColorBlanco, AdminAccent)
            )))
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.cargarResenas()
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
                            Text("Reseñas", color = colores.texto,
                                fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            if (barberoFiltroResena != null) {
                                TextButton(onClick = { barberoFiltroResena = null }) {
                                    Icon(Icons.Filled.ArrowBack, null,
                                        tint = AdminAccent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ver todas", color = AdminAccent, fontSize = 12.sp)
                                }
                            }
                        }
                        TextButton(onClick = { viewModel.cargarResenas() }) {
                            Icon(Icons.Filled.Refresh, null,
                                tint = AdminAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Actualizar", color = AdminAccent, fontSize = 12.sp)
                        }
                    }
                }

            if (barberoFiltroResena == null) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = colores.superficie),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = colores.sombra.dp)) {
                        Column(modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Promedio general",
                                color = colores.textoSub, fontSize = 12.sp)
                            Text("%.1f".format(promedioGeneral),
                                color = ColorDorado, fontSize = 36.sp,
                                fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                repeat(5) { i ->
                                    Icon(
                                        if (i < promedioGeneral.toInt()) Icons.Filled.Star
                                        else Icons.Filled.StarOutline,
                                        null, tint = ColorDorado, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text("${uiState.resenas.size} reseñas",
                                color = colores.textoSub, fontSize = 12.sp)
                        }
                    }
                }

                item {
                    Text("Promedio por barbero", color = colores.texto,
                        fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp))
                }

                for (barbero in uiState.barberos) {
                    val resenasBarbero = uiState.resenas.filter {
                        it.idBarbero == barbero.idBarbero
                    }
                    if (resenasBarbero.isNotEmpty()) {
                        val promBarbero = resenasBarbero.map { it.calificacion }.average()
                        item {
                            Card(modifier = Modifier.fillMaxWidth()
                                .clickable { barberoFiltroResena = barbero },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = colores.superficie),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = colores.sombra.dp)) {
                                Row(modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(modifier = Modifier.size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(AdminAccentSoft),
                                        contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Star, null,
                                            tint = AdminAccent, modifier = Modifier.size(20.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(barbero.nombre, color = colores.texto,
                                            fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        Text("${resenasBarbero.size} reseñas",
                                            color = colores.textoSub, fontSize = 12.sp)
                                    }
                                    Text("%.1f".format(promBarbero),
                                        color = ColorDorado,
                                        fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Icon(Icons.Filled.ChevronRight, null,
                                        tint = colores.textoSub, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Text("Reseñas de ${barberoFiltroResena!!.nombre}",
                        color = colores.texto, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            if (uiState.resenas.isEmpty() && !uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.StarOutline, null,
                                tint = colores.textoSub, modifier = Modifier.size(48.dp))
                            Text("No hay reseñas", color = colores.textoSub, fontSize = 15.sp)
                        }
                    }
                }
            } else {
                items(resenasFiltradas) { resena ->
                    val barbero = uiState.barberos.find {
                        it.idBarbero == resena.idBarbero
                    }
                    val cliente = uiState.todosUsuarios.find {
                        it.idUsuario == resena.idUsuario
                    }
                    Card(modifier = Modifier.fillMaxWidth()
                        .clickable { resenaSeleccionada = resena },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = colores.superficie),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = colores.sombra.dp)) {
                        Row(modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AdminAccentSoft),
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Star, null,
                                    tint = AdminAccent, modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(cliente?.nombre ?: "Cliente #${resena.idUsuario}",
                                        color = colores.texto,
                                        fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                        .background(ColorDorado.copy(0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text("${resena.calificacion}★",
                                            color = ColorDorado, fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold)
                                    }
                                }
                                barbero?.let {
                                    Text(it.nombre, color = colores.textoSub,
                                        fontSize = 11.sp)
                                }
                                resena.comentario?.let {
                                    Text(it, color = colores.textoSub,
                                        fontSize = 12.sp, maxLines = 2,
                                        overflow = TextOverflow.Ellipsis)
                                }
                            }
                            IconButton(onClick = { resenaAEliminar = resena },
                                modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.DeleteOutline, null,
                                    tint = ColorError, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
    }

    resenaSeleccionada?.let { resena ->
        val barbero = uiState.barberos.find { it.idBarbero == resena.idBarbero }
        val cliente = uiState.todosUsuarios.find { it.idUsuario == resena.idUsuario }
        val cita = uiState.todasLasCitas.find { it.idCita == resena.idCita }
        val servicio = cita?.let { c ->
            uiState.servicios.find { it.idServicio == c.idServicio }
        }
        AlertDialog(
            onDismissRequest = { resenaSeleccionada = null },
            containerColor   = colores.superficie,
            shape            = RoundedCornerShape(20.dp),
            title = { Text("Detalle de reseña", color = colores.texto,
                fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${resena.calificacion}/5",
                            color = ColorDorado, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        repeat(5) { i ->
                            Icon(
                                if (i < resena.calificacion) Icons.Filled.Star
                                else Icons.Filled.StarOutline,
                                null, tint = ColorDorado, modifier = Modifier.size(14.dp))
                        }
                    }
                    DetalleItem("Cliente", cliente?.nombre ?: "Desconocido", Icons.Filled.Person, colores)
                    DetalleItem("Barbero", barbero?.nombre ?: "Desconocido", Icons.Filled.ContentCut, colores)
                    DetalleItem("Servicio", servicio?.nombre ?: "Desconocido", Icons.Filled.ShoppingBag, colores)
                    resena.comentario?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Comentario:", color = colores.textoSub, fontSize = 11.sp)
                        Text(it, color = colores.texto, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                BarberiaBoton("Cerrar", colorFondo = AdminAccent,
                    onClick = { resenaSeleccionada = null })
            }
        )
    }

    resenaAEliminar?.let { resena ->
        AlertDialog(
            onDismissRequest = { resenaAEliminar = null },
            containerColor   = colores.superficie,
            shape            = RoundedCornerShape(20.dp),
            title = { Text("Eliminar reseña", color = colores.texto,
                fontWeight = FontWeight.Bold) },
            text  = { Text("¿Eliminar esta reseña de ${resena.calificacion}★?",
                color = colores.textoSub, fontSize = 14.sp) },
            confirmButton = {
                BarberiaBoton("Eliminar", colorFondo = ColorError, onClick = {
                    viewModel.eliminarResena(resena.idResena!!)
                    resenaAEliminar = null
                })
            },
            dismissButton = {
                TextButton(onClick = { resenaAEliminar = null }) {
                    Text("Cancelar", color = colores.textoSub) }
            }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// COMPONENTES
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AdminStatCard(
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
private fun SeccionTituloAdmin(texto: String, colores: BarberiaColores) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.width(3.dp).height(18.dp)
            .clip(RoundedCornerShape(2.dp)).background(AdminAccent))
        Text(texto, color = colores.texto, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FiltroChip(
    texto: String, seleccionado: Boolean, color: Color,
    colores: BarberiaColores, onClick: () -> Unit
) {
    Box(modifier = Modifier
        .clip(RoundedCornerShape(20.dp))
        .background(if (seleccionado) color else colores.superficie2)
        .border(1.dp,
            if (seleccionado) color else colores.borde, RoundedCornerShape(20.dp))
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
private fun TarjetaCitaAdmin(
    detalle: CitaConDetalle,
    colores: BarberiaColores,
    onClick: () -> Unit = {},
    onCancelar: () -> Unit
) {
    val colorEstado = when (detalle.cita.estado) {
        EstadoCitaEnum.PENDIENTE     -> ColorDorado
        EstadoCitaEnum.EN_CURSO      -> AdminAccent
        EstadoCitaEnum.FINALIZADA    -> ColorVerde
        EstadoCitaEnum.CANCELADA     -> ColorError
        EstadoCitaEnum.NO_PRESENTADO -> colores.textoSub
        null                         -> colores.textoSub
    }
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(14.dp),
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
                            color = colores.textoSub, fontSize = 12.sp)
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
                        Text("\$${detalle.precio.toInt()}", color = AdminAccent,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
            }
        }
    }
}

@Composable
private fun TarjetaBarberoAdmin(
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
                    AdminAccent.copy(0.15f) else colores.borde),
                contentAlignment = Alignment.Center) {
                Text(barbero.nombre.take(2).uppercase(),
                    color = if (barbero.activo == true) AdminAccent else colores.textoSub,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(barbero.nombre, color = colores.texto,
                    fontWeight = FontWeight.Medium, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(barbero.especialidad ?: "Sin especialidad",
                    color = colores.textoSub, fontSize = 12.sp)
            }
            Switch(
                checked  = barbero.activo ?: false,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor      = Color.White,
                    checkedTrackColor      = AdminAccent,
                    uncheckedThumbColor    = Color.White,
                    uncheckedTrackColor    = colores.borde
                )
            )
            IconButton(onClick = onEditar, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Edit, null,
                    tint = AdminAccent, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onEliminar, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.DeleteOutline, null,
                    tint = ColorError, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun TarjetaServicioAdmin(
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
                .background(ColorVerde.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.ContentCut, null,
                    tint = ColorVerde, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(servicio.nombre, color = colores.texto,
                    fontWeight = FontWeight.Medium, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${servicio.duracionMinutos} min",
                    color = colores.textoSub, fontSize = 12.sp)
            }
            Text("\$${servicio.precio.toInt()}", color = AdminAccent,
                fontWeight = FontWeight.Bold, fontSize = 16.sp)
            IconButton(onClick = onEditar, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Edit, null,
                    tint = AdminAccent, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onEliminar, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.DeleteOutline, null,
                    tint = ColorError, modifier = Modifier.size(18.dp))
            }
        }
    }
}


private fun fechaHoyFormateada(): String {
    val cal = java.util.Calendar.getInstance()
    val dias = listOf("Domingo","Lunes","Martes","Miércoles",
        "Jueves","Viernes","Sábado")
    val meses = listOf("Ene","Feb","Mar","Abr","May","Jun",
        "Jul","Ago","Sep","Oct","Nov","Dic")
    return "${dias[cal.get(java.util.Calendar.DAY_OF_WEEK)-1]} " +
            "${cal.get(java.util.Calendar.DAY_OF_MONTH)} " +
            "${meses[cal.get(java.util.Calendar.MONTH)]} " +
            "${cal.get(java.util.Calendar.YEAR)}"
}

private fun getDiaSemanaFromFecha(fecha: String): DiaSemanaEnum? {
    val partes = fecha.split("-")
    if (partes.size != 3) return null
    return try {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.YEAR, partes[0].toInt())
        cal.set(java.util.Calendar.MONTH, partes[1].toInt() - 1)
        cal.set(java.util.Calendar.DAY_OF_MONTH, partes[2].toInt())
        when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY    -> DiaSemanaEnum.LUNES
            java.util.Calendar.TUESDAY   -> DiaSemanaEnum.MARTES
            java.util.Calendar.WEDNESDAY -> DiaSemanaEnum.MIERCOLES
            java.util.Calendar.THURSDAY  -> DiaSemanaEnum.JUEVES
            java.util.Calendar.FRIDAY    -> DiaSemanaEnum.VIERNES
            java.util.Calendar.SATURDAY  -> DiaSemanaEnum.SABADO
            java.util.Calendar.SUNDAY    -> DiaSemanaEnum.DOMINGO
            else                         -> null
        }
    } catch (_: Exception) { null }
}



