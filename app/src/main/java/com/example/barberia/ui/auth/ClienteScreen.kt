package com.example.barberia.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.barberia.model.*
import com.example.barberia.network.ApiService
import com.example.barberia.viewmodel.ClienteViewModel



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

    // Tab actual: 0=Inicio, 1=Agendar, 2=Mis Citas
    var tabSeleccionado by remember { mutableStateOf(0) }

    // Snackbar para mensajes
    val snackbarHostState = remember { SnackbarHostState() }

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
        containerColor = ColorFondo,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Bottom Navigation Bar
            NavigationBar(containerColor = Color(0xFF1A1A1A)) {
                listOf(
                    Triple("Inicio", Icons.Filled.Home, 0),
                    Triple("Agendar", Icons.Filled.CalendarMonth, 1),
                    Triple("Mis Citas", Icons.Filled.List, 2)
                ).forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = tabSeleccionado == index,
                        onClick = { tabSeleccionado = index },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (tabSeleccionado == index) ColorDorado else ColorTextoSub
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                color = if (tabSeleccionado == index) ColorDorado else ColorTextoSub,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0xFF252525)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (tabSeleccionado) {
                0 -> InicioTab(nombre, uiState, onLogout)
                1 -> AgendarTab(uiState, viewModel)
                2 -> MisCitasTab(uiState, viewModel)
            }
        }
    }
}

