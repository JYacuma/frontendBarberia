package com.example.barberia.ui.screens

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
import com.example.barberia.ui.auth.BarberiaBoton
import com.example.barberia.ui.auth.BarberiaTextField
import com.example.barberia.ui.theme.*
import com.example.barberia.viewmodel.BarberoViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BarberoScreen(
    apiService: ApiService,
    idBarbero: Long,
    idUsuario: Long,
    nombre: String,
    onLogout: () -> Unit
) {
    val colores = LocalBarberiaColores.current
    val sistemaOscuro = isSystemInDarkTheme()

    val viewModel: BarberoViewModel = viewModel(
        factory = BarberoViewModel.factory(apiService, idBarbero, idUsuario)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Swipe + tap sincronizados — igual que ClienteScreen
    val pagerState    = rememberPagerState(pageCount = { 3 })
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
                    actionColor    = ColorAzul,
                    shape          = RoundedCornerShape(12.dp)
                )
            }
        },
        bottomBar = {
            BarberoBottomBar(pagerState.currentPage, colores) { index ->
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
                0 -> HoyTab(nombre, uiState, viewModel, onLogout, colores, sistemaOscuro)
                1 -> AgendaTab(uiState, viewModel, colores)
                2 -> ResenasBarberoTab(uiState, colores)
            }
        }
    }
}

