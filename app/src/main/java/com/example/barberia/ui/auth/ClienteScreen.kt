package com.example.barberia.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.barberia.model.*
import com.example.barberia.network.ApiService
import com.example.barberia.ui.theme.*
import com.example.barberia.viewmodel.ClienteViewModel
import com.example.barberia.viewmodel.ClienteUiState

import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import java.text.Normalizer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Create


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ClienteScreen(
    apiService: ApiService,
    idUsuario: Long,
    nombre: String,
    onLogout: () -> Unit,
    onNavigateToNotificaciones: () -> Unit = {}
)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pagerState    = rememberPagerState(pageCount = { 3 })
    val scope         = rememberCoroutineScope()
    val snackbarState = remember { SnackbarHostState() }

    val initials = remember(nombre) {
        val ascii = Normalizer.normalize(nombre, Normalizer.Form.NFD)
            .replace(Regex("[^\\p{ASCII}]"), "")
        ascii.split(" ").take(2).joinToString("") { it.first().uppercase() }
    }

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
                Snackbar(
                    snackbarData   = data,
                    containerColor = colores.superficie,
                    contentColor   = colores.texto,
                    actionColor    = ColorRojo,
                    shape          = RoundedCornerShape(12.dp)
                )
            }
        },
        topBar = {
            Surface(color = colores.fondo) {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(
                        Brush.horizontalGradient(
                            listOf(ColorRojo, ColorBlanco, ColorRojo, ColorBlanco, ColorRojo)
                        )))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                            .background(ColorRojo.copy(0.15f))
                            .border(1.5.dp, ColorRojo, CircleShape),
                            contentAlignment = Alignment.Center) {
                            Text(initials, color = ColorRojo,
                                fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(nombre, color = colores.texto,
                                fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Cliente", color = colores.textoSub, fontSize = 12.sp)
                        }
                        Spacer(Modifier.weight(1f))
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
                        BadgedBox(badge = {
                            if (uiState.notificaciones.isNotEmpty()) {
                                Badge(containerColor = ColorRojo) {
                                    Text("${uiState.notificaciones.size}",
                                        color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }) {
                            IconButton(onClick = onNavigateToNotificaciones) {
                                Icon(Icons.Filled.Notifications, "Notificaciones",
                                    tint = colores.texto, modifier = Modifier.size(22.dp))
                            }
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión",
                                tint = colores.textoSub, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = colores.superficie,
                tonalElevation = 0.dp,
                modifier = if (!colores.esModoOscuro) Modifier.shadow(4.dp)
                else Modifier.border(1.dp, colores.borde,
                    RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp))
            ) {
                listOf(
                    Triple("Inicio",   Icons.Filled.Home,          0),
                    Triple("Agendar",  Icons.Filled.CalendarMonth, 1),
                    Triple("Mis Citas",Icons.Filled.List,           2)
                ).forEach { (label, icon, index) ->
                    val activo = pagerState.currentPage == index
                    NavigationBarItem(
                        selected = activo,
                        onClick  = { scope.launch { pagerState.animateScrollToPage(index) } },
                        icon = { Icon(icon, label,
                            tint = if (activo) ColorRojo else colores.textoSub,
                            modifier = Modifier.size(22.dp)) },
                        label = { Text(label,
                            color = if (activo) ColorRojo else colores.textoSub,
                            fontSize = 10.sp,
                            fontWeight = if (activo) FontWeight.SemiBold
                            else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = ColorRojo.copy(alpha = 0.15f))
                    )
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state             = pagerState,
            modifier          = Modifier.padding(padding).fillMaxSize(),
            userScrollEnabled = true
        ) { pagina ->
            when (pagina) {
                0 -> InicioTab(nombre, uiState, viewModel, colores, pagerState, scope)
                1 -> AgendarTab(uiState, viewModel, colores)
                2 -> MisCitasTab(uiState, viewModel, colores)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 0 — INICIO
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun InicioTab(
    nombre: String,
    uiState: ClienteUiState,
    viewModel: ClienteViewModel,
    colores: BarberiaColores,
    pagerState: androidx.compose.foundation.pager.PagerState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.cargarDatosIniciales() }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(colores.fondo)
        ) {
            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ColorRojo,
                            strokeWidth = 2.5.dp, modifier = Modifier.size(36.dp))
                    }
                }
            } else {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Barberos populares
                item {
                    SeccionTituloCliente("Nuestros Barberos", colores,
                        modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        items(uiState.popularBarberos.ifEmpty { uiState.barberos }) { barbero ->
                            Card(
                                modifier = Modifier.width(110.dp)
                                    .shadow(4.dp, RoundedCornerShape(14.dp))
                                    .clickable {
                                        scope.launch { pagerState.animateScrollToPage(1) }
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = colores.superficie)
                            ) {
                                Column(modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                                        .background(ColorRojo.copy(0.15f)),
                                        contentAlignment = Alignment.Center) {
                                        Text(barbero.nombre.take(2).uppercase(),
                                            color = ColorRojo,
                                            fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(barbero.nombre, color = colores.texto,
                                        fontSize = 12.sp, textAlign = TextAlign.Center,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    barbero.especialidad?.let {
                                        Text(it, color = colores.textoSub,
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Servicios populares (ordenados por precio ascendente)
                item {
                    SeccionTituloCliente("Nuestros Servicios", colores,
                        modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                }

                val serviciosOrdenados = uiState.popularServicios.ifEmpty {
                    uiState.servicios.sortedBy { it.precio }
                }
                items(serviciosOrdenados) { servicio ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                                .shadow(4.dp, RoundedCornerShape(14.dp))
                                .clickable {
                                    scope.launch { pagerState.animateScrollToPage(1) }
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = colores.superficie)
                        ) {
                            Row(modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)) {
                                    Box(modifier = Modifier.size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ColorRojo.copy(0.1f)),
                                        contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.ContentCut, null,
                                            tint = ColorRojo, modifier = Modifier.size(20.dp))
                                    }
                                    Column {
                                        Text(servicio.nombre, color = colores.texto,
                                            fontWeight = FontWeight.Medium, fontSize = 14.sp,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${servicio.duracionMinutos} min",
                                            color = colores.textoSub, fontSize = 12.sp)
                                    }
                                }
                                Text("\$${servicio.precio.toInt()}", color = ColorRojo,
                                    fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 1 — AGENDAR
// ══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgendarTab(
    uiState: ClienteUiState,
    viewModel: ClienteViewModel,
    colores: BarberiaColores
) {
    var pasoAgenda            by remember { mutableIntStateOf(0) }
    var barberoSeleccionado   by remember { mutableStateOf<BarberoDTO?>(null) }
    var servicioSeleccionado  by remember { mutableStateOf<ServicioDTO?>(null) }
    var fechaSeleccionada     by remember { mutableStateOf("") }
    var horaSeleccionada      by remember { mutableStateOf("") }
    var mostrarCalendario     by remember { mutableStateOf(false) }
    var mostrarDialogoBarbero by remember { mutableStateOf<BarberoDTO?>(null) }
    var mostrarExito          by remember { mutableStateOf(false) }
    var filtroEspecialidad    by remember { mutableStateOf<String?>(null) }
    var filtroServicioIdx     by remember { mutableIntStateOf(0) }

    val especialidades = remember(uiState.barberos) {
        uiState.barberos.mapNotNull { it.especialidad }.distinct().sorted()
    }
    val barberosFiltrados = remember(filtroEspecialidad, uiState.barberos) {
        if (filtroEspecialidad.isNullOrBlank()) uiState.barberos
        else uiState.barberos.filter { it.especialidad == filtroEspecialidad }
    }
    val serviciosFiltrados = remember(filtroServicioIdx, uiState.servicios) {
        when (filtroServicioIdx) {
            1 -> uiState.servicios.sortedBy { it.precio }
            2 -> uiState.servicios.sortedByDescending { it.precio }
            else -> uiState.servicios
        }
    }

    val hoy = remember {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val dentroDe8Dias = remember { hoy + 8L * 24 * 60 * 60 * 1000 }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = null,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis in hoy..dentroDe8Dias
            }
            override fun isSelectableYear(year: Int): Boolean = true
        }
    )

    fun millisAFecha(millis: Long): String {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = millis + timeZone.getOffset(millis)
        }
        return "%d-%02d-%02d".format(
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    if (mostrarCalendario) {
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        fechaSeleccionada = millisAFecha(it)
                        horaSeleccionada = ""
                        pasoAgenda = 3
                        barberoSeleccionado?.idBarbero?.let { id ->
                            viewModel.cargarDisponibilidad(id, fechaSeleccionada)
                        }
                    }
                    mostrarCalendario = false
                }) { Text("Confirmar", color = ColorRojo, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalendario = false }) {
                    Text("Cancelar", color = colores.textoSub) }
            },
            colors = DatePickerDefaults.colors(
                containerColor            = colores.superficie,
                titleContentColor         = colores.texto,
                headlineContentColor      = ColorRojo,
                weekdayContentColor       = colores.textoSub,
                dayContentColor           = colores.texto,
                selectedDayContainerColor = ColorRojo,
                selectedDayContentColor   = Color.White,
                todayDateBorderColor      = ColorRojo,
                todayContentColor         = ColorRojo
            )
        ) { DatePicker(state = datePickerState,
            colors = DatePickerDefaults.colors(
                containerColor            = colores.superficie,
                titleContentColor         = colores.texto,
                headlineContentColor      = ColorRojo,
                weekdayContentColor       = colores.textoSub,
                dayContentColor           = colores.texto,
                selectedDayContainerColor = ColorRojo,
                selectedDayContentColor   = Color.White,
                todayDateBorderColor      = ColorRojo,
                todayContentColor         = ColorRojo
            )) }
    }

    // Dialog perfil barbero
    mostrarDialogoBarbero?.let { barbero ->
        val bInitials = remember(barbero.nombre) {
            val ascii = Normalizer.normalize(barbero.nombre, Normalizer.Form.NFD)
                .replace(Regex("[^\\p{ASCII}]"), "")
            ascii.split(" ").take(2).joinToString("") { it.first().uppercase() }
        }
        AlertDialog(
            onDismissRequest = { mostrarDialogoBarbero = null },
            containerColor   = colores.superficie,
            shape            = RoundedCornerShape(20.dp),
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.size(64.dp).clip(CircleShape)
                        .background(ColorRojo.copy(0.15f))
                        .border(2.dp, ColorRojo, CircleShape),
                        contentAlignment = Alignment.Center) {
                        Text(bInitials, color = ColorRojo,
                            fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(barbero.nombre, color = colores.texto,
                        fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()) {
                    barbero.especialidad?.let {
                        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                            .background(ColorRojo.copy(0.1f))
                            .padding(horizontal = 16.dp, vertical = 6.dp)) {
                            Text(it, color = ColorRojo, fontSize = 13.sp,
                                fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                BarberiaBoton("Confirmar selección", onClick = {
                    barberoSeleccionado = barbero
                    horaSeleccionada = ""
                    pasoAgenda = 1
                    viewModel.limpiarDisponibilidad()
                    if (fechaSeleccionada.length == 10)
                        viewModel.cargarDisponibilidad(
                            barbero.idBarbero!!, fechaSeleccionada)
                    mostrarDialogoBarbero = null
                })
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoBarbero = null }) {
                    Text("Cancelar", color = colores.textoSub)
                }
            }
        )
    }

    // Dialog exito
    if (mostrarExito) {
        AlertDialog(
            onDismissRequest = { mostrarExito = false },
            containerColor   = colores.superficie,
            shape            = RoundedCornerShape(20.dp),
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.CheckCircle, null,
                        tint = ColorVerde, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("¡Cita agendada!", color = colores.texto,
                        fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    barberoSeleccionado?.let {
                        Text("Barbero: ${it.nombre}",
                            color = colores.texto, fontSize = 14.sp)
                    }
                    servicioSeleccionado?.let {
                        Text("Servicio: ${it.nombre} - \$${it.precio.toInt()}",
                            color = colores.texto, fontSize = 14.sp)
                    }
                    if (fechaSeleccionada.isNotBlank())
                        Text("Fecha: $fechaSeleccionada",
                            color = colores.texto, fontSize = 14.sp)
                    if (horaSeleccionada.isNotBlank())
                        Text("Hora: ${horaSeleccionada.take(5)}",
                            color = colores.texto, fontSize = 14.sp)
                }
            },
            confirmButton = {
                BarberiaBoton("Aceptar", onClick = {
                    mostrarExito = false
                    barberoSeleccionado = null
                    servicioSeleccionado = null
                    fechaSeleccionada = ""
                    horaSeleccionada = ""
                    pasoAgenda = 0
                })
            }
        )
    }

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.cargarDatosIniciales() }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(colores.fondo)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Agendar Cita", color = colores.texto,
                    fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Sigue los pasos para reservar", color = colores.textoSub, fontSize = 13.sp)
            }

            // Paso 1 — Barbero con filtro por especialidad
            item {
                PasoTituloCliente("1. Elige tu barbero", colores)
                Spacer(modifier = Modifier.height(8.dp))

                // Chips de filtro por especialidad
                if (especialidades.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = filtroEspecialidad == null,
                                onClick = { filtroEspecialidad = null },
                                label = { Text("Todos", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ColorRojo,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        items(especialidades) { esp ->
                            FilterChip(
                                selected = filtroEspecialidad == esp,
                                onClick = { filtroEspecialidad = esp },
                                label = { Text(esp, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ColorRojo,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(barberosFiltrados) { barbero ->
                        Card(
                            modifier = Modifier.width(110.dp)
                                .shadow(4.dp, RoundedCornerShape(14.dp))
                                .clickable { mostrarDialogoBarbero = barbero },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = colores.superficie)
                        ) {
                            Column(modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                                    .background(ColorRojo.copy(0.15f)),
                                    contentAlignment = Alignment.Center) {
                                    Text(barbero.nombre.take(2).uppercase(),
                                        color = ColorRojo,
                                        fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(barbero.nombre, color = colores.texto,
                                    fontSize = 12.sp, textAlign = TextAlign.Center,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                barbero.especialidad?.let {
                                    Text(it, color = colores.textoSub,
                                        fontSize = 10.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }

            // Paso 2 — Servicio con filtros
            if (barberoSeleccionado != null) {
                item {
                    PasoTituloCliente("2. Elige el servicio", colores)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = filtroServicioIdx == 0,
                                onClick = { filtroServicioIdx = 0 },
                                label = { Text("Todos", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ColorRojo,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = filtroServicioIdx == 1,
                                onClick = { filtroServicioIdx = 1 },
                                label = { Text("Populares", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ColorRojo,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = filtroServicioIdx == 2,
                                onClick = { filtroServicioIdx = 2 },
                                label = { Text("Precio \u2191", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ColorRojo,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        serviciosFiltrados.forEach { servicio ->
                            val sel = servicioSeleccionado?.idServicio == servicio.idServicio
                            Card(
                                modifier = Modifier.fillMaxWidth()
                                    .shadow(4.dp, RoundedCornerShape(14.dp))
                                    .clickable {
                                        servicioSeleccionado = servicio
                                        pasoAgenda = 2
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (sel) ColorRojo else colores.superficie)
                            ) {
                                Row(modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(servicio.nombre,
                                            color = if (sel) Color.White else colores.texto,
                                            fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        Text("${servicio.duracionMinutos} min",
                                            color = if (sel) Color.White.copy(0.7f)
                                            else colores.textoSub, fontSize = 12.sp)
                                    }
                                    Text("\$${servicio.precio.toInt()}",
                                        color = if (sel) Color.White else ColorRojo,
                                        fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Paso 3 — Fecha
            if (servicioSeleccionado != null) {
                item {
                    PasoTituloCliente("3. Elige la fecha", colores)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(colores.superficie)
                        .clickable { mostrarCalendario = true }
                        .padding(horizontal = 16.dp, vertical = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Filled.CalendarMonth, null,
                                tint = ColorRojo, modifier = Modifier.size(22.dp))
                            Text(
                                text = if (fechaSeleccionada.isNotBlank()) fechaSeleccionada
                                else "Toca para abrir el calendario",
                                color = if (fechaSeleccionada.isNotBlank()) colores.texto
                                else colores.textoSub,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Paso 4 — Hora
            if (fechaSeleccionada.isNotBlank()) {
                val fechaHoy = java.util.Calendar.getInstance().let {
                    "%d-%02d-%02d".format(it.get(java.util.Calendar.YEAR),
                        it.get(java.util.Calendar.MONTH) + 1,
                        it.get(java.util.Calendar.DAY_OF_MONTH))
                }
                val horaActual = java.util.Calendar.getInstance().let {
                    "%02d:%02d".format(it.get(java.util.Calendar.HOUR_OF_DAY),
                        it.get(java.util.Calendar.MINUTE))
                }
                val horasFiltradas = if (fechaSeleccionada == fechaHoy) {
                    uiState.horasDisponibles.filter { it.take(5) > horaActual }
                } else {
                    uiState.horasDisponibles
                }

                if (horasFiltradas.isEmpty() && uiState.horasDisponibles.isNotEmpty()) {
                    item {
                        PasoTituloCliente("4. Elige la hora", colores)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("No hay horas disponibles para hoy después de las $horaActual",
                            color = colores.textoSub, fontSize = 13.sp)
                    }
                }

                if (horasFiltradas.isNotEmpty()) {
                    item {
                        PasoTituloCliente("4. Elige la hora", colores)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(horasFiltradas) { hora ->
                                val horaCorta = hora.take(5)
                                val sel       = horaSeleccionada == hora
                                Box(modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) ColorRojo else colores.superficie)
                                    .border(1.dp,
                                        if (sel) ColorRojo else colores.borde,
                                        RoundedCornerShape(10.dp))
                                    .clickable {
                                        horaSeleccionada = hora
                                        pasoAgenda = 4
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)) {
                                    Text(horaCorta,
                                        color = if (sel) Color.White else colores.texto,
                                        fontWeight = if (sel) FontWeight.Bold
                                        else FontWeight.Normal,
                                        fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Botón confirmar
            item {
                val listo = barberoSeleccionado != null &&
                        servicioSeleccionado != null &&
                        fechaSeleccionada.length == 10 &&
                        horaSeleccionada.isNotBlank()

                BarberiaBoton(
                    texto      = "Confirmar Cita",
                    icono      = Icons.Filled.CheckCircle,
                    isLoading  = uiState.isLoading,
                    colorFondo = if (listo) ColorRojo else colores.borde,
                    onClick    = {
                        if (listo) {
                            viewModel.agendarCita(
                                idBarbero  = barberoSeleccionado!!.idBarbero!!,
                                idServicio = servicioSeleccionado!!.idServicio!!,
                                fecha      = fechaSeleccionada,
                                horaInicio = horaSeleccionada
                            )
                            mostrarExito = true
                        }
                    }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 2 — MIS CITAS
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun MisCitasTab(
    uiState: ClienteUiState,
    viewModel: ClienteViewModel,
    colores: BarberiaColores
) {
    var filtroEstado by remember { mutableStateOf<String?>(null) }
    var citaParaCancelar by remember { mutableStateOf<CitaConDetalle?>(null) }
    var motivoCancelacion by remember { mutableStateOf("") }
    var mostrarCancelada by remember { mutableStateOf(false) }
    var citaParaResena by remember { mutableStateOf<CitaDTO?>(null) }
    var calificacion   by remember { mutableStateOf(5) }
    var comentario     by remember { mutableStateOf("") }

    val motivos = listOf(
        "Cambié de planes",
        "Encontré otro barbero",
        "Emergencia personal",
        "Me equivoqué de fecha",
        "Otro"
    )

    val citasFiltradas = remember(filtroEstado, uiState.citasConDetalles) {
        when (filtroEstado) {
            "Pendientes"  -> uiState.citasConDetalles.filter {
                it.cita.estado == EstadoCitaEnum.PENDIENTE }
            "Finalizadas" -> uiState.citasConDetalles.filter {
                it.cita.estado == EstadoCitaEnum.FINALIZADA }
            "Canceladas"  -> uiState.citasConDetalles.filter {
                it.cita.estado == EstadoCitaEnum.CANCELADA }
            else          -> uiState.citasConDetalles
        }
    }

    // Dialog cancelar con motivo
    citaParaCancelar?.let { detalle ->
        AlertDialog(
            onDismissRequest = {
                citaParaCancelar = null
                motivoCancelacion = ""
            },
            containerColor   = colores.superficie,
            shape            = RoundedCornerShape(20.dp),
            title = {
                Text("Cancelar cita", color = colores.texto,
                    fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Selecciona un motivo:",
                        color = colores.textoSub, fontSize = 13.sp)
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        motivos.forEach { motivo ->
                            FilterChip(
                                selected = motivoCancelacion == motivo,
                                onClick = { motivoCancelacion = motivo },
                                label = { Text(motivo, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ColorRojo,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                BarberiaBoton("Confirmar cancelación", onClick = {
                    if (motivoCancelacion.isNotBlank()) {
                        viewModel.cancelarCitaConMotivo(
                            detalle.cita.idCita!!, motivoCancelacion)
                        citaParaCancelar = null
                        motivoCancelacion = ""
                        mostrarCancelada = true
                    }
                })
            },
            dismissButton = {
                TextButton(onClick = {
                    citaParaCancelar = null
                    motivoCancelacion = ""
                }) {
                    Text("Volver", color = colores.textoSub)
                }
            }
        )
    }

    // Dialog cita cancelada
    if (mostrarCancelada) {
        AlertDialog(
            onDismissRequest = { mostrarCancelada = false },
            containerColor   = colores.superficie,
            shape            = RoundedCornerShape(20.dp),
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Cancel, null,
                        tint = ColorError, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Cita cancelada", color = colores.texto,
                        fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Text("Tu cita ha sido cancelada correctamente.",
                    color = colores.textoSub, fontSize = 14.sp)
            },
            confirmButton = {
                BarberiaBoton("Aceptar", onClick = { mostrarCancelada = false })
            }
        )
    }

    // Dialog reseña
    citaParaResena?.let { cita ->
        AlertDialog(
            onDismissRequest = { citaParaResena = null },
            containerColor   = colores.superficie,
            shape            = RoundedCornerShape(20.dp),
            title = { Text("Dejar Reseña", color = colores.texto,
                fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Calificación", color = colores.textoSub, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { estrella ->
                            Icon(
                                imageVector = if (estrella <= calificacion)
                                    Icons.Filled.Star else Icons.Filled.StarOutline,
                                contentDescription = null,
                                tint = ColorDorado,
                                modifier = Modifier.size(32.dp).clickable {
                                    calificacion = estrella }
                            )
                        }
                    }
                    BarberiaTextField(comentario, { comentario = it },
                        "Comentario (opcional)",
                        Icons.Filled.Comment, ColorRojo, colores)
                }
            },
            confirmButton = {
                BarberiaBoton("Enviar", colorFondo = ColorRojo, onClick = {
                    viewModel.enviarResena(cita.idCita!!, cita.idBarbero,
                        calificacion, comentario)
                    citaParaResena = null; comentario = ""; calificacion = 5
                })
            },
            dismissButton = {
                TextButton(onClick = { citaParaResena = null }) {
                    Text("Cancelar", color = colores.textoSub) }
            }
        )
    }

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.cargarDatosIniciales() }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(colores.fondo)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Mis Citas", color = colores.texto,
                    fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                // Chips de filtro
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val opciones = listOf("Todas" to null,
                        "Pendientes" to "Pendientes",
                        "Finalizadas" to "Finalizadas",
                        "Canceladas" to "Canceladas")
                    items(opciones) { (label, valor) ->
                        FilterChip(
                            selected = filtroEstado == valor,
                            onClick = { filtroEstado = valor },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ColorRojo,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ColorRojo,
                            modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
                    }
                }
            } else if (citasFiltradas.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.CalendarMonth, null,
                                tint = colores.textoSub, modifier = Modifier.size(48.dp))
                            Text("No tienes citas aún",
                                color = colores.textoSub, fontSize = 15.sp)
                        }
                    }
                }
            } else {
                items(citasFiltradas, key = { it.cita.idCita ?: 0 }) { detalle ->
                    TarjetaCitaCliente(
                        detalle    = detalle,
                        colores    = colores,
                        onCancelar = {
                            if (detalle.cita.estado == EstadoCitaEnum.PENDIENTE) {
                                citaParaCancelar = detalle
                            }
                        },
                        onResena   = {
                            if (detalle.cita.estado == EstadoCitaEnum.FINALIZADA) {
                                citaParaResena = detalle.cita
                            }
                        }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// COMPONENTES
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun BarberiaBoton(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    habilitado: Boolean = true,
    icono: ImageVector? = null,
    colorFondo: Color = ColorRojo
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    Button(
        onClick    = onClick,
        enabled    = !isLoading && habilitado,
        modifier   = Modifier.fillMaxWidth().height(52.dp)
            .scale(scale)
            .shadow(4.dp, RoundedCornerShape(14.dp)),
        shape      = RoundedCornerShape(14.dp),
        colors     = ButtonDefaults.buttonColors(
            containerColor         = colorFondo,
            disabledContainerColor = colorFondo.copy(alpha = 0.5f)
        ),
        interactionSource = interactionSource
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White,
                modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
        } else {
            icono?.let {
                Icon(it, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(texto, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun BarberiaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    accentColor: Color = ColorRojo,
    colores: BarberiaColores,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value       = value,
        onValueChange = onValueChange,
        label       = { Text(label, color = colores.textoSub, fontSize = 13.sp) },
        leadingIcon = {
            Icon(leadingIcon, null, tint = accentColor,
                modifier = Modifier.size(20.dp))
        },
        trailingIcon = if (isPassword) {{
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    if (passwordVisible) Icons.Filled.Visibility
                    else Icons.Filled.VisibilityOff,
                    null, tint = colores.textoSub, modifier = Modifier.size(20.dp))
            }
        }} else null,
        visualTransformation = if (isPassword && !passwordVisible)
            androidx.compose.ui.text.input.PasswordVisualTransformation()
        else
            androidx.compose.ui.text.input.VisualTransformation.None,
        singleLine      = true,
        modifier        = modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = accentColor,
            unfocusedBorderColor    = colores.borde,
            focusedTextColor        = colores.texto,
            unfocusedTextColor      = colores.texto,
            cursorColor             = accentColor,
            focusedContainerColor   = colores.superficie,
            unfocusedContainerColor = colores.superficie
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun CampoSoloLectura(
    label: String, valor: String,
    icono: ImageVector,
    colores: BarberiaColores
) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
            .background(ColorRojo.copy(0.1f)),
            contentAlignment = Alignment.Center) {
            Icon(icono, null, tint = ColorRojo, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(label, color = colores.textoSub, fontSize = 11.sp, letterSpacing = 0.5.sp)
            Text(valor, color = colores.texto, fontSize = 14.sp,
                fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun StatPerfilCliente(
    numero: String, label: String, color: Color,
    colores: BarberiaColores, modifier: Modifier = Modifier
) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(0.1f))) {
        Column(modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(numero, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, color = colores.textoSub, fontSize = 10.sp,
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TarjetaCitaCliente(
    detalle: CitaConDetalle,
    colores: BarberiaColores,
    onCancelar: () -> Unit,
    onResena: () -> Unit
) {
    val cita = detalle.cita
    val colorEstado = when (cita.estado) {
        EstadoCitaEnum.PENDIENTE     -> ColorDorado
        EstadoCitaEnum.EN_CURSO      -> ColorAzulClaro
        EstadoCitaEnum.FINALIZADA    -> ColorVerde
        EstadoCitaEnum.CANCELADA     -> ColorError
        EstadoCitaEnum.NO_PRESENTADO -> colores.textoSub
        null                         -> colores.textoSub
    }
    val textoEstado = when (cita.estado) {
        EstadoCitaEnum.PENDIENTE     -> "Pendiente"
        EstadoCitaEnum.EN_CURSO      -> "En curso"
        EstadoCitaEnum.FINALIZADA    -> "Finalizada"
        EstadoCitaEnum.CANCELADA     -> "Cancelada"
        EstadoCitaEnum.NO_PRESENTADO -> "No presentó"
        null -> ""
    }
    Card(
        modifier = Modifier.fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colores.superficie)
    ) {
        Column {
            Row {
                Box(modifier = Modifier.width(4.dp).height(100.dp)
                    .background(colorEstado))
                Column(modifier = Modifier.weight(1f)
                    .padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top) {
                        Column {
                            Text("Cita #${cita.idCita}", color = colores.texto,
                                fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${cita.fecha} · ${cita.horaInicio?.take(5)}",
                                color = colores.textoSub, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(detalle.barberoNombre, color = colores.texto,
                                fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text(detalle.servicioNombre, color = colores.textoSub,
                                fontSize = 11.sp)
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(colorEstado.copy(0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text(textoEstado, color = colorEstado, fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            if (cita.estado == EstadoCitaEnum.PENDIENTE ||
                cita.estado == EstadoCitaEnum.FINALIZADA) {
                HorizontalDivider(color = colores.borde)
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    if (cita.estado == EstadoCitaEnum.PENDIENTE) {
                        TextButton(onClick = onCancelar) {
                            Icon(Icons.Filled.Cancel, null,
                                modifier = Modifier.size(16.dp), tint = ColorError)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancelar", color = ColorError, fontSize = 13.sp)
                        }
                    }
                    if (cita.estado == EstadoCitaEnum.FINALIZADA) {
                        TextButton(onClick = onResena) {
                            Icon(Icons.Filled.Star, null,
                                modifier = Modifier.size(16.dp), tint = ColorDorado)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dejar reseña", color = ColorDorado, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeccionTituloCliente(
    texto: String, colores: BarberiaColores, modifier: Modifier = Modifier
) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier) {
        Box(modifier = Modifier.width(3.dp).height(18.dp)
            .clip(RoundedCornerShape(2.dp)).background(ColorRojo))
        Text(texto, color = colores.texto, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PasoTituloCliente(texto: String, colores: BarberiaColores) {
    Text(texto, color = colores.textoSub, fontSize = 13.sp, letterSpacing = 0.5.sp)
}



