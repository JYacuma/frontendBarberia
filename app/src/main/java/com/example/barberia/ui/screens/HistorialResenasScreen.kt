package com.example.barberia.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.barberia.model.CitaConDetalle
import com.example.barberia.model.EstadoCitaEnum
import com.example.barberia.model.ResenaDTO
import com.example.barberia.network.ApiService
import com.example.barberia.ui.theme.ColorDorado
import com.example.barberia.ui.theme.ColorRojo
import com.example.barberia.ui.theme.LocalBarberiaColores

data class ResenaConDetalle(
    val resena: ResenaDTO,
    val barberoNombre: String,
    val fecha: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialResenasScreen(
    apiService: ApiService,
    idUsuario: Long,
    onVolver: () -> Unit
) {
    val colores = LocalBarberiaColores.current
    var resenasConDetalle by remember { mutableStateOf<List<ResenaConDetalle>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val barberosResp = apiService.getBarberosActivos()
            val barberosMap = if (barberosResp.isSuccessful)
                (barberosResp.body() ?: emptyList()).associateBy { it.idBarbero }
            else emptyMap()

            val todas = mutableListOf<ResenaConDetalle>()
            for ((_, barbero) in barberosMap) {
                barbero.idBarbero?.let { idB ->
                    try {
                        val resResp = apiService.getResenasByBarbero(idB)
                        Log.d("RESENAS", "Total reseñas barbero $idB: ${resResp.body()?.size}")
                        resResp.body()?.forEach { Log.d("RESENAS", "idUsuario=${it.idUsuario}, idCita=${it.idCita}") }
                        if (resResp.isSuccessful) {
                            for (r in (resResp.body() ?: emptyList())) {
                                if (r.idUsuario == idUsuario) {
                                    todas.add(
                                        ResenaConDetalle(
                                            resena = r,
                                            barberoNombre = barbero.nombre,
                                            fecha = r.fecha ?: ""
                                        )
                                    )
                                }
                            }
                        }
                    } catch (_: Exception) { }
                }
            }

            val citasResp = apiService.getCitasByUsuario(idUsuario)
            val citasMap = if (citasResp.isSuccessful)
                (citasResp.body() ?: emptyList()).associateBy { it.idCita }
            else emptyMap()

            val result = todas.map { rcd ->
                val cita = citasMap[rcd.resena.idCita]
                rcd.copy(fecha = cita?.fecha ?: "")
            }.sortedByDescending { it.resena.idResena }

            resenasConDetalle = result
        } catch (_: Exception) { }
        cargando = false
    }

    Scaffold(
        containerColor = colores.fondo,
        topBar = {
            TopAppBar(
                title = { Text("Historial de reseñas", fontWeight = FontWeight.Bold, color = colores.texto) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = colores.texto)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colores.superficie)
            )
        }
    ) { padding ->
        if (cargando) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ColorRojo)
            }
        } else if (resenasConDetalle.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.StarOutline, null, tint = colores.textoSub, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No has dejado reseñas aún", color = colores.textoSub, fontSize = 16.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(resenasConDetalle) { rcd ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = colores.superficie)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                                .background(ColorRojo.copy(0.15f)),
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Star, null, tint = ColorDorado, modifier = Modifier.size(22.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rcd.barberoNombre, color = colores.texto,
                                    fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    repeat(5) { i ->
                                        Icon(
                                            if (i < rcd.resena.calificacion) Icons.Filled.Star else Icons.Filled.StarOutline,
                                            null,
                                            tint = ColorDorado,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                if (rcd.fecha.isNotBlank()) {
                                    Text(rcd.fecha, color = colores.textoSub, fontSize = 12.sp)
                                }
                                if (!rcd.resena.comentario.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(rcd.resena.comentario, color = colores.texto, fontSize = 13.sp, maxLines = 3)
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}
