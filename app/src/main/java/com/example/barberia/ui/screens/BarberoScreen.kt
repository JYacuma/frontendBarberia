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
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.barberia.model.*
import com.example.barberia.network.ApiService
import com.example.barberia.ui.theme.*
import com.example.barberia.viewmodel.BarberoViewModel

import kotlinx.coroutines.launch
import java.text.Normalizer

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BarberoScreen(
    apiService: ApiService,
    idBarbero: Long,
    idUsuario: Long,
    nombre: String,
    onLogout: () -> Unit,
    onNavigateToNotificaciones: () -> Unit = {}
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
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            BarberoHeader(nombre, uiState.notificacionesCount, colores, onLogout, onNavigateToNotificaciones)

            HorizontalPager(
                state             = pagerState,
                modifier          = Modifier.weight(1f),
                userScrollEnabled = true
            ) { pagina ->
                when (pagina) {
                    0 -> HoyTab(nombre, uiState, viewModel, colores)
                    1 -> AgendaTab(uiState, viewModel, colores)
                    2 -> ResenasBarberoTab(uiState, colores)
                    3 -> HorariosBarberoTab(uiState, colores)
                }
            }
        }
    }
}

// ── Header compartido ───────────────────────────────────────────────────────
@Composable
private fun BarberoHeader(
    nombre: String,
    notificacionesCount: Int,
    colores: BarberiaColores,
    onLogout: () -> Unit,
    onNavigateToNotificaciones: () -> Unit = {}
)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .zIndex(1f)
                .background(
                    Brush.horizontalGradient(
                        listOf(ColorAzul, ColorBlanco, ColorRojo, ColorBlanco, ColorAzul)
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(ColorAzul),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getInitials(nombre),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Column {
                    Text(nombre, color = colores.texto, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Barbero", color = ColorAzul, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box {
                    BotonIconoBarbero(Icons.Filled.Notifications, colores.textoSub, onClick = onNavigateToNotificaciones)
                    if (notificacionesCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(ColorError),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (notificacionesCount > 99) "99+" else notificacionesCount.toString(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
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
                BotonIconoBarbero(Icons.AutoMirrored.Filled.Logout, colores.textoSub, onLogout)
            }
        }
    }
}

// ── Bottom Navigation Bar ────────────────────────────────────────────────────
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
            Triple("Hoy",      Icons.Filled.Today,          0),
            Triple("Agenda",   Icons.Filled.CalendarMonth,  1),
            Triple("Reseñas",  Icons.Filled.Star,           2),
            Triple("Horarios", Icons.Filled.Schedule,       3)
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
// TAB 0 — HOY
// ══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HoyTab(
    nombre: String,
    uiState: com.example.barberia.viewmodel.BarberoUiState,
    viewModel: BarberoViewModel,
    colores: BarberiaColores
) {
    var filtroEstado by remember { mutableStateOf<EstadoCitaEnum?>(null) }
    var citaAccion     by remember { mutableStateOf<CitaDTO?>(null) }
    var tipoAccion     by remember { mutableStateOf("") }
    var citaDetalle    by remember { mutableStateOf<CitaConDetalle?>(null) }

    val citasFiltradas = if (filtroEstado != null) {
        uiState.citasHoy.filter { it.estado == filtroEstado }
    } else {
        uiState.citasHoy
    }

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.cargarDatosIniciales() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(colores.fondo)
        ) {
            item {
                Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ColorAzul))
                        Text("BARBERO", color = ColorAzul, fontSize = 10.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("$nombre, hoy tienes", color = colores.texto,
                        fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    val citasActivas = uiState.citasHoy.count {
                        it.estado != EstadoCitaEnum.CANCELADA &&
                                it.estado != EstadoCitaEnum.NO_PRESENTADO
                    }
                    Text(
                        text = "$citasActivas cita${if (citasActivas != 1) "s" else ""} programadas",
                        color = ColorAzul, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (uiState.isLoading && uiState.citasHoy.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ColorAzul,
                            strokeWidth = 2.5.dp, modifier = Modifier.size(36.dp))
                    }
                }
            } else {
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
                            activo = filtroEstado == EstadoCitaEnum.PENDIENTE,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                filtroEstado = if (filtroEstado == EstadoCitaEnum.PENDIENTE) null
                                else EstadoCitaEnum.PENDIENTE
                            }
                        )
                        StatBarbero(
                            numero = uiState.citasHoy.count {
                                it.estado == EstadoCitaEnum.EN_CURSO }.toString(),
                            label = "En curso",
                            color = ColorAzul,
                            colores = colores,
                            activo = filtroEstado == EstadoCitaEnum.EN_CURSO,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                filtroEstado = if (filtroEstado == EstadoCitaEnum.EN_CURSO) null
                                else EstadoCitaEnum.EN_CURSO
                            }
                        )
                        StatBarbero(
                            numero = uiState.citasHoy.count {
                                it.estado == EstadoCitaEnum.FINALIZADA }.toString(),
                            label = "Finalizadas",
                            color = ColorVerde,
                            colores = colores,
                            activo = filtroEstado == EstadoCitaEnum.FINALIZADA,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                filtroEstado = if (filtroEstado == EstadoCitaEnum.FINALIZADA) null
                                else EstadoCitaEnum.FINALIZADA
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

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

                if (citasFiltradas.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp),
                            contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.EventAvailable, null,
                                    tint = colores.textoSub, modifier = Modifier.size(44.dp))
                                Text(
                                    if (filtroEstado != null) "Sin citas en este filtro"
                                    else "Sin citas para hoy",
                                    color = colores.textoSub, fontSize = 15.sp)
                            }
                        }
                    }
                } else {
                    items(citasFiltradas) { cita ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                            TarjetaCitaBarbero(
                                cita    = cita,
                                colores = colores,
                                onClick = {
                                    citaDetalle = uiState.citasConDetalle.find {
                                        it.cita.idCita == cita.idCita
                                    } ?: CitaConDetalle(cita, "Cliente #${cita.idUsuario}",
                                        "Barbero", "Servicio #${cita.idServicio}")
                                },
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
    }

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
                BotonBarberoAnimado(
                    text = "Confirmar",
                    color = colorBtn,
                    onClick = {
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

    citaDetalle?.let { detalle ->
        AlertDialog(
            onDismissRequest = { citaDetalle = null },
            containerColor   = colores.superficie,
            shape            = RoundedCornerShape(20.dp),
            title = {
                Text("Detalle de cita", color = colores.texto, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilaDetalle("Fecha", detalle.cita.fecha ?: "--", colores)
                    FilaDetalle("Hora", "${detalle.cita.horaInicio?.take(5) ?: "--:--"} - ${detalle.cita.horaFin?.take(5) ?: "--:--"}", colores)
                    FilaDetalle("Cliente", detalle.clienteNombre, colores)
                    FilaDetalle("Barbero", detalle.barberoNombre, colores)
                    FilaDetalle("Servicio", detalle.servicioNombre, colores)
                    val estadoTxt = when (detalle.cita.estado) {
                        EstadoCitaEnum.PENDIENTE -> "Pendiente"
                        EstadoCitaEnum.EN_CURSO -> "En curso"
                        EstadoCitaEnum.FINALIZADA -> "Finalizada"
                        EstadoCitaEnum.CANCELADA -> "Cancelada"
                        EstadoCitaEnum.NO_PRESENTADO -> "No se presentó"
                        null -> "--"
                    }
                    FilaDetalle("Estado", estadoTxt, colores)
                }
            },
            confirmButton = {
                TextButton(onClick = { citaDetalle = null }) {
                    Text("Cerrar", color = ColorAzul)
                }
            }
        )
    }
}

@Composable
private fun FilaDetalle(label: String, valor: String, colores: BarberiaColores) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = colores.textoSub, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(valor, color = colores.texto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 1 — AGENDA (selector de día y bloque de descanso)
// ══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgendaTab(
    uiState: com.example.barberia.viewmodel.BarberoUiState,
    viewModel: BarberoViewModel,
    colores: BarberiaColores
) {
    val diasSemana = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    var selectedDay by remember { mutableStateOf<String?>(null) }
    var selectedBlock by remember { mutableStateOf<String?>(null) }

    val bloques30 = remember {
        val blocks = mutableListOf<String>()
        for (h in 9 until 19) {
            blocks.add("%02d:00-%02d:30".format(h, h))
            blocks.add("%02d:30-%02d:00".format(h, h + 1))
        }
        blocks
    }

    fun bloqueOcupado(bloque: String): Boolean {
        val parts = bloque.split("-")
        if (parts.size != 2) return false
        return uiState.bloqueos.any { b ->
            val ini = b.fechaInicio?.substringAfter("T")?.take(5) ?: ""
            val fin = b.fechaFin?.substringAfter("T")?.take(5) ?: ""
            ini <= parts[1] && fin >= parts[0]
        } || uiState.citasHoy.any { c ->
            val ci = c.horaInicio?.take(5) ?: ""
            val cf = c.horaFin?.take(5) ?: ""
            ci < parts[1] && cf > parts[0]
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colores.fondo)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Bloquear descanso", color = colores.texto,
                fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Selecciona un día y un bloque de 30 min para descanso",
                color = colores.textoSub, fontSize = 13.sp)
        }

        item {
            Text("Día de la semana", color = colores.texto,
                fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                diasSemana.forEach { dia ->
                    val seleccionado = selectedDay == dia
                    FilterChip(
                        selected = seleccionado,
                        onClick = {
                            selectedDay = if (seleccionado) null else dia
                            selectedBlock = null
                        },
                        label = { Text(dia, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ColorAzul,
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (seleccionado) ColorAzul else colores.borde,
                            selectedBorderColor = ColorAzul,
                            enabled = true,
                            selected = seleccionado
                        )
                    )
                }
            }
        }

        if (selectedDay != null) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Bloques de 30 min", color = colores.texto,
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                val chunks = bloques30.chunked(4)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chunks.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            row.forEach { bloque ->
                                val ocupado = bloqueOcupado(bloque)
                                val seleccionado = selectedBlock == bloque
                                FilterChip(
                                    selected = seleccionado,
                                    onClick = {
                                        if (!ocupado) {
                                            selectedBlock = if (seleccionado) null else bloque
                                        }
                                    },
                                    label = {
                                        Text(
                                            if (ocupado) "$bloque Ocupado" else bloque,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    enabled = !ocupado,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ColorAzul,
                                        selectedLabelColor = Color.White,
                                        containerColor = if (ocupado)
                                            colores.superficie.copy(alpha = 0.5f)
                                        else colores.superficie,
                                        labelColor = if (ocupado) colores.textoSub else colores.texto
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = if (ocupado) ColorError.copy(alpha = 0.3f)
                                        else if (seleccionado) ColorAzul else colores.borde,
                                        selectedBorderColor = ColorAzul,
                                        enabled = !ocupado,
                                        selected = seleccionado
                                    )
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                BotonBarberoAnimado(
                    text = "Guardar bloque de descanso",
                    icon = Icons.Filled.Lock,
                    color = if (selectedBlock != null) ColorAzul else colores.borde,
                    enabled = selectedBlock != null,
                    colores = colores,
                    onClick = {
                        selectedDay?.let { d ->
                            selectedBlock?.let { b ->
                                viewModel.guardarBloqueoDescanso(d, b)
                                selectedBlock = null
                                selectedDay = null
                            }
                        }
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
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
// TAB 2 — RESEÑAS agrupadas por cliente
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun ResenasBarberoTab(
    uiState: com.example.barberia.viewmodel.BarberoUiState,
    colores: BarberiaColores
) {
    val grouped = remember(uiState.resenasConCliente) {
        uiState.resenasConCliente.groupBy({ it.second }, { it.first })
    }

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
                                Text("%.1f".format(uiState.promedio), color = ColorDorado,
                                    fontSize = 36.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Filled.Star, null,
                                    tint = ColorDorado, modifier = Modifier.size(28.dp))
                            }
                            Text("${uiState.resenas.size} reseña${if (uiState.resenas.size != 1) "s" else ""}",
                                color = colores.textoSub, fontSize = 12.sp)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            (5 downTo 1).forEach { estrellas ->
                                val cant = uiState.resenas.count {
                                    it.calificacion == estrellas }
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("$estrellas", color = colores.textoSub, fontSize = 11.sp)
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

        if (grouped.isEmpty()) {
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
            grouped.forEach { (cliente, resenas) ->
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(cliente, color = ColorAzul, fontSize = 15.sp,
                        fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                }
                items(resenas) { resena ->
                    TarjetaResenaBarbero(resena, colores, cliente)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// TAB 3 — MIS HORARIOS
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun HorariosBarberoTab(
    uiState: com.example.barberia.viewmodel.BarberoUiState,
    colores: BarberiaColores
) {
    val diaCompleto = mapOf(
        DiaSemanaEnum.LUNES to "Lunes", DiaSemanaEnum.MARTES to "Martes",
        DiaSemanaEnum.MIERCOLES to "Miércoles", DiaSemanaEnum.JUEVES to "Jueves",
        DiaSemanaEnum.VIERNES to "Viernes", DiaSemanaEnum.SABADO to "Sábado",
        DiaSemanaEnum.DOMINGO to "Domingo"
    )

    val diasConHorario = uiState.horarios.map { it.diaSemana }.toSet()
    val todosDias = DiaSemanaEnum.entries.map { dia ->
        val horariosDia = uiState.horarios.filter { it.diaSemana == dia }
        val esDescanso = horariosDia.isEmpty()
        Triple(dia, horariosDia, esDescanso)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colores.fondo)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Mi Horario", color = colores.texto,
                fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Horarios configurados para la semana",
                color = colores.textoSub, fontSize = 13.sp)
        }

        if (uiState.horarios.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Schedule, null,
                            tint = colores.textoSub, modifier = Modifier.size(48.dp))
                        Text("Sin horarios configurados",
                            color = colores.textoSub, fontSize = 15.sp)
                        Text("El administrador debe configurar tus horarios",
                            color = colores.textoSub.copy(0.7f), fontSize = 12.sp)
                    }
                }
            }
        } else {
            todosDias.forEach { (dia, horariosDia, esDescanso) ->
                item {
                    if (esDescanso) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = ColorError.copy(alpha = 0.08f)),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = colores.sombra.dp)
                        ) {
                            Row(modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Filled.Bedtime, null,
                                    tint = ColorError, modifier = Modifier.size(20.dp))
                                Text(diaCompleto[dia] ?: dia.name,
                                    color = ColorError, fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.weight(1f))
                                Text("Día de descanso", color = ColorError.copy(alpha = 0.7f),
                                    fontSize = 11.sp)
                            }
                        }
                    } else {
                        Text(diaCompleto[dia] ?: dia.name,
                            color = ColorAzul, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp))
                    }
                }

                if (!esDescanso) {
                    for (horario in horariosDia) {
                        item {
                            val bloquesDelDia = remember(horario.horaInicio, horario.horaFin) {
                                val ini = horario.horaInicio?.take(5) ?: "09:00"
                                val fin = horario.horaFin?.take(5) ?: "19:00"
                                val startH = ini.substringBefore(":").toIntOrNull() ?: 9
                                val startM = ini.substringAfter(":").toIntOrNull() ?: 0
                                val endH = fin.substringBefore(":").toIntOrNull() ?: 19
                                val endM = fin.substringAfter(":").toIntOrNull() ?: 0
                                val blks = mutableListOf<String>()
                                var h = startH
                                var m = startM
                                while (h < endH || (h == endH && m < endM)) {
                                    val nextH = if (m == 0) h else h + 1
                                    val nextM = if (m == 0) 30 else 0
                                    blks.add("%02d:%02d-%02d:%02d".format(h, m, nextH, nextM))
                                    h = nextH; m = nextM
                                }
                                blks
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(14.dp)),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = colores.superficie)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("${horario.horaInicio?.take(5)} - ${horario.horaFin?.take(5)}",
                                        color = colores.texto,
                                        fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val blockChunks = bloquesDelDia.chunked(4)
                                    blockChunks.forEach { row ->
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                        ) {
                                            row.forEach { blk ->
                                                val ocupado = uiState.citasConDetalle.any { det ->
                                                    val ci = det.cita.horaInicio?.take(5) ?: ""
                                                    val cf = det.cita.horaFin?.take(5) ?: ""
                                                    ci < blk.substringAfter("-").take(5) &&
                                                            cf > blk.take(5)
                                                }
                                                val esBloqueo = uiState.bloqueos.any { b ->
                                                    val bi = b.fechaInicio?.substringAfter("T")?.take(5) ?: ""
                                                    val bf = b.fechaFin?.substringAfter("T")?.take(5) ?: ""
                                                    bi <= blk.substringAfter("-").take(5) && bf >= blk.take(5)
                                                }
                                                val detOcupado = if (ocupado) {
                                                    uiState.citasConDetalle.find { det ->
                                                        val ci = det.cita.horaInicio?.take(5) ?: ""
                                                        val cf = det.cita.horaFin?.take(5) ?: ""
                                                        ci < blk.substringAfter("-").take(5) &&
                                                                cf > blk.take(5)
                                                    }
                                                } else null

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(
                                                            when {
                                                                esBloqueo -> ColorAzul.copy(alpha = 0.2f)
                                                                ocupado -> ColorVerde.copy(alpha = 0.15f)
                                                                else -> colores.superficie2
                                                            }
                                                        )
                                                        .border(
                                                            0.5.dp,
                                                            when {
                                                                esBloqueo -> ColorAzul.copy(alpha = 0.4f)
                                                                ocupado -> ColorVerde.copy(alpha = 0.3f)
                                                                else -> colores.borde
                                                            },
                                                            RoundedCornerShape(6.dp)
                                                        )
                                                        .padding(horizontal = 4.dp, vertical = 3.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (esBloqueo) {
                                                        Text("Descanso", color = ColorAzul,
                                                            fontSize = 8.sp, fontWeight = FontWeight.Medium,
                                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    } else if (detOcupado != null) {
                                                        Text(
                                                            "${detOcupado.clienteNombre.take(6)} - ${detOcupado.servicioNombre.take(8)}",
                                                            color = ColorVerde, fontSize = 7.sp,
                                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    } else {
                                                        Text(blk, color = colores.textoSub,
                                                            fontSize = 7.sp,
                                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    }
                                                }
                                            }
                                        }
                                    }
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

// ══════════════════════════════════════════════════════════════════════════════
// COMPONENTES
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TarjetaCitaBarbero(
    cita: CitaDTO,
    colores: BarberiaColores,
    onClick: () -> Unit,
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colores.superficie)
    ) {
        Column {
            Row {
                Box(modifier = Modifier.width(4.dp).height(90.dp).background(colorEstado))
                Column(modifier = Modifier.weight(1f)
                    .padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top) {
                        Column {
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
                cita.estado == EstadoCitaEnum.EN_CURSO) {
                HorizontalDivider(color = colores.borde)
                Row(modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (cita.estado == EstadoCitaEnum.PENDIENTE) {
                        BotonBarberoAnimado(
                            text = "Iniciar",
                            icon = Icons.Filled.PlayArrow,
                            color = ColorAzul,
                            colores = colores,
                            modifier = Modifier.weight(1f),
                            onClick = onIniciar
                        )
                        BotonBarberoAnimado(
                            text = "No presentó",
                            icon = Icons.Filled.PersonOff,
                            color = ColorError,
                            colores = colores,
                            modifier = Modifier.weight(1f),
                            onClick = onNoPresento
                        )
                    }
                    if (cita.estado == EstadoCitaEnum.EN_CURSO) {
                        BotonBarberoAnimado(
                            text = "Finalizar",
                            icon = Icons.Filled.CheckCircle,
                            color = ColorVerde,
                            colores = colores,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onFinalizar
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaBloqueoBarbero(
    bloqueo: BloqueoHorarioDTO,
    colores: BarberiaColores,
    onEliminar: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colores.superficie)) {
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
                        text = "${bloqueo.fechaInicio?.take(10)} \u2192 ${bloqueo.fechaFin?.take(10)}",
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

@Composable
private fun TarjetaResenaBarbero(
    resena: ResenaDTO,
    colores: BarberiaColores,
    clienteNombre: String = "Cliente"
) {
    Card(modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colores.superficie)) {
        Column(modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(clienteNombre, color = colores.texto,
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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

@Composable
private fun StatBarbero(
    numero: String, label: String, color: Color,
    colores: BarberiaColores, modifier: Modifier = Modifier,
    activo: Boolean = false, onClick: (() -> Unit)? = null
) {
    val bgColor = if (activo) color.copy(alpha = 0.12f) else colores.superficie
    val borderMod = if (activo) Modifier.border(1.5.dp, color, RoundedCornerShape(12.dp))
    else Modifier

    Card(
        modifier = modifier
            .then(borderMod)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(numero, color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(label, color = if (activo) color else colores.textoSub, fontSize = 10.sp,
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SeccionTituloBarbero(texto: String, colores: BarberiaColores) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.width(3.dp).height(18.dp)
            .clip(RoundedCornerShape(2.dp)).background(ColorAzul))
        Text(texto, color = colores.texto, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BotonBarberoAnimado(
    text: String,
    color: Color,
    colores: BarberiaColores? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btnScale"
    )
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .height(52.dp)
            .scale(scale),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            disabledContainerColor = colores?.borde ?: color.copy(alpha = 0.4f)
        )
    ) {
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

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





