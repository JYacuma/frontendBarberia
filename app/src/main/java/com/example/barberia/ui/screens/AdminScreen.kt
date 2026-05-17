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

// Acento del admin — azul claro diferente al azul del barbero
private val AdminAccent     = ColorAzulClaro
private val AdminAccentSoft = ColorAzulClaro.copy(alpha = 0.15f)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdminScreen(
    apiService: ApiService,
    nombre: String,
    onLogout: () -> Unit
) {
    val colores       = LocalBarberiaColores.current
    val sistemaOscuro = isSystemInDarkTheme()

    val viewModel: AdminViewModel = viewModel(
        factory = AdminViewModel.factory(apiService)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pagerState    = rememberPagerState(pageCount = { 4 })
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
                    onLogout, colores, sistemaOscuro)
                1 -> AdminCitasTab(uiState, viewModel, colores)
                2 -> AdminBarberosTab(uiState, viewModel, colores)
                3 -> AdminServiciosTab(uiState, viewModel, colores)
            }
        }
    }
}

// ── Bottom Bar — 4 tabs, acento azul claro ────────────────────────────────────
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
            Triple("Servicios", Icons.Filled.ContentCut,    3)
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
// TAB 1 — INICIO (Panel de control)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun AdminInicioTab(
    nombre: String,
    uiState: com.example.barberia.viewmodel.AdminUiState,
    viewModel: AdminViewModel,
    onLogout: () -> Unit,
    colores: BarberiaColores,
    sistemaOscuro: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colores.fondo)
    ) {
        // ── Header azul claro ─────────────────────────────────────────────
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
                    start = 20.dp, end = 20.dp, top = 52.dp, bottom = 20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape)
                                    .background(AdminAccent))
                                Text("ADMINISTRADOR", color = AdminAccent,
                                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Panel de control", color = colores.texto,
                                fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text(fechaHoyFormateada(), color = colores.textoSub, fontSize = 13.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                                .background(AdminAccent),
                                contentAlignment = Alignment.Center) {
                                Text("AD", color = Color.White,
                                    fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            IconButton(onClick = { TemaManager.toggleModo(sistemaOscuro) }) {
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
            // Stats principales — 2x2
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AdminStatCard(
                            numero = uiState.citasHoy.size.toString(),
                            label  = "Citas hoy",
                            color  = AdminAccent,
                            colores = colores,
                            modifier = Modifier.weight(1f)
                        )
                        AdminStatCard(
                            numero = uiState.barberos.count {
                                it.activo == true }.toString(),
                            label  = "Barberos activos",
                            color  = ColorVerde,
                            colores = colores,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AdminStatCard(
                            numero = uiState.citasHoy.count {
                                it.estado == EstadoCitaEnum.CANCELADA }.toString(),
                            label  = "Cancelaciones hoy",
                            color  = ColorError,
                            colores = colores,
                            modifier = Modifier.weight(1f)
                        )
                        AdminStatCard(
                            numero = uiState.servicios.size.toString(),
                            label  = "Servicios",
                            color  = ColorDorado,
                            colores = colores,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Acciones rápidas
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SeccionTituloAdmin("Acciones rápidas", colores)
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccionRapidaCard(
                            icono     = Icons.Filled.People,
                            titulo    = "Gestionar barberos",
                            subtitulo = "${uiState.barberos.size} barberos · " +
                                    "${uiState.barberos.count { it.activo == true }} activos",
                            color     = ColorAzul,
                            colores   = colores
                        )
                        AccionRapidaCard(
                            icono     = Icons.Filled.ContentCut,
                            titulo    = "Gestionar servicios",
                            subtitulo = "${uiState.servicios.size} servicios activos",
                            color     = ColorVerde,
                            colores   = colores
                        )
                        AccionRapidaCard(
                            icono     = Icons.Filled.CalendarMonth,
                            titulo    = "Todas las citas",
                            subtitulo = "${uiState.todasLasCitas.size} citas en total · " +
                                    "${uiState.citasHoy.size} hoy",
                            color     = ColorRojo,
                            colores   = colores
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 2 — CITAS (todas)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun AdminCitasTab(
    uiState: com.example.barberia.viewmodel.AdminUiState,
    viewModel: AdminViewModel,
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
            Text("${uiState.todasLasCitas.size} citas en total",
                color = colores.textoSub, fontSize = 13.sp)
        }

        // Filtros por estado
        item {
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FiltroChip("Todas", filtroEstado == null, AdminAccent, colores) {
                        filtroEstado = null }
                }
                items(EstadoCitaEnum.entries.toTypedArray()) { estado ->
                    FiltroChip(
                        texto = when (estado) {
                            EstadoCitaEnum.PENDIENTE     -> "Pendiente"
                            EstadoCitaEnum.EN_CURSO      -> "En curso"
                            EstadoCitaEnum.FINALIZADA    -> "Finalizada"
                            EstadoCitaEnum.CANCELADA     -> "Cancelada"
                            EstadoCitaEnum.NO_PRESENTADO -> "No presentó"
                        },
                        seleccionado = filtroEstado == estado,
                        color        = AdminAccent,
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
            items(citasFiltradas) { cita ->
                TarjetaCitaAdmin(cita, colores,
                    onCancelar = {
                        if (cita.estado == EstadoCitaEnum.PENDIENTE ||
                            cita.estado == EstadoCitaEnum.EN_CURSO)
                            citaACancelar = cita
                    }
                )
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    // Dialog cancelar cita
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
// TAB 3 — BARBEROS
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun AdminBarberosTab(
    uiState: com.example.barberia.viewmodel.AdminUiState,
    viewModel: AdminViewModel,
    colores: BarberiaColores
) {
    var mostrarFormulario by remember { mutableStateOf(false) }
    var nombreBarbero     by remember { mutableStateOf("") }
    var especialidad      by remember { mutableStateOf("") }
    var telefono          by remember { mutableStateOf("") }
    var barberoAEliminar  by remember { mutableStateOf<BarberoDTO?>(null) }

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
                // Botón agregar con animación
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

        // Formulario crear barbero — aparece/desaparece con animación
        item {
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
                            BarberiaBoton(
                                texto      = "Crear barbero",
                                icono      = Icons.Filled.PersonAdd,
                                isLoading  = uiState.isLoading,
                                colorFondo = if (nombreBarbero.isNotBlank())
                                    AdminAccent else colores.borde,
                                onClick    = {
                                    if (nombreBarbero.isNotBlank()) {
                                        viewModel.crearBarbero(
                                            nombreBarbero, especialidad, telefono)
                                        nombreBarbero = ""; especialidad = ""; telefono = ""
                                        mostrarFormulario = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Lista de barberos
        items(uiState.barberos) { barbero ->
            TarjetaBarberoAdmin(
                barbero   = barbero,
                colores   = colores,
                onToggle  = { viewModel.toggleActivoBarbero(barbero) },
                onEliminar = { barberoAEliminar = barbero }
            )
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    // Dialog eliminar barbero
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
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 4 — SERVICIOS
// ══════════════════════════════════════════════════════════════════════════════
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

        // Formulario crear servicio
        item {
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

        // Lista de servicios
        items(uiState.servicios) { servicio ->
            TarjetaServicioAdmin(
                servicio   = servicio,
                colores    = colores,
                onEliminar = { servicioAEliminar = servicio }
            )
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    // Dialog eliminar servicio
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
}

// ══════════════════════════════════════════════════════════════════════════════
// COMPONENTES
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AdminStatCard(
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
private fun SeccionTituloAdmin(texto: String, colores: BarberiaColores) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.width(3.dp).height(18.dp)
            .clip(RoundedCornerShape(2.dp)).background(AdminAccent))
        Text(texto, color = colores.texto, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AccionRapidaCard(
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
                Text(titulo, color = colores.texto, fontWeight = FontWeight.Medium,
                    fontSize = 14.sp)
                Text(subtitulo, color = colores.textoSub, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Filled.ChevronRight, null,
                tint = colores.textoSub, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun TarjetaCitaAdmin(
    cita: CitaDTO, colores: BarberiaColores, onCancelar: () -> Unit
) {
    val colorEstado = when (cita.estado) {
        EstadoCitaEnum.PENDIENTE     -> ColorDorado
        EstadoCitaEnum.EN_CURSO      -> AdminAccent
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
private fun TarjetaBarberoAdmin(
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
                    AdminAccent.copy(0.15f) else colores.borde),
                contentAlignment = Alignment.Center) {
                Text(barbero.nombre.take(2).uppercase(),
                    color = if (barbero.activo == true) AdminAccent else colores.textoSub,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(barbero.nombre, color = colores.texto,
                    fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(barbero.especialidad ?: "Sin especialidad",
                    color = colores.textoSub, fontSize = 12.sp)
            }
            // Switch activo/inactivo
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
            IconButton(onClick = onEliminar, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.DeleteOutline, null,
                    tint = ColorError, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun TarjetaServicioAdmin(
    servicio: ServicioDTO, colores: BarberiaColores, onEliminar: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
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
                    fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text("${servicio.duracionMinutos} min",
                    color = colores.textoSub, fontSize = 12.sp)
            }
            Text("\$${servicio.precio.toInt()}", color = AdminAccent,
                fontWeight = FontWeight.Bold, fontSize = 16.sp)
            IconButton(onClick = onEliminar, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.DeleteOutline, null,
                    tint = ColorError, modifier = Modifier.size(18.dp))
            }
        }
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

// Fecha de hoy formateada para el header
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

