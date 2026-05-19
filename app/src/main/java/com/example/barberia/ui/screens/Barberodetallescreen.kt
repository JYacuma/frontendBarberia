package com.example.barberia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.barberia.model.*
import com.example.barberia.network.ApiService
import com.example.barberia.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ── ViewModel inline — solo para esta pantalla ────────────────────────────────
data class BarberoDetalleUiState(
    val isLoading: Boolean = false,
    val barbero: BarberoDTO? = null,
    val horarios: List<HorarioBarberoDTO> = emptyList(),
    val resenas: List<ResenaDTO> = emptyList(),
    val errorMessage: String? = null
)

class BarberoDetalleViewModel(
    private val apiService: ApiService,
    private val idBarbero: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(BarberoDetalleUiState())
    val uiState: StateFlow<BarberoDetalleUiState> = _uiState

    init { cargar() }

    fun cargar() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val barbero  = apiService.getBarberoById(idBarbero)
                val horarios = apiService.getHorariosByBarbero(idBarbero)
                val resenas  = apiService.getResenasByBarbero(idBarbero)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    barbero   = if (barbero.isSuccessful) barbero.body() else null,
                    horarios  = if (horarios.isSuccessful)
                        horarios.body() ?: emptyList() else emptyList(),
                    resenas   = if (resenas.isSuccessful)
                        resenas.body() ?: emptyList() else emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading    = false,
                    errorMessage = "Sin conexión."
                )
            }
        }
    }

    companion object {
        fun factory(apiService: ApiService, idBarbero: Long) =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return BarberoDetalleViewModel(apiService, idBarbero) as T
                }
            }
    }
}

