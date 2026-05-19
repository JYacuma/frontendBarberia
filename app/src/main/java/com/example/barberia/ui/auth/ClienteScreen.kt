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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.barberia.ui.theme.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClienteScreen(
    apiService: ApiService,
    idUsuario: Long,
    nombre: String,
    onLogout: () -> Unit
) {
    val colores       = LocalBarberiaColores.current
    val sistemaOscuro = isSystemInDarkTheme()

    val viewModel: ClienteViewModel = viewModel(
        factory = ClienteViewModel.factory(apiService, idUsuario)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 4 tabs con swipe
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
                Snackbar(
                    snackbarData   = data,
                    containerColor = colores.superficie,
                    contentColor   = colores.texto,
                    actionColor    = ColorRojo,
                    shape          = RoundedCornerShape(12.dp)
                )
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
                    Triple("Mis Citas",Icons.Filled.List,           2),
                    Triple("Perfil",   Icons.Filled.Person,         3)
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
                0 -> InicioTab(nombre, uiState, onLogout, colores, sistemaOscuro)
                1 -> AgendarTab(uiState, viewModel, colores)
                2 -> MisCitasTab(uiState, viewModel, colores)
                3 -> PerfilTab(uiState, viewModel, onLogout, colores, sistemaOscuro)
            }
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
    onLogout: () -> Unit,
    colores: BarberiaColores,
    sistemaOscuro: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colores.fondo)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().background(
                Brush.verticalGradient(
                    if (colores.esModoOscuro)
                        listOf(Color(0xFF1A0505), colores.fondo)
                    else
                        listOf(Color(0xFFFFF5F5), colores.fondo)
                )
            )) {
                Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(
                    Brush.horizontalGradient(
                        listOf(ColorRojo, ColorBlanco, ColorRojo, ColorBlanco, ColorRojo)
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
                                    .background(ColorRojo))
                                Text("CLIENTE", color = ColorRojo, fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Hola, $nombre", color = colores.texto,
                                fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("¿Qué servicio deseas hoy?",
                                color = colores.textoSub, fontSize = 13.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                                .background(ColorRojo.copy(0.15f))
                                .border(1.5.dp, ColorRojo, CircleShape),
                                contentAlignment = Alignment.Center) {
                                Text(nombre.take(2).uppercase(), color = ColorRojo,
                                    fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                        }
                    }
                }
            }
        }

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
                Spacer(modifier = Modifier.height(20.dp))
                // Banner rojo
                Box(modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(
                        listOf(ColorRojo, ColorRojoClaro)))) {
                    Row(modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ContentCut, null,
                            tint = Color.White, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Reserva tu cita", color = Color.White,
                                fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Elige tu barbero favorito",
                                color = Color.White.copy(0.8f), fontSize = 13.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Barberos activos
            item {
                Row(modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    SeccionTituloCliente("Nuestros Barberos", colores)
                    Text("${uiState.barberos.size} disponibles",
                        color = colores.textoSub, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    items(uiState.barberos) { barbero ->
                        TarjetaBarberoCliente(barbero, colores)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Servicios
            item {
                SeccionTituloCliente("Servicios", colores,
                    modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(modifier = Modifier.height(10.dp))
            }

            items(uiState.servicios) { servicio ->
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    TarjetaServicioCliente(servicio, colores)
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 2 — AGENDAR
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

    // Convierte millis UTC a "YYYY-MM-DD" sumando 1 día por zona horaria
    fun millisAFecha(millis: Long): String {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = millis
            add(java.util.Calendar.DAY_OF_MONTH, 1)
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

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colores.fondo)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Agendar Cita", color = colores.texto,
                fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Sigue los pasos para reservar", color = colores.textoSub, fontSize = 13.sp)
        }

        // Paso 1 — Barbero
        item {
            PasoTituloCliente("1. Elige tu barbero", colores)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.barberos) { barbero ->
                    val sel = barberoSeleccionado?.idBarbero == barbero.idBarbero
                    Card(modifier = Modifier.width(110.dp).clickable {
                        barberoSeleccionado = barbero
                        horaSeleccionada = ""
                        viewModel.limpiarDisponibilidad()
                        if (fechaSeleccionada.length == 10)
                            viewModel.cargarDisponibilidad(
                                barbero.idBarbero!!, fechaSeleccionada)
                    },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (sel) ColorRojo else colores.superficie),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = colores.sombra.dp)) {
                        Column(modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                                .background(if (sel) Color.White.copy(0.2f)
                                else ColorRojo.copy(0.15f)),
                                contentAlignment = Alignment.Center) {
                                Text(barbero.nombre.take(2).uppercase(),
                                    color = if (sel) Color.White else ColorRojo,
                                    fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(barbero.nombre,
                                color = if (sel) Color.White else colores.texto,
                                fontSize = 12.sp, textAlign = TextAlign.Center,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            barbero.especialidad?.let {
                                Text(it,
                                    color = if (sel) Color.White.copy(0.7f)
                                    else colores.textoSub,
                                    fontSize = 10.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }

        // Paso 2 — Servicio
        item {
            PasoTituloCliente("2. Elige el servicio", colores)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.servicios.forEach { servicio ->
                    val sel = servicioSeleccionado?.idServicio == servicio.idServicio
                    Card(modifier = Modifier.fillMaxWidth().clickable {
                        servicioSeleccionado = servicio },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (sel) ColorRojo else colores.superficie),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = colores.sombra.dp)) {
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

        // Paso 3 — Fecha
        item {
            PasoTituloCliente("3. Elige la fecha", colores)
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colores.superficie)
                .border(1.dp,
                    if (fechaSeleccionada.isNotBlank()) ColorRojo else colores.borde,
                    RoundedCornerShape(14.dp))
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

        // Paso 4 — Hora
        if (uiState.horasDisponibles.isNotEmpty()) {
            item {
                PasoTituloCliente("4. Elige la hora", colores)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.horasDisponibles) { hora ->
                        val horaCorta = hora.take(5)
                        val sel       = horaSeleccionada == hora
                        Box(modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) ColorRojo else colores.superficie)
                            .border(1.dp,
                                if (sel) ColorRojo else colores.borde,
                                RoundedCornerShape(10.dp))
                            .clickable { horaSeleccionada = hora }
                            .padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Text(horaCorta,
                                color = if (sel) Color.White else colores.texto,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp)
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
                    }
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
    var citaParaResena by remember { mutableStateOf<CitaDTO?>(null) }
    var calificacion   by remember { mutableStateOf(5) }
    var comentario     by remember { mutableStateOf("") }

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
                Text("Mis Citas", color = colores.texto,
                    fontSize = 22.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { viewModel.cargarDatosIniciales() }) {
                    Icon(Icons.Filled.Refresh, null,
                        tint = ColorRojo, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Actualizar", color = ColorRojo, fontSize = 12.sp)
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
        } else if (uiState.citas.isEmpty()) {
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
            items(uiState.citas) { cita ->
                TarjetaCitaCliente(
                    cita      = cita,
                    colores   = colores,
                    onCancelar = { viewModel.cancelarCita(cita.idCita!!) },
                    onResena   = { citaParaResena = cita }
                )
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
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
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 4 — PERFIL
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun PerfilTab(
    uiState: com.example.barberia.viewmodel.ClienteUiState,
    viewModel: ClienteViewModel,
    onLogout: () -> Unit,
    colores: BarberiaColores,
    sistemaOscuro: Boolean
) {
    var modoEdicion  by remember { mutableStateOf(false) }
    var nombreEdit   by remember { mutableStateOf("") }
    var telefonoEdit by remember { mutableStateOf("") }

    // Inicializa los campos con los datos actuales del perfil
    LaunchedEffect(uiState.perfil) {
        uiState.perfil?.let {
            nombreEdit   = it.nombre ?: ""
            telefonoEdit = it.telefono ?: ""
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colores.fondo)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Mi Perfil", color = colores.texto,
                fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        // Avatar + nombre
        item {
            Card(modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colores.superficie),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = colores.sombra.dp)) {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(
                        Brush.horizontalGradient(
                            listOf(ColorRojo, ColorRojoClaro))))
                    Column(modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        // Avatar circular con iniciales
                        Box(modifier = Modifier.size(80.dp).clip(CircleShape)
                            .background(ColorRojo.copy(0.15f))
                            .border(2.dp, ColorRojo, CircleShape),
                            contentAlignment = Alignment.Center) {
                            Text(
                                text = uiState.perfil?.nombre
                                    ?.split(" ")?.take(2)
                                    ?.joinToString("") { it.take(1).uppercase() }
                                    ?: "?",
                                color = ColorRojo,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(uiState.perfil?.nombre ?: "Cargando...",
                            color = colores.texto, fontSize = 18.sp,
                            fontWeight = FontWeight.Bold)
                        Text(uiState.perfil?.correo ?: "",
                            color = colores.textoSub, fontSize = 13.sp)

                        // Badge del rol
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(ColorRojo.copy(0.15f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)) {
                            Text("CLIENTE", color = ColorRojo,
                                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }

        // Datos editables
        item {
            Card(modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colores.superficie),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = colores.sombra.dp)) {
                Column(modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Información personal", color = colores.texto,
                            fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        TextButton(onClick = { modoEdicion = !modoEdicion }) {
                            Icon(
                                imageVector = if (modoEdicion)
                                    Icons.Filled.Close else Icons.Filled.Edit,
                                contentDescription = null,
                                tint = ColorRojo,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (modoEdicion) "Cancelar" else "Editar",
                                color = ColorRojo, fontSize = 13.sp)
                        }
                    }

                    if (modoEdicion) {
                        // Modo edición — campos editables
                        BarberiaTextField(nombreEdit, { nombreEdit = it },
                            "Nombre completo *", Icons.Filled.Person,
                            ColorRojo, colores)
                        BarberiaTextField(telefonoEdit, { telefonoEdit = it },
                            "Teléfono", Icons.Filled.Phone,
                            ColorRojo, colores,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone))
                        // Correo — solo lectura, no se puede cambiar
                        CampoSoloLectura("Correo", uiState.perfil?.correo ?: "",
                            Icons.Filled.Email, colores)

                        BarberiaBoton(
                            texto      = "Guardar cambios",
                            icono      = Icons.Filled.Save,
                            isLoading  = uiState.isLoadingPerfil,
                            colorFondo = if (nombreEdit.isNotBlank()) ColorRojo
                            else colores.borde,
                            onClick    = {
                                if (nombreEdit.isNotBlank()) {
                                    viewModel.actualizarPerfil(nombreEdit, telefonoEdit)
                                    modoEdicion = false
                                }
                            }
                        )
                    } else {
                        // Modo lectura — muestra los datos actuales
                        CampoSoloLectura("Nombre",
                            uiState.perfil?.nombre ?: "Cargando...",
                            Icons.Filled.Person, colores)
                        CampoSoloLectura("Correo",
                            uiState.perfil?.correo ?: "Cargando...",
                            Icons.Filled.Email, colores)
                        CampoSoloLectura("Teléfono",
                            uiState.perfil?.telefono ?: "No registrado",
                            Icons.Filled.Phone, colores)
                    }
                }
            }
        }

        // Estadísticas del cliente
        item {
            Card(modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colores.superficie),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = colores.sombra.dp)) {
                Column(modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Mis estadísticas", color = colores.texto,
                        fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatPerfilCliente(
                            numero   = uiState.citas.size.toString(),
                            label    = "Citas totales",
                            color    = ColorRojo,
                            colores  = colores,
                            modifier = Modifier.weight(1f)
                        )
                        StatPerfilCliente(
                            numero   = uiState.citas.count {
                                it.estado == EstadoCitaEnum.FINALIZADA }.toString(),
                            label    = "Completadas",
                            color    = ColorVerde,
                            colores  = colores,
                            modifier = Modifier.weight(1f)
                        )
                        StatPerfilCliente(
                            numero   = uiState.citas.count {
                                it.estado == EstadoCitaEnum.PENDIENTE }.toString(),
                            label    = "Pendientes",
                            color    = ColorDorado,
                            colores  = colores,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Botón cerrar sesión
        item {
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ColorError),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, ColorError)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null,
                    modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar sesión", fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(20.dp))
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
        icono: ImageVector? = null, // <- Nuevo parámetro opcional
        colorFondo: Color = ColorRojo // <- Nuevo parámetro con valor por defecto
    ) {
    Button(
        onClick    = onClick,
        enabled    = !isLoading,
        modifier   = Modifier.fillMaxWidth().height(52.dp),
        shape      = RoundedCornerShape(14.dp),
        colors     = ButtonDefaults.buttonColors(
            containerColor         = colorFondo,
            disabledContainerColor = colorFondo.copy(alpha = 0.5f)
        )
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
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
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

// Campo de solo lectura para el perfil — no editable
@Composable
private fun CampoSoloLectura(
    label: String, valor: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
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
private fun TarjetaBarberoCliente(barbero: BarberoDTO, colores: BarberiaColores) {
    Card(modifier = Modifier.width(120.dp), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colores.superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = colores.sombra.dp)) {
        Column(modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(42.dp).clip(CircleShape)
                .background(ColorRojo.copy(0.15f)),
                contentAlignment = Alignment.Center) {
                Text(barbero.nombre.take(2).uppercase(),
                    color = ColorRojo, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(barbero.nombre, color = colores.texto, fontSize = 12.sp,
                fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            barbero.especialidad?.let {
                Text(it, color = colores.textoSub, fontSize = 10.sp,
                    textAlign = TextAlign.Center) }
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
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(ColorRojo.copy(0.1f)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.ContentCut, null,
                        tint = ColorRojo, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(servicio.nombre, color = colores.texto,
                        fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text("${servicio.duracionMinutos} min",
                        color = colores.textoSub, fontSize = 12.sp)
                }
            }
            Text("\$${servicio.precio.toInt()}", color = ColorRojo,
                fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
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
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colores.superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = colores.sombra.dp)) {
        Column {
            Row {
                Box(modifier = Modifier.width(4.dp).height(80.dp)
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
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
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
            // Botones según estado
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
                            Text("Reseñar", color = ColorDorado, fontSize = 13.sp)
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