package com.example.barberia.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.barberia.model.BarberoDTO
import com.example.barberia.model.ResenaDTO
import com.example.barberia.network.ApiService
import com.example.barberia.ui.theme.BarberiaColores
import com.example.barberia.ui.theme.ColorAzul
import com.example.barberia.ui.theme.ColorDorado
import com.example.barberia.ui.theme.ColorRojo
import com.example.barberia.ui.theme.ColorVerde
import com.example.barberia.ui.theme.LocalBarberiaColores

@Composable
fun InfoBarberoScreen(
    apiService: ApiService,
    idBarbero: Long,
    onVolver: () -> Unit,
    onAgendar: (Long) -> Unit
) {
    val colores = LocalBarberiaColores.current
    var barbero by remember { mutableStateOf<BarberoDTO?>(null) }
    var resenas by remember { mutableStateOf<List<ResenaDTO>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }

    LaunchedEffect(idBarbero) {
        val barberoResp = apiService.getBarberoById(idBarbero)
        if (barberoResp.isSuccessful) barbero = barberoResp.body()
        val resenasResp = apiService.getResenasByBarbero(idBarbero)
        if (resenasResp.isSuccessful) resenas = resenasResp.body() ?: emptyList()
        cargando = false
    }

    val promedio = remember(resenas) {
        if (resenas.isEmpty()) 0.0
        else resenas.map { it.calificacion }.average()
    }

    Column(modifier = Modifier.fillMaxSize().background(colores.fondo)) {
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(
            androidx.compose.ui.graphics.Brush.horizontalGradient(
                listOf(ColorRojo, ColorRojo.copy(0.5f), ColorRojo)
            ))) {}
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVolver) {
                Icon(Icons.Filled.ArrowBack, "Volver", tint = colores.texto)
            }
        }

        if (cargando) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ColorRojo)
            }
        } else {
            val data = barbero
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                Box(modifier = Modifier.size(80.dp).clip(CircleShape)
                    .background(ColorRojo.copy(0.15f)),
                    contentAlignment = Alignment.Center) {
                    Text(
                        (data?.nombre?.take(2)?.uppercase() ?: "?"),
                        color = ColorRojo, fontWeight = FontWeight.Bold, fontSize = 28.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    data?.nombre ?: "Barbero",
                    color = colores.texto,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colores.superficie),
                    elevation = CardDefaults.cardElevation(defaultElevation = colores.sombra.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        data?.especialidad?.let {
                            InfoFila(icono = Icons.Filled.ContentCut, label = "Especialidad", valor = it, colores)
                        }
                        if (!data?.telefono.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            InfoFila(icono = Icons.Filled.Phone, label = "Teléfono", valor = data!!.telefono!!, colores)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colores.superficie),
                    elevation = CardDefaults.cardElevation(defaultElevation = colores.sombra.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Reseñas", color = colores.texto,
                            fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        if (resenas.isEmpty()) {
                            Text("Sin reseñas aún", color = colores.textoSub, fontSize = 13.sp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "%.1f".format(promedio),
                                    color = ColorDorado,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                )
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val estrellas = kotlin.math.round(promedio).toInt()
                                        repeat(5) { i ->
                                            Icon(
                                                if (i < estrellas) Icons.Filled.Star else Icons.Filled.Star,
                                                null,
                                                tint = if (i < estrellas) ColorDorado else ColorDorado.copy(0.2f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "${resenas.size} reseña${if (resenas.size != 1) "s" else ""}",
                                        color = colores.textoSub,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { onAgendar(idBarbero) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorRojo)
                ) {
                    Icon(Icons.Filled.CalendarMonth, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Agendar cita", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun InfoFila(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    valor: String,
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
            Text(label, color = colores.textoSub, fontSize = 11.sp)
            Text(valor, color = colores.texto, fontSize = 14.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