// ── Pantalla ──────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarberoDetalleScreen(
    apiService: ApiService,
    idBarbero: Long,
    onVolver: () -> Unit
) {
    val colores = LocalBarberiaColores.current
    val viewModel: BarberoDetalleViewModel = viewModel(
        factory = BarberoDetalleViewModel.factory(apiService, idBarbero)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Promedio de calificación calculado en el cliente
    val promedio = if (uiState.resenas.isNotEmpty())
        uiState.resenas.mapNotNull { it.calificacion }.average() else 0.0

    Scaffold(
        containerColor = colores.fondo,
        topBar = {
            TopAppBar(
                title = {
                    Text(uiState.barbero?.nombre ?: "Barbero",
                        color = colores.texto, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                            tint = colores.texto)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colores.superficie
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ColorRojo,
                    modifier = Modifier.size(36.dp), strokeWidth = 2.5.dp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
                    .background(colores.fondo),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Header con avatar y datos del barbero ─────────────────
                item {
                    Box(modifier = Modifier.fillMaxWidth().background(
                        Brush.verticalGradient(
                            if (colores.esModoOscuro)
                                listOf(Color(0xFF020A1A), colores.fondo)
                            else
                                listOf(Color(0xFFF0F5FF), colores.fondo)
                        )
                    )) {
                        Column(modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            // Avatar grande
                            Box(modifier = Modifier.size(90.dp).clip(CircleShape)
                                .background(ColorAzul.copy(0.15f))
                                .border(2.dp, ColorAzul, CircleShape),
                                contentAlignment = Alignment.Center) {
                                Text(
                                    uiState.barbero?.nombre
                                        ?.split(" ")?.take(2)
                                        ?.joinToString("") { it.take(1).uppercase() }
                                        ?: "?",
                                    color = ColorAzul,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(uiState.barbero?.nombre ?: "",
                                color = colores.texto, fontSize = 22.sp,
                                fontWeight = FontWeight.Bold)
                            uiState.barbero?.especialidad?.let {
                                Text(it, color = ColorAzul, fontSize = 14.sp)
                            }
                            uiState.barbero?.telefono?.let {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Filled.Phone, null,
                                        tint = colores.textoSub,
                                        modifier = Modifier.size(14.dp))
                                    Text(it, color = colores.textoSub, fontSize = 13.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Stats: promedio y total reseñas
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("%.1f".format(promedio),
                                            color = ColorDorado, fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold)
                                        Icon(Icons.Filled.Star, null,
                                            tint = ColorDorado,
                                            modifier = Modifier.size(20.dp))
                                    }
                                    Text("Promedio", color = colores.textoSub, fontSize = 11.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${uiState.resenas.size}",
                                        color = ColorAzul, fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold)
                                    Text("Reseñas", color = colores.textoSub, fontSize = 11.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${uiState.horarios.size}",
                                        color = ColorVerde, fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold)
                                    Text("Horarios", color = colores.textoSub, fontSize = 11.sp)
                                }
                            }

                            // Badge activo/inactivo
                            Spacer(modifier = Modifier.height(8.dp))
                            val activo = uiState.barbero?.activo == true
                            Box(modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (activo) ColorVerde.copy(0.15f)
                                    else ColorError.copy(0.15f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)) {
                                Text(
                                    if (activo) "● Disponible" else "● No disponible",
                                    color = if (activo) ColorVerde else ColorError,
                                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // ── Horarios ──────────────────────────────────────────────
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        DetalleSeccionTitulo("Horarios de atención", ColorAzul, colores)
                        Spacer(modifier = Modifier.height(10.dp))

                        if (uiState.horarios.isEmpty()) {
                            Text("Sin horarios registrados",
                                color = colores.textoSub, fontSize = 13.sp)
                        } else {
                            // Orden fijo de días
                            val ordenDias = listOf("LUNES","MARTES","MIERCOLES",
                                "JUEVES","VIERNES","SABADO","DOMINGO")

                            // 👇 CORRECCIÓN AQUÍ: Usamos .name en lugar de .uppercase()
                            val horarioOrdenado = uiState.horarios.sortedBy {
                                ordenDias.indexOf(it.diaSemana?.name ?: "")
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                horarioOrdenado.forEach { horario ->
                                    TarjetaHorarioDetalle(horario, colores)
                                }
                            }
                        }
                    }
                }

                // ── Reseñas ───────────────────────────────────────────────
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        DetalleSeccionTitulo("Reseñas de clientes", ColorDorado, colores)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                if (uiState.resenas.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center) {
                            Text("Sin reseñas aún",
                                color = colores.textoSub, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(uiState.resenas) { resena ->
                        Box(modifier = Modifier.padding(
                            horizontal = 20.dp, vertical = 4.dp)) {
                            TarjetaResenaDetalle(resena, colores)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

// ── Componentes ───────────────────────────────────────────────────────────────

@Composable
fun DetalleSeccionTitulo(texto: String, color: Color, colores: BarberiaColores) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.width(3.dp).height(18.dp)
            .clip(RoundedCornerShape(2.dp)).background(color))
        Text(texto, color = colores.texto, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TarjetaHorarioDetalle(horario: HorarioBarberoDTO, colores: BarberiaColores) {

    // Como es un Enum, usamos .name para sacar el texto "LUNES", "MARTES", etc.
    // Si llega a ser nulo, usamos un texto vacío
    val nombreDia = horario.diaSemana?.name ?: ""

    val diaLabel = when (nombreDia) {
        "LUNES"     -> "Lunes"
        "MARTES"    -> "Martes"
        "MIERCOLES" -> "Miércoles"
        "JUEVES"    -> "Jueves"
        "VIERNES"   -> "Viernes"
        "SABADO"    -> "Sábado"
        "DOMINGO"   -> "Domingo"
        else        -> ""
    }

    // ... aquí sigue el código de tu Card ...
    Card(modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colores.superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = colores.sombra.dp)) {
        Row(modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                    .background(ColorAzul.copy(0.1f)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Schedule, null,
                        tint = ColorAzul, modifier = Modifier.size(18.dp))
                }
                Text(diaLabel, color = colores.texto,
                    fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
            Text(
                "${horario.horaInicio?.take(5)} — ${horario.horaFin?.take(5)}",
                color = ColorAzul, fontWeight = FontWeight.SemiBold, fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun TarjetaResenaDetalle(resena: ResenaDTO, colores: BarberiaColores) {
    Card(modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colores.superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = colores.sombra.dp)) {
        Column(modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Cliente #${resena.idUsuario}",
                    color = colores.textoSub, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    (1..5).forEach { i ->
                        Icon(
                            imageVector = if (i <= (resena.calificacion ?: 0))
                                Icons.Filled.Star else Icons.Filled.StarOutline,
                            contentDescription = null,
                            tint = ColorDorado, modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            if (!resena.comentario.isNullOrBlank()) {
                Text(resena.comentario, color = colores.texto,
                    fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}