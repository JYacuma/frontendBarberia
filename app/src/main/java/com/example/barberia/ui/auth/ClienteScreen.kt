package com.example.barberia.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.scale
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
import com.example.barberia.viewmodel.ClienteViewModel

// ── Colores específicos del dashboard Cliente (rojo) ─────────────────────────
private val ClienteAccent     = ColorRojo
private val ClienteAccentSoft = ColorRojo.copy(alpha = 0.15f)
private val ClienteFondo      = ColorFondo
private val ClienteCard       = ColorSuperficie

@Composable
fun ClienteScreen(
    apiService: ApiService,
    idUsuario: Long,
    nombre: String,
    onLogout: () -> Unit
) {
    val viewModel: ClienteViewModel = viewModel(
        factory = ClienteViewModel.factory(apiService, idUsuario)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var tabSeleccionado by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Muestra mensajes de éxito y error en snackbar automáticamente
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        containerColor = ClienteFondo,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = ColorSuperficie2,
                    contentColor = ColorTexto,
                    actionColor = ClienteAccent,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        bottomBar = {
            ClienteBottomBar(
                tabSeleccionado = tabSeleccionado,
                onTabSelected = { tabSeleccionado = it }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            // Animación de transición entre tabs con fade
            AnimatedContent(
                targetState = tabSeleccionado,
                transitionSpec = {
                    fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                },
                label = "tabTransicion"
            ) { tab ->
                when (tab) {
                    0 -> InicioTab(nombre, uiState, viewModel, onLogout)
                    1 -> AgendarTab(uiState, viewModel)
                    2 -> MisCitasTab(uiState, viewModel)
                }
            }
        }
    }
}

// ── Bottom Navigation Bar del cliente ────────────────────────────────────────
// 3 tabs: Inicio, Agendar, Mis Citas
// El tab activo se resalta en rojo
@Composable
private fun ClienteBottomBar(
    tabSeleccionado: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = ColorSuperficie,
        tonalElevation = 0.dp,
        modifier = Modifier.border(
            width = 1.dp,
            color = ColorBorde,
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
        )
    ) {
        listOf(
            Triple("Inicio",    Icons.Filled.Home,          0),
            Triple("Agendar",   Icons.Filled.CalendarMonth, 1),
            Triple("Mis Citas", Icons.Filled.List,          2)
        ).forEach { (label, icon, index) ->
            val activo = tabSeleccionado == index
            NavigationBarItem(
                selected = activo,
                onClick  = { onTabSelected(index) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (activo) ClienteAccent else ColorTextoSub,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        color = if (activo) ClienteAccent else ColorTextoSub,
                        fontSize = 11.sp,
                        fontWeight = if (activo) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = ClienteAccentSoft
                )
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 1 — INICIO
// Muestra: saludo, próxima cita, barberos disponibles, servicios
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun InicioTab(
    nombre: String,
    uiState: com.example.barberia.viewmodel.ClienteUiState,
    viewModel: ClienteViewModel,
    onLogout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ClienteFondo),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── Header con gradiente rojo ─────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1A0303), Color(0xFF0A0A0A))
                        )
                    )
            ) {
                // Franja roja superior
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(ClienteAccent, ColorAzul, ColorBlanco, ClienteAccent)
                            )
                        )
                )
                Column(
                    modifier = Modifier.padding(
                        start = 20.dp, end = 20.dp,
                        top = 52.dp, bottom = 20.dp
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            // Badge de rol
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(ClienteAccent)
                                )
                                Text(
                                    text = "CLIENTE",
                                    color = ClienteAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Hola, $nombre 👋",
                                color = ColorTexto,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "¿Qué servicio necesitas hoy?",
                                color = ColorTextoSub,
                                fontSize = 13.sp
                            )
                        }

                        // Avatar inicial + botón logout
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(ClienteAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = nombre.take(2).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // Botón cerrar sesión con animación de escala
                            BotonIcono(
                                icono = Icons.AutoMirrored.Filled.Logout,
                                tint = ColorTextoSub,
                                onClick = onLogout
                            )
                        }
                    }
                }
            }
        }

        // ── Cuerpo ────────────────────────────────────────────────────────
        item { Spacer(modifier = Modifier.height(20.dp)) }

        // Estado de carga global
        if (uiState.isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = ClienteAccent,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        } else {

            // Próxima cita (si existe una PENDIENTE)
            val proximaCita = uiState.citas.firstOrNull {
                it.estado == EstadoCitaEnum.PENDIENTE
            }
            if (proximaCita != null) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SeccionTitulo("Mi próxima cita")
                        Spacer(modifier = Modifier.height(10.dp))
                        ProximaCitaCard(proximaCita)
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Estadísticas rápidas
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        numero = uiState.citas.size.toString(),
                        label = "Citas totales",
                        color = ClienteAccent,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        numero = uiState.citas.count {
                            it.estado == EstadoCitaEnum.FINALIZADA
                        }.toString(),
                        label = "Completadas",
                        color = Color(0xFF3CB86A),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Barberos disponibles
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SeccionTitulo("Nuestros barberos")
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    items(uiState.barberos) { barbero ->
                        TarjetaBarberoChip(barbero)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Servicios
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    SeccionTitulo("Servicios disponibles")
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            items(uiState.servicios) { servicio ->
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    TarjetaServicioCliente(servicio)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 2 — AGENDAR
// Flujo: Barbero → Servicio → Fecha → Hora → Confirmar
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun AgendarTab(
    uiState: com.example.barberia.viewmodel.ClienteUiState,
    viewModel: ClienteViewModel
) {
    var barberoSeleccionado  by remember { mutableStateOf<BarberoDTO?>(null) }
    var servicioSeleccionado by remember { mutableStateOf<ServicioDTO?>(null) }
    var fechaSeleccionada    by remember { mutableStateOf("") }
    var horaSeleccionada     by remember { mutableStateOf("") }

    // Fecha de hoy sin java.time (compatible con minSdk 24)
    val hoy = remember {
        java.util.Calendar.getInstance().let {
            "%d-%02d-%02d".format(
                it.get(java.util.Calendar.YEAR),
                it.get(java.util.Calendar.MONTH) + 1,
                it.get(java.util.Calendar.DAY_OF_MONTH)
            )
        }
    }

    // Cuando cambia el barbero o la fecha, recarga disponibilidad
    LaunchedEffect(barberoSeleccionado, fechaSeleccionada) {
        val idB = barberoSeleccionado?.idBarbero
        if (idB != null && fechaSeleccionada.length == 10) {
            viewModel.cargarDisponibilidad(idB, fechaSeleccionada)
            horaSeleccionada = ""
        } else {
            viewModel.limpiarDisponibilidad()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ClienteFondo)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Agendar cita",
                color = ColorTexto,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sigue los pasos para reservar",
                color = ColorTextoSub,
                fontSize = 13.sp
            )
        }

        // ── Paso 1: Barbero ───────────────────────────────────────────────
        item {
            PasoTitulo(numero = "1", texto = "Elige tu barbero",
                completado = barberoSeleccionado != null)
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.barberos) { barbero ->
                    val seleccionado = barberoSeleccionado?.idBarbero == barbero.idBarbero
                    BarberoSelectorCard(
                        barbero = barbero,
                        seleccionado = seleccionado,
                        onClick = {
                            barberoSeleccionado = barbero
                            horaSeleccionada = ""
                        }
                    )
                }
            }
        }

        // ── Paso 2: Servicio ──────────────────────────────────────────────
        item {
            PasoTitulo(numero = "2", texto = "Elige el servicio",
                completado = servicioSeleccionado != null)
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.servicios.forEach { servicio ->
                    val seleccionado = servicioSeleccionado?.idServicio == servicio.idServicio
                    ServicioSelectorCard(
                        servicio = servicio,
                        seleccionado = seleccionado,
                        onClick = { servicioSeleccionado = servicio }
                    )
                }
            }
        }

        // ── Paso 3: Fecha ─────────────────────────────────────────────────
        item {
            PasoTitulo(numero = "3", texto = "Selecciona la fecha",
                completado = fechaSeleccionada.length == 10)
            Spacer(modifier = Modifier.height(10.dp))
            BarberiaTextField(
                value = fechaSeleccionada,
                onValueChange = { nueva ->
                    fechaSeleccionada = nueva
                    horaSeleccionada = ""
                },
                label = "Fecha — ej: $hoy",
                leadingIcon = Icons.Filled.CalendarMonth,
                accentColor = ClienteAccent,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        // ── Paso 4: Hora ──────────────────────────────────────────────────
        item {
            PasoTitulo(numero = "4", texto = "Elige la hora",
                completado = horaSeleccionada.isNotBlank())
            Spacer(modifier = Modifier.height(10.dp))

            if (barberoSeleccionado == null || fechaSeleccionada.length < 10) {
                Text(
                    text = "Selecciona barbero y fecha primero",
                    color = ColorTextoSub,
                    fontSize = 13.sp
                )
            } else if (uiState.isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        color = ClienteAccent,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Text("Cargando horarios...", color = ColorTextoSub, fontSize = 13.sp)
                }
            } else if (uiState.horasDisponibles.isEmpty()) {
                Text(
                    text = "Sin horarios disponibles para esta fecha",
                    color = ColorTextoSub,
                    fontSize = 13.sp
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.horasDisponibles) { hora ->
                        val horaCorta = hora.take(5) // "09:00"
                        val seleccionada = horaSeleccionada == hora
                        HoraSelectorChip(
                            hora = horaCorta,
                            seleccionada = seleccionada,
                            onClick = { horaSeleccionada = hora }
                        )
                    }
                }
            }
        }

        // ── Botón confirmar ───────────────────────────────────────────────
        item {
            val formListo = barberoSeleccionado != null &&
                    servicioSeleccionado != null &&
                    fechaSeleccionada.length == 10 &&
                    horaSeleccionada.isNotBlank()

            // Resumen antes de confirmar
            AnimatedVisibility(visible = formListo) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ClienteAccentSoft
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Resumen de tu cita",
                            color = ClienteAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp)
                        ResumenFila("Barbero",   barberoSeleccionado?.nombre ?: "")
                        ResumenFila("Servicio",  servicioSeleccionado?.nombre ?: "")
                        ResumenFila("Fecha",     fechaSeleccionada)
                        ResumenFila("Hora",      horaSeleccionada.take(5))
                        ResumenFila("Precio",    "\$${servicioSeleccionado?.precio?.toInt()}")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            BarberiaBoton(
                texto = "Confirmar cita",
                icono = Icons.Filled.Check,
                isLoading = uiState.isLoading,
                colorFondo = if (formListo) ClienteAccent else ColorBorde,
                onClick = {
                    if (formListo) {
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
// Lista todas las citas del cliente con opción de cancelar o dejar reseña
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun MisCitasTab(
    uiState: com.example.barberia.viewmodel.ClienteUiState,
    viewModel: ClienteViewModel
) {
    var citaParaResena  by remember { mutableStateOf<CitaDTO?>(null) }
    var citaParaCancelar by remember { mutableStateOf<CitaDTO?>(null) }
    var calificacion    by remember { mutableStateOf(5) }
    var comentario      by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ClienteFondo)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Mis citas", color = ColorTexto, fontSize = 22.sp,
                fontWeight = FontWeight.Bold)
            Text("${uiState.citas.size} citas en total",
                color = ColorTextoSub, fontSize = 13.sp)
        }

        if (uiState.isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ClienteAccent,
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
                            tint = ColorTextoSub, modifier = Modifier.size(48.dp))
                        Text("No tienes citas aún", color = ColorTextoSub,
                            fontSize = 15.sp)
                        Text("Ve a la tab Agendar para reservar",
                            color = ColorTextoSub.copy(0.6f), fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(uiState.citas) { cita ->
                TarjetaCitaCliente(
                    cita = cita,
                    onCancelar = { citaParaCancelar = cita },
                    onResena   = { citaParaResena  = cita }
                )
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    // ── Dialog confirmar cancelación ──────────────────────────────────────
    citaParaCancelar?.let { cita ->
        AlertDialog(
            onDismissRequest = { citaParaCancelar = null },
            containerColor   = ColorSuperficie,
            shape            = RoundedCornerShape(20.dp),
            title = {
                Text("Cancelar cita", color = ColorTexto, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "¿Seguro que deseas cancelar la cita del ${cita.fecha} a las ${cita.horaInicio?.take(5)}?",
                    color = ColorTextoSub, fontSize = 14.sp
                )
            },
            confirmButton = {
                BarberiaBoton(
                    texto = "Sí, cancelar",
                    colorFondo = ColorError,
                    onClick = {
                        viewModel.cancelarCita(cita.idCita!!)
                        citaParaCancelar = null
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { citaParaCancelar = null }) {
                    Text("No", color = ColorTextoSub)
                }
            }
        )
    }

    // ── Dialog reseña ─────────────────────────────────────────────────────
    citaParaResena?.let { cita ->
        AlertDialog(
            onDismissRequest = { citaParaResena = null },
            containerColor   = ColorSuperficie,
            shape            = RoundedCornerShape(20.dp),
            title = {
                Text("Dejar reseña", color = ColorTexto, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Barbero: Cita #${cita.idCita}",
                        color = ColorTextoSub, fontSize = 13.sp)

                    // Selector de estrellas con animación
                    Column {
                        Text("Calificación", color = ColorTextoSub,
                            fontSize = 12.sp, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (1..5).forEach { estrella ->
                                val interactionSource =
                                    remember { MutableInteractionSource() }
                                val isPressed by
                                interactionSource.collectIsPressedAsState()
                                val escala by animateFloatAsState(
                                    targetValue = if (isPressed) 0.85f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy
                                    ),
                                    label = "estrella$estrella"
                                )
                                Icon(
                                    imageVector = if (estrella <= calificacion)
                                        Icons.Filled.Star
                                    else
                                        Icons.Filled.StarOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFD4A017),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .scale(escala)
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null
                                        ) { calificacion = estrella }
                                )
                            }
                        }
                    }

                    BarberiaTextField(
                        value = comentario,
                        onValueChange = { comentario = it },
                        label = "Comentario (opcional)",
                        leadingIcon = Icons.Filled.Comment,
                        accentColor = ClienteAccent
                    )
                }
            },
            confirmButton = {
                BarberiaBoton(
                    texto = "Enviar reseña",
                    icono = Icons.Filled.Send,
                    colorFondo = ClienteAccent,
                    onClick = {
                        viewModel.enviarResena(
                            idCita       = cita.idCita!!,
                            idBarbero    = cita.idBarbero,
                            calificacion = calificacion,
                            comentario   = comentario
                        )
                        citaParaResena = null
                        comentario     = ""
                        calificacion   = 5
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { citaParaResena = null }) {
                    Text("Cancelar", color = ColorTextoSub)
                }
            }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// COMPONENTES REUTILIZABLES DEL CLIENTE
// ══════════════════════════════════════════════════════════════════════════════

// Tarjeta de próxima cita en la pestaña Inicio
@Composable
private fun ProximaCitaCard(cita: CitaDTO) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ClienteCard)
    ) {
        Column {
            // Borde superior rojo
            Box(modifier = Modifier.fillMaxWidth().height(3.dp)
                .background(Brush.horizontalGradient(
                    listOf(ClienteAccent, ClienteAccent.copy(0.3f)))))
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Próxima cita", color = ColorTextoSub, fontSize = 11.sp,
                        letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Cita #${cita.idCita}",
                        color = ColorTexto, fontWeight = FontWeight.Bold, fontSize = 16.sp
                    )
                    Text(
                        "${cita.fecha} · ${cita.horaInicio?.take(5)}",
                        color = ColorTextoSub, fontSize = 13.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ClienteAccentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.ContentCut, null,
                        tint = ClienteAccent, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

// Stat rápida en el inicio
@Composable
private fun StatCard(
    numero: String, label: String,
    color: Color, modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ClienteCard)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(numero, color = color, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(label, color = ColorTextoSub, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

// Chip de barbero en el inicio (horizontal scroll)
@Composable
private fun TarjetaBarberoChip(barbero: BarberoDTO) {
    Card(
        modifier = Modifier.width(130.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ClienteCard)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(ClienteAccentSoft),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    barbero.nombre.take(2).uppercase(),
                    color = ClienteAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(barbero.nombre, color = ColorTexto, fontSize = 13.sp,
                fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            barbero.especialidad?.let {
                Text(it, color = ColorTextoSub, fontSize = 11.sp,
                    textAlign = TextAlign.Center, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// Tarjeta de servicio en el inicio
@Composable
private fun TarjetaServicioCliente(servicio: ServicioDTO) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ClienteCard)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(servicio.nombre, color = ColorTexto,
                    fontWeight = FontWeight.Medium, fontSize = 14.sp)
                servicio.descripcion?.let {
                    Text(it, color = ColorTextoSub, fontSize = 12.sp, maxLines = 1)
                }
                Text("${servicio.duracionMinutos} min",
                    color = ColorTextoSub, fontSize = 12.sp)
            }
            Text(
                "\$${servicio.precio.toInt()}",
                color = ClienteAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp
            )
        }
    }
}

// Título de sección con línea decorativa roja
@Composable
private fun SeccionTitulo(texto: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.width(3.dp).height(18.dp)
            .clip(RoundedCornerShape(2.dp)).background(ClienteAccent))
        Text(texto, color = ColorTexto, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

// Título de paso con número y check animado
@Composable
private fun PasoTitulo(numero: String, texto: String, completado: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Círculo numerado o check
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (completado) ClienteAccent else ColorSuperficie2)
                .border(1.dp, if (completado) ClienteAccent else ColorBorde, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = completado,
                transitionSpec = { scaleIn() togetherWith scaleOut() },
                label = "pasoIcon$numero"
            ) { done ->
                if (done) {
                    Icon(Icons.Filled.Check, null,
                        tint = Color.White, modifier = Modifier.size(14.dp))
                } else {
                    Text(numero, color = ColorTextoSub,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(texto, color = if (completado) ColorTexto else ColorTextoSub,
            fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// Card de barbero en el selector de Agendar
@Composable
private fun BarberoSelectorCard(
    barbero: BarberoDTO,
    seleccionado: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "barberoScale"
    )
    Card(
        modifier = Modifier
            .width(120.dp)
            .scale(escala)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionado) ClienteAccent else ClienteCard
        ),
        border = if (seleccionado) null else
            androidx.compose.foundation.BorderStroke(1.dp, ColorBorde)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Person, null,
                tint = if (seleccionado) Color.White else ClienteAccent,
                modifier = Modifier.size(30.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(barbero.nombre,
                color = if (seleccionado) Color.White else ColorTexto,
                fontSize = 12.sp, fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center, maxLines = 2,
                overflow = TextOverflow.Ellipsis)
            barbero.especialidad?.let {
                Text(it,
                    color = if (seleccionado) Color.White.copy(0.7f) else ColorTextoSub,
                    fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// Card de servicio en el selector de Agendar
@Composable
private fun ServicioSelectorCard(
    servicio: ServicioDTO,
    seleccionado: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "servicioScale"
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(escala)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionado) ClienteAccent else ClienteCard
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(servicio.nombre,
                    color = if (seleccionado) Color.White else ColorTexto,
                    fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text("${servicio.duracionMinutos} min",
                    color = if (seleccionado) Color.White.copy(0.7f) else ColorTextoSub,
                    fontSize = 12.sp)
            }
            Text("\$${servicio.precio.toInt()}",
                color = if (seleccionado) Color.White else ClienteAccent,
                fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// Chip de hora disponible
@Composable
private fun HoraSelectorChip(
    hora: String,
    seleccionada: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "horaScale"
    )
    Box(
        modifier = Modifier
            .scale(escala)
            .clip(RoundedCornerShape(10.dp))
            .background(if (seleccionada) ClienteAccent else ColorSuperficie2)
            .border(1.dp,
                if (seleccionada) ClienteAccent else ColorBorde,
                RoundedCornerShape(10.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(hora,
            color = if (seleccionada) Color.White else ColorTexto,
            fontSize = 13.sp,
            fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Normal)
    }
}

// Fila del resumen de cita
@Composable
private fun ResumenFila(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(etiqueta, color = ColorTextoSub, fontSize = 13.sp)
        Text(valor, color = ColorTexto, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// Tarjeta de cita en Mis Citas con estado y botones
@Composable
private fun TarjetaCitaCliente(
    cita: CitaDTO,
    onCancelar: () -> Unit,
    onResena: () -> Unit
) {
    val colorEstado = when (cita.estado) {
        EstadoCitaEnum.PENDIENTE     -> Color(0xFFD4A017)
        EstadoCitaEnum.EN_CURSO      -> ColorAzulClaro
        EstadoCitaEnum.FINALIZADA    -> Color(0xFF3CB86A)
        EstadoCitaEnum.CANCELADA     -> ColorError
        EstadoCitaEnum.NO_PRESENTADO -> ColorTextoSub
        null                         -> ColorTextoSub
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ClienteCard)
    ) {
        Column {
            // Borde izquierdo de color según estado
            Row {
                Box(modifier = Modifier.width(4.dp).height(80.dp)
                    .background(colorEstado))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text("Cita #${cita.idCita}",
                                color = ColorTexto, fontWeight = FontWeight.Bold,
                                fontSize = 14.sp)
                            Text("${cita.fecha} · ${cita.horaInicio?.take(5)} – ${cita.horaFin?.take(5)}",
                                color = ColorTextoSub, fontSize = 12.sp)
                        }
                        // Badge de estado
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colorEstado.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = when (cita.estado) {
                                    EstadoCitaEnum.PENDIENTE     -> "Pendiente"
                                    EstadoCitaEnum.EN_CURSO      -> "En curso"
                                    EstadoCitaEnum.FINALIZADA    -> "Finalizada"
                                    EstadoCitaEnum.CANCELADA     -> "Cancelada"
                                    EstadoCitaEnum.NO_PRESENTADO -> "No se presentó"
                                    null                         -> ""
                                },
                                color = colorEstado,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Botones de acción según estado
            if (cita.estado == EstadoCitaEnum.PENDIENTE ||
                cita.estado == EstadoCitaEnum.FINALIZADA) {
                HorizontalDivider(color = ColorBorde)
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
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
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFD4A017))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dejar reseña",
                                color = Color(0xFFD4A017), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// Botón de ícono con animación de escala (logout, etc.)
@Composable
private fun BotonIcono(
    icono: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "iconBtnScale"
    )
    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier.scale(escala)
    ) {
        Icon(icono, null, tint = tint, modifier = Modifier.size(22.dp))
    }
}