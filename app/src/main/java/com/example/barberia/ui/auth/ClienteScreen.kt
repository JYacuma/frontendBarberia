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
import com.example.barberia.ui.theme.BarberiaColores
import com.example.barberia.ui.theme.ColorAzulClaro
import com.example.barberia.ui.theme.ColorBlanco
import com.example.barberia.ui.theme.ColorAzul
import com.example.barberia.ui.theme.ColorDorado
import com.example.barberia.ui.theme.ColorError
import com.example.barberia.ui.theme.ColorRojo
import com.example.barberia.ui.theme.ColorVerde
import com.example.barberia.ui.theme.LocalBarberiaColores
import com.example.barberia.ui.theme.TemaManager
import com.example.barberia.viewmodel.ClienteViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ClienteScreen(
    apiService: ApiService,
    idUsuario: Long,
    nombre: String,
    onLogout: () -> Unit
) {
    val colores = LocalBarberiaColores.current

    val viewModel: ClienteViewModel = viewModel(
        factory = ClienteViewModel.factory(apiService, idUsuario)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // pagerState sincroniza el swipe con el tap en la bottom bar
    val pagerState    = rememberPagerState(pageCount = { 3 })
    val scope         = rememberCoroutineScope()
    val snackbarState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { snackbarState.showSnackbar(it); viewModel.clearMessages() }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarState.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        containerColor = colores.fondo,
        snackbarHost = {
            SnackbarHost(snackbarState) { data ->
                Snackbar(snackbarData = data, containerColor = colores.superficie,
                    contentColor = colores.texto, actionColor = ColorRojo,
                    shape = RoundedCornerShape(12.dp))
            }
        },
        bottomBar = {
            ClienteBottomBar(pagerState.currentPage, colores) { index ->
                scope.launch { pagerState.animateScrollToPage(index) }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(padding).fillMaxSize(),
            userScrollEnabled = true   // permite deslizar con el dedo
        ) { pagina ->
            when (pagina) {
                0 -> InicioTab(nombre, uiState, viewModel, onLogout, colores)
                1 -> AgendarTab(uiState, viewModel, colores)
                2 -> MisCitasTab(uiState, viewModel, colores)
            }
        }
    }
}

// ── Bottom Bar ────────────────────────────────────────────────────────────────
@Composable
private fun ClienteBottomBar(
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
            Triple("Agendar",   Icons.Filled.CalendarMonth, 1),
            Triple("Mis Citas", Icons.Filled.List,          2)
        ).forEach { (label, icon, index) ->
            val activo = paginaActual == index
            NavigationBarItem(
                selected = activo, onClick = { onTabSelected(index) },
                icon = { Icon(icon, label,
                    tint = if (activo) ColorRojo else colores.textoSub,
                    modifier = Modifier.size(22.dp)) },
                label = { Text(label,
                    color = if (activo) ColorRojo else colores.textoSub,
                    fontSize = 11.sp,
                    fontWeight = if (activo) FontWeight.SemiBold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = ColorRojo.copy(alpha = 0.15f))
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 1 — INICIO
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun InicioTab(
    nombre: String,
    uiState: com.example.barberia.viewmodel.ClienteUiState,
    viewModel: ClienteViewModel,
    onLogout: () -> Unit,
    colores: BarberiaColores
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colores.fondo)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().background(
                Brush.verticalGradient(
                    if (colores.esModoOscuro)
                        listOf(Color(0xFF1A0303), colores.fondo)
                    else
                        listOf(Color(0xFFFFF5F5), colores.fondo)
                )
            )) {
                Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(
                    Brush.horizontalGradient(
                        listOf(ColorRojo, ColorAzul, ColorBlanco, ColorRojo))))
                Column(modifier = Modifier.padding(
                    start = 20.dp, end = 20.dp, top = 52.dp, bottom = 20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape)
                                    .background(ColorRojo))
                                Text("CLIENTE", color = ColorRojo, fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Hola, $nombre 👋", color = colores.texto,
                                fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("¿Qué servicio necesitas hoy?",
                                color = colores.textoSub, fontSize = 13.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                                .background(ColorRojo), contentAlignment = Alignment.Center) {
                                Text(nombre.take(2).uppercase(), color = Color.White,
                                    fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            // Botón modo claro/oscuro
                            val sistemaOscuro = isSystemInDarkTheme()
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
                            BotonIcono(Icons.AutoMirrored.Filled.Logout,
                                colores.textoSub, onLogout)
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
                    CircularProgressIndicator(color = ColorRojo,
                        strokeWidth = 2.5.dp, modifier = Modifier.size(36.dp))
                }
            }
        } else {
            val proximaCita = uiState.citas.firstOrNull {
                it.estado == EstadoCitaEnum.PENDIENTE }

            if (proximaCita != null) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SeccionTitulo("Mi próxima cita", colores)
                        Spacer(modifier = Modifier.height(10.dp))
                        ProximaCitaCard(proximaCita, colores)
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(uiState.citas.size.toString(), "Citas totales",
                        ColorRojo, colores, Modifier.weight(1f))
                    StatCard(uiState.citas.count {
                        it.estado == EstadoCitaEnum.FINALIZADA }.toString(),
                        "Completadas", ColorVerde, colores, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SeccionTitulo("Nuestros barberos", colores) }
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp)) {
                    items(uiState.barberos) { TarjetaBarberoChip(it, colores) }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SeccionTitulo("Servicios disponibles", colores)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
            items(uiState.servicios) { servicio ->
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    TarjetaServicioCliente(servicio, colores)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 2 — AGENDAR con DatePickerDialog nativo
// ══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgendarTab(
    uiState: com.example.barberia.viewmodel.ClienteUiState,
    viewModel: ClienteViewModel,
    colores: BarberiaColores
) {
    var barberoSeleccionado  by remember { mutableStateOf<BarberoDTO?>(null) }
    var servicioSeleccionado by remember { mutableStateOf<ServicioDTO?>(null) }
    var fechaSeleccionada    by remember { mutableStateOf("") }
    var horaSeleccionada     by remember { mutableStateOf("") }
    var mostrarCalendario    by remember { mutableStateOf(false) }
    val datePickerState      = rememberDatePickerState()

    // Abre el calendario cuando el usuario toca el campo de fecha
    // No permite escritura manual — solo selección visual
    if (mostrarCalendario) {
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    // Convierte millis a "YYYY-MM-DD" sin java.time (minSdk 24)
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = java.util.Calendar.getInstance().apply {
                            timeInMillis = millis
                            // El picker usa UTC → suma 1 día para compensar desfase
                            add(java.util.Calendar.DAY_OF_MONTH, 1)
                        }
                        fechaSeleccionada = "%d-%02d-%02d".format(
                            cal.get(java.util.Calendar.YEAR),
                            cal.get(java.util.Calendar.MONTH) + 1,
                            cal.get(java.util.Calendar.DAY_OF_MONTH)
                        )
                        horaSeleccionada = ""
                    }
                    mostrarCalendario = false
                }) { Text("Confirmar", color = ColorRojo, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalendario = false }) {
                    Text("Cancelar", color = colores.textoSub) }
            },
            colors = DatePickerDefaults.colors(
                containerColor = colores.superficie,
                titleContentColor = colores.texto,
                headlineContentColor = ColorRojo,
                weekdayContentColor = colores.textoSub,
                dayContentColor = colores.texto,
                selectedDayContainerColor = ColorRojo,
                selectedDayContentColor = Color.White,
                todayDateBorderColor = ColorRojo,
                todayContentColor = ColorRojo
            )
        ) {
            DatePicker(state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = colores.superficie,
                    titleContentColor = colores.texto,
                    headlineContentColor = ColorRojo,
                    weekdayContentColor = colores.textoSub,
                    dayContentColor = colores.texto,
                    selectedDayContainerColor = ColorRojo,
                    selectedDayContentColor = Color.White,
                    todayDateBorderColor = ColorRojo,
                    todayContentColor = ColorRojo
                ))
        }
    }

    LaunchedEffect(barberoSeleccionado, fechaSeleccionada) {
        val idB = barberoSeleccionado?.idBarbero
        if (idB != null && fechaSeleccionada.length == 10) {
            viewModel.cargarDisponibilidad(idB, fechaSeleccionada)
            horaSeleccionada = ""
        } else viewModel.limpiarDisponibilidad()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colores.fondo)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Agendar cita", color = colores.texto,
                fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Sigue los pasos para reservar",
                color = colores.textoSub, fontSize = 13.sp)
        }

        // Paso 1: Barbero
        item {
            PasoTitulo("1", "Elige tu barbero",
                barberoSeleccionado != null, colores)
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.barberos) { barbero ->
                    BarberoSelectorCard(
                        barbero = barbero,
                        seleccionado = barberoSeleccionado?.idBarbero == barbero.idBarbero,
                        colores = colores,
                        onClick = { barberoSeleccionado = barbero; horaSeleccionada = "" }
                    )
                }
            }
        }

        // Paso 2: Servicio
        item {
            PasoTitulo("2", "Elige el servicio",
                servicioSeleccionado != null, colores)
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.servicios.forEach { servicio ->
                    ServicioSelectorCard(
                        servicio = servicio,
                        seleccionado = servicioSeleccionado?.idServicio == servicio.idServicio,
                        colores = colores,
                        onClick = { servicioSeleccionado = servicio }
                    )
                }
            }
        }

        // Paso 3: Fecha — toca para abrir calendario
        item {
            PasoTitulo("3", "Selecciona la fecha",
                fechaSeleccionada.length == 10, colores)
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colores.superficie)
                    .border(1.dp,
                        if (fechaSeleccionada.isNotBlank()) ColorRojo else colores.borde,
                        RoundedCornerShape(12.dp))
                    .clickable { mostrarCalendario = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.CalendarMonth, null,
                        tint = ColorRojo, modifier = Modifier.size(20.dp))
                    Text(
                        text = if (fechaSeleccionada.isNotBlank()) fechaSeleccionada
                        else "Toca para abrir el calendario",
                        color = if (fechaSeleccionada.isNotBlank()) colores.texto
                        else colores.textoSub,
                        fontSize = 14.sp
                    )
                    if (fechaSeleccionada.isNotBlank()) {
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.Edit, null,
                            tint = colores.textoSub, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Paso 4: Hora
        item {
            PasoTitulo("4", "Elige la hora",
                horaSeleccionada.isNotBlank(), colores)
            Spacer(modifier = Modifier.height(10.dp))
            when {
                barberoSeleccionado == null || fechaSeleccionada.length < 10 ->
                    Text("Selecciona barbero y fecha primero",
                        color = colores.textoSub, fontSize = 13.sp)
                uiState.isLoading ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(color = ColorRojo,
                            modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Cargando horarios...", color = colores.textoSub, fontSize = 13.sp)
                    }
                uiState.horasDisponibles.isEmpty() ->
                    Text("Sin horarios disponibles para esta fecha",
                        color = colores.textoSub, fontSize = 13.sp)
                else ->
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.horasDisponibles) { hora ->
                            HoraSelectorChip(hora.take(5),
                                horaSeleccionada == hora, colores) {
                                horaSeleccionada = hora
                            }
                        }
                    }
            }
        }

        // Resumen + Confirmar
        item {
            val formListo = barberoSeleccionado != null &&
                    servicioSeleccionado != null &&
                    fechaSeleccionada.length == 10 &&
                    horaSeleccionada.isNotBlank()

            AnimatedVisibility(visible = formListo) {
                Card(modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ColorRojo.copy(alpha = 0.12f))) {
                    Column(modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Resumen de tu cita", color = ColorRojo,
                            fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        ResumenFila("Barbero",  barberoSeleccionado?.nombre ?: "", colores)
                        ResumenFila("Servicio", servicioSeleccionado?.nombre ?: "", colores)
                        ResumenFila("Fecha",    fechaSeleccionada, colores)
                        ResumenFila("Hora",     horaSeleccionada.take(5), colores)
                        ResumenFila("Precio",
                            "\$${servicioSeleccionado?.precio?.toInt()}", colores)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            BarberiaBoton(
                texto = "Confirmar cita", icono = Icons.Filled.Check,
                isLoading = uiState.isLoading,
                colorFondo = if (formListo) ColorRojo else colores.borde,
                onClick = {
                    if (formListo) viewModel.agendarCita(
                        barberoSeleccionado!!.idBarbero!!,
                        servicioSeleccionado!!.idServicio!!,
                        fechaSeleccionada, horaSeleccionada)
                }
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 3 — MIS CITAS
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun MisCitasTab(
    uiState: com.example.barberia.viewmodel.ClienteUiState,
    viewModel: ClienteViewModel,
    colores: BarberiaColores
) {
    var citaParaResena   by remember { mutableStateOf<CitaDTO?>(null) }
    var citaParaCancelar by remember { mutableStateOf<CitaDTO?>(null) }
    var calificacion     by remember { mutableStateOf(5) }
    var comentario       by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colores.fondo)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Mis citas", color = colores.texto,
                fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("${uiState.citas.size} citas en total",
                color = colores.textoSub, fontSize = 13.sp)
        }

        if (uiState.isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ColorRojo,
                        modifier = Modifier.size(36.dp), strokeWidth = 2.5.dp)
                }
            }
        } else if (uiState.citas.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(220.dp),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.CalendarMonth, null,
                            tint = colores.textoSub, modifier = Modifier.size(48.dp))
                        Text("No tienes citas aún",
                            color = colores.textoSub, fontSize = 15.sp)
                    }
                }
            }
        } else {
            items(uiState.citas) { cita ->
                TarjetaCitaCliente(cita, colores,
                    onCancelar = { citaParaCancelar = cita },
                    onResena   = { citaParaResena   = cita })
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    // Dialog cancelar
    citaParaCancelar?.let { cita ->
        AlertDialog(
            onDismissRequest = { citaParaCancelar = null },
            containerColor = colores.superficie,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Cancelar cita", color = colores.texto,
                fontWeight = FontWeight.Bold) },
            text = { Text(
                "¿Seguro que deseas cancelar la cita del ${cita.fecha} a las ${cita.horaInicio?.take(5)}?",
                color = colores.textoSub, fontSize = 14.sp) },
            confirmButton = {
                BarberiaBoton("Sí, cancelar", colorFondo = ColorError, onClick = {
                    viewModel.cancelarCita(cita.idCita!!)
                    citaParaCancelar = null
                })
            },
            dismissButton = {
                TextButton(onClick = { citaParaCancelar = null }) {
                    Text("No", color = colores.textoSub) }
            }
        )
    }

    // Dialog reseña
    citaParaResena?.let { cita ->
        AlertDialog(
            onDismissRequest = { citaParaResena = null },
            containerColor = colores.superficie,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Dejar reseña", color = colores.texto,
                fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Cita #${cita.idCita}",
                        color = colores.textoSub, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..5).forEach { estrella ->
                            val interSrc = remember { MutableInteractionSource() }
                            val isPressed by interSrc.collectIsPressedAsState()
                            val escala by animateFloatAsState(
                                targetValue = if (isPressed) 0.85f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "star$estrella")
                            Icon(
                                imageVector = if (estrella <= calificacion)
                                    Icons.Filled.Star else Icons.Filled.StarOutline,
                                contentDescription = null, tint = ColorDorado,
                                modifier = Modifier.size(36.dp).scale(escala)
                                    .clickable(interSrc, null) { calificacion = estrella }
                            )
                        }
                    }
                    BarberiaTextField(value = comentario,
                        onValueChange = { comentario = it },
                        label = "Comentario (opcional)",
                        leadingIcon = Icons.Filled.Comment, accentColor = ColorRojo)
                }
            },
            confirmButton = {
                BarberiaBoton("Enviar reseña", icono = Icons.Filled.Send,
                    colorFondo = ColorRojo, onClick = {
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
}

// ══════════════════════════════════════════════════════════════════════════════
// COMPONENTES
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ProximaCitaCard(cita: CitaDTO, colores: BarberiaColores) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colores.superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = colores.sombra.dp)) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(
                Brush.horizontalGradient(listOf(ColorRojo, ColorRojo.copy(0.3f)))))
            Row(modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Próxima cita", color = colores.textoSub,
                        fontSize = 11.sp, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Cita #${cita.idCita}", color = colores.texto,
                        fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${cita.fecha} · ${cita.horaInicio?.take(5)}",
                        color = colores.textoSub, fontSize = 13.sp)
                }
                Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                    .background(ColorRojo.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.ContentCut, null,
                        tint = ColorRojo, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun StatCard(
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
private fun TarjetaBarberoChip(barbero: BarberoDTO, colores: BarberiaColores) {
    Card(modifier = Modifier.width(130.dp), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colores.superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = colores.sombra.dp)) {
        Column(modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                .background(ColorRojo.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center) {
                Text(barbero.nombre.take(2).uppercase(), color = ColorRojo,
                    fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(barbero.nombre, color = colores.texto, fontSize = 13.sp,
                fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            barbero.especialidad?.let {
                Text(it, color = colores.textoSub, fontSize = 11.sp,
                    textAlign = TextAlign.Center, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun TarjetaServicioCliente(servicio: ServicioDTO, colores: BarberiaColores) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colores.superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = colores.sombra.dp)) {
        Row(modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(servicio.nombre, color = colores.texto,
                    fontWeight = FontWeight.Medium, fontSize = 14.sp)
                servicio.descripcion?.let {
                    Text(it, color = colores.textoSub, fontSize = 12.sp, maxLines = 1) }
                Text("${servicio.duracionMinutos} min",
                    color = colores.textoSub, fontSize = 12.sp)
            }
            Text("\$${servicio.precio.toInt()}", color = ColorRojo,
                fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun SeccionTitulo(texto: String, colores: BarberiaColores) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.width(3.dp).height(18.dp)
            .clip(RoundedCornerShape(2.dp)).background(ColorRojo))
        Text(texto, color = colores.texto, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PasoTitulo(
    numero: String, texto: String,
    completado: Boolean, colores: BarberiaColores
) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.size(26.dp).clip(CircleShape)
            .background(if (completado) ColorRojo else colores.superficie2)
            .border(1.dp, if (completado) ColorRojo else colores.borde, CircleShape),
            contentAlignment = Alignment.Center) {
            AnimatedContent(completado,
                transitionSpec = { scaleIn() togetherWith scaleOut() },
                label = "paso$numero") { done ->
                if (done)
                    Icon(Icons.Filled.Check, null, tint = Color.White,
                        modifier = Modifier.size(14.dp))
                else
                    Text(numero, color = colores.textoSub,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(texto,
            color = if (completado) colores.texto else colores.textoSub,
            fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BarberoSelectorCard(
    barbero: BarberoDTO, seleccionado: Boolean,
    colores: BarberiaColores, onClick: () -> Unit
) {
    val interSrc = remember { MutableInteractionSource() }
    val isPressed by interSrc.collectIsPressedAsState()
    val escala by animateFloatAsState(
        if (isPressed) 0.94f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "bs")
    Card(modifier = Modifier.width(120.dp).scale(escala)
        .clickable(interSrc, null) { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionado) ColorRojo else colores.superficie),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (seleccionado) 0.dp else colores.sombra.dp)) {
        Column(modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Person, null,
                tint = if (seleccionado) Color.White else ColorRojo,
                modifier = Modifier.size(30.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(barbero.nombre,
                color = if (seleccionado) Color.White else colores.texto,
                fontSize = 12.sp, fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center, maxLines = 2,
                overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ServicioSelectorCard(
    servicio: ServicioDTO, seleccionado: Boolean,
    colores: BarberiaColores, onClick: () -> Unit
) {
    val interSrc = remember { MutableInteractionSource() }
    val isPressed by interSrc.collectIsPressedAsState()
    val escala by animateFloatAsState(
        if (isPressed) 0.97f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "ss")
    Card(modifier = Modifier.fillMaxWidth().scale(escala)
        .clickable(interSrc, null) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionado) ColorRojo else colores.superficie)) {
        Row(modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(servicio.nombre,
                    color = if (seleccionado) Color.White else colores.texto,
                    fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text("${servicio.duracionMinutos} min",
                    color = if (seleccionado) Color.White.copy(0.7f) else colores.textoSub,
                    fontSize = 12.sp)
            }
            Text("\$${servicio.precio.toInt()}",
                color = if (seleccionado) Color.White else ColorRojo,
                fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun HoraSelectorChip(
    hora: String, seleccionada: Boolean,
    colores: BarberiaColores, onClick: () -> Unit
) {
    val interSrc = remember { MutableInteractionSource() }
    val isPressed by interSrc.collectIsPressedAsState()
    val escala by animateFloatAsState(
        if (isPressed) 0.9f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "hs")
    Box(modifier = Modifier.scale(escala)
        .clip(RoundedCornerShape(10.dp))
        .background(if (seleccionada) ColorRojo else colores.superficie2)
        .border(1.dp,
            if (seleccionada) ColorRojo else colores.borde, RoundedCornerShape(10.dp))
        .clickable(interSrc, null) { onClick() }
        .padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(hora, color = if (seleccionada) Color.White else colores.texto,
            fontSize = 13.sp,
            fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun ResumenFila(etiqueta: String, valor: String, colores: BarberiaColores) {
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, color = colores.textoSub, fontSize = 13.sp)
        Text(valor, color = colores.texto, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TarjetaCitaCliente(
    cita: CitaDTO, colores: BarberiaColores,
    onCancelar: () -> Unit, onResena: () -> Unit
) {
    val colorEstado = when (cita.estado) {
        EstadoCitaEnum.PENDIENTE     -> ColorDorado
        EstadoCitaEnum.EN_CURSO      -> ColorAzulClaro
        EstadoCitaEnum.FINALIZADA    -> ColorVerde
        EstadoCitaEnum.CANCELADA     -> ColorError
        EstadoCitaEnum.NO_PRESENTADO -> colores.textoSub
        null                         -> colores.textoSub
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colores.superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = colores.sombra.dp)) {
        Column {
            Row {
                Box(modifier = Modifier.width(4.dp).height(80.dp).background(colorEstado))
                Column(modifier = Modifier.weight(1f)
                    .padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top) {
                        Column {
                            Text("Cita #${cita.idCita}", color = colores.texto,
                                fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${cita.fecha} · ${cita.horaInicio?.take(5)} – ${cita.horaFin?.take(5)}",
                                color = colores.textoSub, fontSize = 12.sp)
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(colorEstado.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text(
                                text = when (cita.estado) {
                                    EstadoCitaEnum.PENDIENTE     -> "Pendiente"
                                    EstadoCitaEnum.EN_CURSO      -> "En curso"
                                    EstadoCitaEnum.FINALIZADA    -> "Finalizada"
                                    EstadoCitaEnum.CANCELADA     -> "Cancelada"
                                    EstadoCitaEnum.NO_PRESENTADO -> "No se presentó"
                                    null -> ""
                                },
                                color = colorEstado, fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            if (cita.estado == EstadoCitaEnum.PENDIENTE ||
                cita.estado == EstadoCitaEnum.FINALIZADA) {
                HorizontalDivider(color = colores.borde)
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
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
private fun BotonIcono(icono: ImageVector, tint: Color, onClick: () -> Unit) {
    val interSrc = remember { MutableInteractionSource() }
    val isPressed by interSrc.collectIsPressedAsState()
    val escala by animateFloatAsState(
        if (isPressed) 0.85f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "bi")
    IconButton(onClick = onClick, interactionSource = interSrc,
        modifier = Modifier.scale(escala)) {
        Icon(icono, null, tint = tint, modifier = Modifier.size(22.dp))
    }
}