// ── Bottom Navigation Bar — acento azul ───────────────────────────────────────
@Composable
private fun BarberoBottomBar(
    paginaActual: Int,
    colores: BarberiaColores,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = colores.superficie,
        tonalElevation = 0.dp,
        modifier = if (!colores.esModoOscuro) Modifier.shadow(4.dp)
        else Modifier.border(
            1.dp, colores.borde,
            RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp))
    ) {
        listOf(
            Triple("Hoy",     Icons.Filled.Home,        0),
            Triple("Agenda",  Icons.Filled.CalendarMonth, 1),
            Triple("Reseñas", Icons.Filled.Star,        2)
        ).forEach { (label, icon, index) ->
            val activo = paginaActual == index
            NavigationBarItem(
                selected = activo,
                onClick  = { onTabSelected(index) },
                icon = {
                    Icon(icon, label,
                        tint = if (activo) ColorAzul else colores.textoSub,
                        modifier = Modifier.size(22.dp))
                },
                label = {
                    Text(label,
                        color = if (activo) ColorAzul else colores.textoSub,
                        fontSize = 11.sp,
                        fontWeight = if (activo) FontWeight.SemiBold else FontWeight.Normal)
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = ColorAzul.copy(alpha = 0.15f))
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 1 — HOY
// Muestra: agenda del día con acciones por cita (iniciar/finalizar/no presentó)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun HoyTab(
    nombre: String,
    uiState: com.example.barberia.viewmodel.BarberoUiState,
    viewModel: BarberoViewModel,
    onLogout: () -> Unit,
    colores: BarberiaColores,
    sistemaOscuro: Boolean
) {
    // Dialog de confirmación para acciones críticas
    var citaAccion     by remember { mutableStateOf<CitaDTO?>(null) }
    var tipoAccion     by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colores.fondo)
    ) {
        // ── Header azul ───────────────────────────────────────────────────
        item {
            Box(modifier = Modifier.fillMaxWidth().background(
                Brush.verticalGradient(
                    if (colores.esModoOscuro)
                        listOf(Color(0xFF020A1A), colores.fondo)
                    else
                        listOf(Color(0xFFF0F5FF), colores.fondo)
                )
            )) {
                // Franja polo — azul para barbero
                Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(
                    Brush.horizontalGradient(
                        listOf(ColorAzul, ColorBlanco, ColorRojo, ColorBlanco, ColorAzul)
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
                                    .background(ColorAzul))
                                Text("BARBERO", color = ColorAzul, fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("$nombre, hoy tienes", color = colores.texto,
                                fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            // Cuenta solo citas activas (no canceladas ni no presentadas)
                            val citasActivas = uiState.citasHoy.count {
                                it.estado != EstadoCitaEnum.CANCELADA &&
                                        it.estado != EstadoCitaEnum.NO_PRESENTADO
                            }
                            Text(
                                text = "$citasActivas cita${if (citasActivas != 1) "s" else ""} programadas",
                                color = ColorAzul,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Avatar inicial
                            Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                                .background(ColorAzul),
                                contentAlignment = Alignment.Center) {
                                Text(nombre.take(2).uppercase(), color = Color.White,
                                    fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            // Toggle modo claro/oscuro
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
                            BotonIconoBarbero(Icons.AutoMirrored.Filled.Logout,
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
                    CircularProgressIndicator(color = ColorAzul,
                        strokeWidth = 2.5.dp, modifier = Modifier.size(36.dp))
                }
            }
        } else {

            // Stats del día
            item {
                Row(modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatBarbero(
                        numero = uiState.citasHoy.count {
                            it.estado == EstadoCitaEnum.PENDIENTE }.toString(),
                        label = "Pendientes",
                        color = ColorDorado,
                        colores = colores,
                        modifier = Modifier.weight(1f)
                    )
                    StatBarbero(
                        numero = uiState.citasHoy.count {
                            it.estado == EstadoCitaEnum.EN_CURSO }.toString(),
                        label = "En curso",
                        color = ColorAzul,
                        colores = colores,
                        modifier = Modifier.weight(1f)
                    )
                    StatBarbero(
                        numero = uiState.citasHoy.count {
                            it.estado == EstadoCitaEnum.FINALIZADA }.toString(),
                        label = "Finalizadas",
                        color = ColorVerde,
                        colores = colores,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Agenda del día
            item {
                Row(modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    SeccionTituloBarbero("Agenda de hoy", colores)
                    TextButton(onClick = { viewModel.cargarDatosIniciales() }) {
                        Icon(Icons.Filled.Refresh, null,
                            tint = ColorAzul, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Actualizar", color = ColorAzul, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (uiState.citasHoy.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.EventAvailable, null,
                                tint = colores.textoSub, modifier = Modifier.size(44.dp))
                            Text("Sin citas para hoy",
                                color = colores.textoSub, fontSize = 15.sp)
                        }
                    }
                }
            } else {
                // Lista de citas del día con sus botones de acción
                items(uiState.citasHoy) { cita ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        TarjetaCitaBarbero(
                            cita    = cita,
                            colores = colores,
                            onIniciar    = { citaAccion = cita; tipoAccion = "iniciar" },
                            onFinalizar  = { citaAccion = cita; tipoAccion = "finalizar" },
                            onNoPresento = { citaAccion = cita; tipoAccion = "nopresento" }
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    // Dialog confirmación de acción
    citaAccion?.let { cita ->
        val (titulo, mensaje, colorBtn) = when (tipoAccion) {
            "iniciar"    -> Triple("Iniciar cita",
                "¿Confirmas que estás comenzando la cita #${cita.idCita}?", ColorAzul)
            "finalizar"  -> Triple("Finalizar cita",
                "¿Confirmas que terminaste la cita #${cita.idCita}?", ColorVerde)
            "nopresento" -> Triple("No se presentó",
                "¿El cliente de la cita #${cita.idCita} no se presentó?", ColorError)
            else -> Triple("", "", ColorAzul)
        }
        AlertDialog(
            onDismissRequest = { citaAccion = null },
            containerColor   = colores.superficie,
            shape            = RoundedCornerShape(20.dp),
            title = { Text(titulo, color = colores.texto, fontWeight = FontWeight.Bold) },
            text  = { Text(mensaje, color = colores.textoSub, fontSize = 14.sp) },
            confirmButton = {
                BarberiaBoton(
                    texto      = "Confirmar",
                    colorFondo = colorBtn,
                    onClick    = {
                        when (tipoAccion) {
                            "iniciar"    -> viewModel.iniciarCita(cita.idCita!!)
                            "finalizar"  -> viewModel.finalizarCita(cita.idCita!!)
                            "nopresento" -> viewModel.marcarNoPresento(cita.idCita!!)
                        }
                        citaAccion = null
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { citaAccion = null }) {
                    Text("Cancelar", color = colores.textoSub)
                }
            }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 2 — AGENDA (Bloquear horario)
// ══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgendaTab(
    uiState: com.example.barberia.viewmodel.BarberoUiState,
    viewModel: BarberoViewModel,
    colores: BarberiaColores
) {
    var fechaInicio    by remember { mutableStateOf("") }
    var fechaFin       by remember { mutableStateOf("") }
    var horaInicio     by remember { mutableStateOf("") }
    var horaFin        by remember { mutableStateOf("") }
    var motivo         by remember { mutableStateOf("") }

    // DatePicker para fecha inicio
    var mostrarCalInicio by remember { mutableStateOf(false) }
    var mostrarCalFin    by remember { mutableStateOf(false) }
    val datePickerInicio = rememberDatePickerState()
    val datePickerFin    = rememberDatePickerState()

    // Convierte millis a "YYYY-MM-DD"
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

    // DatePickerDialog inicio
    if (mostrarCalInicio) {
        DatePickerDialog(
            onDismissRequest = { mostrarCalInicio = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerInicio.selectedDateMillis?.let {
                        fechaInicio = millisAFecha(it) }
                    mostrarCalInicio = false
                }) { Text("Confirmar", color = ColorAzul, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalInicio = false }) {
                    Text("Cancelar", color = colores.textoSub) }
            },
            colors = datePickerColores(colores)
        ) { DatePicker(state = datePickerInicio, colors = datePickerColores(colores)) }
    }

    // DatePickerDialog fin
    if (mostrarCalFin) {
        DatePickerDialog(
            onDismissRequest = { mostrarCalFin = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerFin.selectedDateMillis?.let {
                        fechaFin = millisAFecha(it) }
                    mostrarCalFin = false
                }) { Text("Confirmar", color = ColorAzul, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalFin = false }) {
                    Text("Cancelar", color = colores.textoSub) }
            },
            colors = datePickerColores(colores)
        ) { DatePicker(state = datePickerFin, colors = datePickerColores(colores)) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colores.fondo)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Bloquear horario", color = colores.texto,
                fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Bloquea tiempo para descanso, vacaciones o imprevistos",
                color = colores.textoSub, fontSize = 13.sp)
        }

        // Formulario de bloqueo
        item {
            Card(modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colores.superficie),
                elevation = CardDefaults.cardElevation(defaultElevation = colores.sombra.dp)) {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(
                        Brush.horizontalGradient(listOf(ColorAzul, ColorAzulClaro))))
                    Column(modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)) {

                        Text("Nuevo bloqueo", color = colores.texto,
                            fontWeight = FontWeight.Bold, fontSize = 15.sp)

                        // Fecha inicio — abre calendario
                        CampoFechaBarbero("Fecha inicio",
                            fechaInicio, colores) { mostrarCalInicio = true }

                        // Hora inicio
                        BarberiaTextField(value = horaInicio,
                            onValueChange = { horaInicio = it },
                            label = "Hora inicio — ej: 09:00",
                            leadingIcon = Icons.Filled.Schedule,
                            accentColor = ColorAzul, colores = colores)

                        // Fecha fin — abre calendario
                        CampoFechaBarbero("Fecha fin",
                            fechaFin, colores) { mostrarCalFin = true }

                        // Hora fin
                        BarberiaTextField(value = horaFin,
                            onValueChange = { horaFin = it },
                            label = "Hora fin — ej: 18:00",
                            leadingIcon = Icons.Filled.Schedule,
                            accentColor = ColorAzul, colores = colores)

                        // Motivo
                        BarberiaTextField(value = motivo,
                            onValueChange = { motivo = it },
                            label = "Motivo (opcional)",
                            leadingIcon = Icons.Filled.Edit,
                            accentColor = ColorAzul, colores = colores)

                        val formListo = fechaInicio.length == 10 &&
                                fechaFin.length == 10 &&
                                horaInicio.length >= 5 &&
                                horaFin.length >= 5

                        BarberiaBoton(
                            texto      = "Bloquear horario",
                            icono      = Icons.Filled.Lock,
                            isLoading  = uiState.isLoading,
                            colorFondo = if (formListo) ColorAzul else colores.borde,
                            onClick    = {
                                if (formListo) {
                                    viewModel.crearBloqueo(
                                        "${fechaInicio}T${horaInicio}:00",
                                        "${fechaFin}T${horaFin}:00",
                                        motivo
                                    )
                                    fechaInicio = ""; fechaFin = ""
                                    horaInicio = ""; horaFin = ""; motivo = ""
                                }
                            }
                        )
                    }
                }
            }
        }

        // Lista de bloqueos activos
        item {
            SeccionTituloBarbero("Bloqueos activos", colores)
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (uiState.bloqueos.isEmpty()) {
            item {
                Text("Sin bloqueos activos", color = colores.textoSub, fontSize = 13.sp)
            }
        } else {
            items(uiState.bloqueos) { bloqueo ->
                TarjetaBloqueoBarbero(bloqueo, colores) {
                    bloqueo.idBloqueo?.let { viewModel.eliminarBloqueo(it) }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 3 — RESEÑAS
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun ResenasBarberoTab(
    uiState: com.example.barberia.viewmodel.BarberoUiState,
    colores: BarberiaColores
) {
    // Calificación promedio calculada en el cliente
    val promedio = if (uiState.resenas.isNotEmpty())
        uiState.resenas.mapNotNull { it.calificacion }.average() else 0.0

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colores.fondo)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Mis reseñas", color = colores.texto,
                fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        // Resumen promedio
        item {
            Card(modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colores.superficie),
                elevation = CardDefaults.cardElevation(defaultElevation = colores.sombra.dp)) {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(
                        Brush.horizontalGradient(listOf(ColorDorado, ColorDorado.copy(0.3f)))))
                    Row(modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Promedio", color = colores.textoSub,
                                fontSize = 12.sp, letterSpacing = 1.sp)
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("%.1f".format(promedio), color = ColorDorado,
                                    fontSize = 36.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Filled.Star, null,
                                    tint = ColorDorado, modifier = Modifier.size(28.dp))
                            }
                            Text("${uiState.resenas.size} reseña${if (uiState.resenas.size != 1) "s" else ""}",
                                color = colores.textoSub, fontSize = 12.sp)
                        }
                        // Mini distribución de estrellas
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            (5 downTo 1).forEach { estrellas ->
                                val cant = uiState.resenas.count {
                                    it.calificacion == estrellas }
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("$estrellas", color = colores.textoSub,
                                        fontSize = 11.sp)
                                    Icon(Icons.Filled.Star, null,
                                        tint = ColorDorado, modifier = Modifier.size(12.dp))
                                    Text("$cant", color = colores.texto, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.resenas.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.StarOutline, null,
                            tint = colores.textoSub, modifier = Modifier.size(48.dp))
                        Text("Aún no tienes reseñas",
                            color = colores.textoSub, fontSize = 15.sp)
                    }
                }
            }
        } else {
            items(uiState.resenas) { resena ->
                TarjetaResenaBarbero(resena, colores)
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// COMPONENTES
// ══════════════════════════════════════════════════════════════════════════════

// Tarjeta de cita con botones de acción según el estado
@Composable
private fun TarjetaCitaBarbero(
    cita: CitaDTO,
    colores: BarberiaColores,
    onIniciar: () -> Unit,
    onFinalizar: () -> Unit,
    onNoPresento: () -> Unit
) {
    val colorEstado = when (cita.estado) {
        EstadoCitaEnum.PENDIENTE     -> ColorDorado
        EstadoCitaEnum.EN_CURSO      -> ColorAzul
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
                // Barra lateral de color según estado
                Box(modifier = Modifier.width(4.dp).height(90.dp).background(colorEstado))
                Column(modifier = Modifier.weight(1f)
                    .padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top) {
                        Column {
                            // Hora de inicio destacada
                            Text(cita.horaInicio?.take(5) ?: "--:--",
                                color = ColorAzul, fontSize = 20.sp,
                                fontWeight = FontWeight.Bold)
                            Text("hasta ${cita.horaFin?.take(5) ?: "--:--"}",
                                color = colores.textoSub, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Cliente #${cita.idUsuario}",
                                color = colores.texto, fontSize = 14.sp,
                                fontWeight = FontWeight.Medium)
                            Text("Servicio #${cita.idServicio}",
                                color = colores.textoSub, fontSize = 12.sp)
                        }
                        // Badge de estado
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

            // Botones de acción — aparecen solo según el estado actual
            // PENDIENTE → puede iniciar o marcar no presentado
            // EN_CURSO  → puede finalizar
            if (cita.estado == EstadoCitaEnum.PENDIENTE ||
                cita.estado == EstadoCitaEnum.EN_CURSO) {
                HorizontalDivider(color = colores.borde)
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (cita.estado == EstadoCitaEnum.PENDIENTE) {
                        TextButton(onClick = onIniciar) {
                            Icon(Icons.Filled.PlayArrow, null,
                                modifier = Modifier.size(16.dp), tint = ColorAzul)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Iniciar", color = ColorAzul, fontSize = 13.sp)
                        }
                        TextButton(onClick = onNoPresento) {
                            Icon(Icons.Filled.PersonOff, null,
                                modifier = Modifier.size(16.dp), tint = ColorError)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("No se presentó", color = ColorError, fontSize = 13.sp)
                        }
                    }
                    if (cita.estado == EstadoCitaEnum.EN_CURSO) {
                        TextButton(onClick = onFinalizar) {
                            Icon(Icons.Filled.CheckCircle, null,
                                modifier = Modifier.size(16.dp), tint = ColorVerde)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Finalizar", color = ColorVerde, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// Tarjeta de bloqueo con botón eliminar
@Composable
private fun TarjetaBloqueoBarbero(
    bloqueo: BloqueoHorarioDTO,
    colores: BarberiaColores,
    onEliminar: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colores.superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = colores.sombra.dp)) {
        Row(modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                    .background(ColorAzul.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Lock, null,
                        tint = ColorAzul, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(bloqueo.motivo ?: "Sin motivo", color = colores.texto,
                        fontSize = 14.sp, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = "${bloqueo.fechaInicio?.take(10)} → ${bloqueo.fechaFin?.take(10)}",
                        color = colores.textoSub, fontSize = 12.sp
                    )
                }
            }
            IconButton(onClick = onEliminar) {
                Icon(Icons.Filled.DeleteOutline, null,
                    tint = ColorError, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// Tarjeta de reseña
@Composable
private fun TarjetaResenaBarbero(resena: ResenaDTO, colores: BarberiaColores) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colores.superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = colores.sombra.dp)) {
        Column(modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Cita #${resena.idCita}", color = colores.textoSub, fontSize = 12.sp)
                // Estrellas
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    (1..5).forEach { i ->
                        Icon(
                            imageVector = if (i <= (resena.calificacion ?: 0))
                                Icons.Filled.Star else Icons.Filled.StarOutline,
                            contentDescription = null,
                            tint = ColorDorado,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            if (!resena.comentario.isNullOrBlank()) {
                Text(resena.comentario, color = colores.texto, fontSize = 13.sp,
                    lineHeight = 18.sp)
            }
        }
    }
}

// Stat del barbero (3 en fila)
@Composable
private fun StatBarbero(
    numero: String, label: String, color: Color,
    colores: BarberiaColores, modifier: Modifier = Modifier
) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colores.superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = colores.sombra.dp)) {
        Column(modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(numero, color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(label, color = colores.textoSub, fontSize = 10.sp,
                textAlign = TextAlign.Center)
        }
    }
}

// Título de sección con barra azul
@Composable
private fun SeccionTituloBarbero(texto: String, colores: BarberiaColores) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.width(3.dp).height(18.dp)
            .clip(RoundedCornerShape(2.dp)).background(ColorAzul))
        Text(texto, color = colores.texto, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

// Campo de fecha que abre calendario al tocar
@Composable
private fun CampoFechaBarbero(
    label: String, valor: String,
    colores: BarberiaColores, onClick: () -> Unit
) {
    Column {
        Text(label, color = colores.textoSub, fontSize = 12.sp,
            letterSpacing = 0.5.sp, modifier = Modifier.padding(bottom = 6.dp))
        Box(modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colores.superficie)
            .border(1.dp,
                if (valor.isNotBlank()) ColorAzul else colores.borde,
                RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.CalendarMonth, null,
                    tint = ColorAzul, modifier = Modifier.size(20.dp))
                Text(
                    text = if (valor.isNotBlank()) valor else "Toca para abrir el calendario",
                    color = if (valor.isNotBlank()) colores.texto else colores.textoSub,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// Colores del DatePicker adaptados al tema — acento azul para barbero
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun datePickerColores(colores: BarberiaColores) = DatePickerDefaults.colors(
    containerColor            = colores.superficie,
    titleContentColor         = colores.texto,
    headlineContentColor      = ColorAzul,
    weekdayContentColor       = colores.textoSub,
    dayContentColor           = colores.texto,
    selectedDayContainerColor = ColorAzul,
    selectedDayContentColor   = Color.White,
    todayDateBorderColor      = ColorAzul,
    todayContentColor         = ColorAzul
)

// Botón icono con animación spring
@Composable
private fun BotonIconoBarbero(
    icono: ImageVector, tint: Color, onClick: () -> Unit
) {
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