// ── Tab Inicio ────────────────────────────────────────────────────────────
@Composable
fun InicioTab(
    nombre: String,
    uiState: com.example.barberia.viewmodel.ClienteUiState,
    onLogout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorFondo)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            // Header saludo + botón cerrar sesión
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Hola,", color = ColorTextoSub, fontSize = 14.sp)
                    Text(
                        nombre,
                        color = ColorTexto,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onLogout) {
                    Icon(
                        Icons.Filled.Logout,
                        contentDescription = "Cerrar sesión",
                        tint = ColorTextoSub
                    )
                }
            }
        }

        item {
            // Banner dorado
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ColorDorado)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.ContentCut,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Reserva tu cita",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            "Elige tu barbero favorito",
                            color = Color.Black.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        item {
            Text(
                "Nuestros Barberos",
                color = ColorTexto,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        item {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ColorDorado)
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(uiState.barberos) { barbero ->
                        TarjetaBarbero(barbero)
                    }
                }
            }
        }

        item {
            Text(
                "Servicios",
                color = ColorTexto,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        items(uiState.servicios) { servicio ->
            TarjetaServicio(servicio)
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ── Tab Agendar ───────────────────────────────────────────────────────────
@Composable
fun AgendarTab(
    uiState: com.example.barberia.viewmodel.ClienteUiState,
    viewModel: ClienteViewModel
) {
    var barberoSeleccionado by remember { mutableStateOf<BarberoDTO?>(null) }
    var servicioSeleccionado by remember { mutableStateOf<ServicioDTO?>(null) }
    var fechaSeleccionada by remember { mutableStateOf("") }
    var horaSeleccionada by remember { mutableStateOf("") }

    // Formatea la fecha de hoy como string para el campo
    val hoy = java.util.Calendar.getInstance().let {
        "%d-%02d-%02d".format(
            it.get(java.util.Calendar.YEAR),
            it.get(java.util.Calendar.MONTH) + 1,
            it.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorFondo)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Agendar Cita",
                color = ColorTexto,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Selector de barbero
        item {
            Text("1. Elige tu barbero", color = ColorTextoSub, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.barberos) { barbero ->
                    val seleccionado = barberoSeleccionado?.idBarbero == barbero.idBarbero
                    Card(
                        modifier = Modifier
                            .width(120.dp)
                            .clickable {
                                barberoSeleccionado = barbero
                                horaSeleccionada = ""
                                if (fechaSeleccionada.isNotBlank()) {
                                    viewModel.cargarDisponibilidad(
                                        barbero.idBarbero!!, fechaSeleccionada
                                    )
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (seleccionado) ColorDorado
                            else Color(0xFF1A1A1A)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                tint = if (seleccionado) Color.Black else ColorDorado,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                barbero.nombre,
                                color = if (seleccionado) Color.Black else ColorTexto,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            barbero.especialidad?.let {
                                Text(
                                    it,
                                    color = if (seleccionado) Color.Black.copy(0.7f)
                                    else ColorTextoSub,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Selector de servicio
        item {
            Text("2. Elige el servicio", color = ColorTextoSub, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            uiState.servicios.forEach { servicio ->
                val seleccionado = servicioSeleccionado?.idServicio == servicio.idServicio
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { servicioSeleccionado = servicio },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (seleccionado) ColorDorado else Color(0xFF1A1A1A)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                servicio.nombre,
                                color = if (seleccionado) Color.Black else ColorTexto,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Text(
                                "${servicio.duracionMinutos} min",
                                color = if (seleccionado) Color.Black.copy(0.7f)
                                else ColorTextoSub,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            "$${servicio.precio.toInt()}",
                            color = if (seleccionado) Color.Black else ColorDorado,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        // Campo de fecha
        item {
            Text("3. Fecha", color = ColorTextoSub, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            BarberiaTextField(
                value = fechaSeleccionada,
                onValueChange = { nuevaFecha ->
                    fechaSeleccionada = nuevaFecha
                    horaSeleccionada = ""
                    barberoSeleccionado?.idBarbero?.let { id ->
                        if (nuevaFecha.length == 10) {
                            viewModel.cargarDisponibilidad(id, nuevaFecha)
                        }
                    }
                },
                label = "Fecha (yyyy-MM-dd) ej: $hoy",
                leadingIcon = Icons.Filled.CalendarMonth
            )
        }

        // Selector de hora
        if (uiState.horasDisponibles.isNotEmpty()) {
            item {
                Text("4. Hora disponible", color = ColorTextoSub, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.horasDisponibles) { hora ->
                        val horaCorta = hora.substring(0, 5)
                        val seleccionada = horaSeleccionada == hora
                        Card(
                            modifier = Modifier.clickable { horaSeleccionada = hora },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (seleccionada) ColorDorado
                                else Color(0xFF1A1A1A)
                            )
                        ) {
                            Text(
                                horaCorta,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                color = if (seleccionada) Color.Black else ColorTexto,
                                fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
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

            Button(
                onClick = {
                    viewModel.agendarCita(
                        idBarbero = barberoSeleccionado!!.idBarbero!!,
                        idServicio = servicioSeleccionado!!.idServicio!!,
                        fecha = fechaSeleccionada,
                        horaInicio = horaSeleccionada
                    )
                },
                enabled = listo && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorDorado,
                    disabledContainerColor = ColorDorado.copy(alpha = 0.3f)
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Confirmar Cita",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Tab Mis Citas ─────────────────────────────────────────────────────────
@Composable
fun MisCitasTab(
    uiState: com.example.barberia.viewmodel.ClienteUiState,
    viewModel: ClienteViewModel
) {
    var citaParaResena by remember { mutableStateOf<CitaDTO?>(null) }
    var calificacion by remember { mutableStateOf(5) }
    var comentario by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorFondo)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Mis Citas",
                color = ColorTexto,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (uiState.citas.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = ColorTextoSub,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No tienes citas aún",
                            color = ColorTextoSub,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        items(uiState.citas) { cita ->
            TarjetaCita(
                cita = cita,
                onCancelar = { viewModel.cancelarCita(cita.idCita!!) },
                onResena = { citaParaResena = cita }
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    // Dialog de reseña
    citaParaResena?.let { cita ->
        AlertDialog(
            onDismissRequest = { citaParaResena = null },
            containerColor = Color(0xFF1A1A1A),
            title = {
                Text("Dejar Reseña", color = ColorTexto, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Calificación", color = ColorTextoSub, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { estrella ->
                            Icon(
                                imageVector = if (estrella <= calificacion)
                                    Icons.Filled.Star else Icons.Filled.StarOutline,
                                contentDescription = null,
                                tint = ColorDorado,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { calificacion = estrella }
                            )
                        }
                    }
                    BarberiaTextField(
                        value = comentario,
                        onValueChange = { comentario = it },
                        label = "Comentario (opcional)",
                        leadingIcon = Icons.Filled.Comment
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.enviarResena(
                        idCita = cita.idCita!!,
                        idBarbero = cita.idBarbero,
                        calificacion = calificacion,
                        comentario = comentario
                    )
                    citaParaResena = null
                    comentario = ""
                    calificacion = 5
                }) {
                    Text("Enviar", color = ColorDorado, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { citaParaResena = null }) {
                    Text("Cancelar", color = ColorTextoSub)
                }
            }
        )
    }
}

// ── Componentes reutilizables ─────────────────────────────────────────────

@Composable
fun TarjetaBarbero(barbero: BarberoDTO) {
    Card(
        modifier = Modifier.width(130.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = ColorDorado,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                barbero.nombre,
                color = ColorTexto,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            barbero.especialidad?.let {
                Text(
                    it,
                    color = ColorTextoSub,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun TarjetaServicio(servicio: ServicioDTO) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    servicio.nombre,
                    color = ColorTexto,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                servicio.descripcion?.let {
                    Text(it, color = ColorTextoSub, fontSize = 12.sp, maxLines = 1)
                }
                Text("${servicio.duracionMinutos} min", color = ColorTextoSub, fontSize = 12.sp)
            }
            Text(
                "$${servicio.precio.toInt()}",
                color = ColorDorado,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun TarjetaCita(
    cita: CitaDTO,
    onCancelar: () -> Unit,
    onResena: () -> Unit
) {
    val colorEstado = when (cita.estado) {
        EstadoCitaEnum.PENDIENTE    -> Color(0xFFFFA726)
        EstadoCitaEnum.EN_CURSO     -> Color(0xFF42A5F5)
        EstadoCitaEnum.FINALIZADA   -> Color(0xFF66BB6A)
        EstadoCitaEnum.CANCELADA    -> Color(0xFFEF5350)
        EstadoCitaEnum.NO_PRESENTADO -> Color(0xFF9E9E9E)
        null                        -> ColorTextoSub
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Cita #${cita.idCita}",
                        color = ColorTexto,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        "${cita.fecha} • ${cita.horaInicio?.substring(0, 5)}",
                        color = ColorTextoSub,
                        fontSize = 12.sp
                    )
                }
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorEstado.copy(alpha = 0.15f)
                    )
                ) {
                    Text(
                        cita.estado?.name ?: "",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = colorEstado,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Botones según estado
            if (cita.estado == EstadoCitaEnum.PENDIENTE) {
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(
                    onClick = onCancelar,
                    colors = ButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = ColorError,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = ColorError.copy(0.5f)
                    )
                ) {
                    Icon(
                        Icons.Filled.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancelar cita", fontSize = 13.sp)
                }
            }

            if (cita.estado == EstadoCitaEnum.FINALIZADA) {
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(
                    onClick = onResena,
                    colors = ButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = ColorDorado,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = ColorDorado.copy(0.5f)
                    )
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dejar reseña", fontSize = 13.sp)
                }
            }
        }
    }
}