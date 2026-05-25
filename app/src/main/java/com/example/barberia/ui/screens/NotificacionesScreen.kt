package com.example.barberia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.barberia.model.NotificacionDTO
import com.example.barberia.model.TipoNotificacionEnum
import com.example.barberia.network.ApiService
import com.example.barberia.ui.theme.LocalBarberiaColores


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacionesScreen(
    apiService: ApiService,
    idUsuario: Long,
    onVolver: () -> Unit
) {
    val colores = LocalBarberiaColores.current
    var notificaciones by remember { mutableStateOf<List<NotificacionDTO>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val citasResponse = apiService.getCitasByUsuario(idUsuario)
            if (citasResponse.isSuccessful) {
                val todas = mutableListOf<NotificacionDTO>()
                for (cita in citasResponse.body() ?: emptyList()) {
                    cita.idCita?.let {
                        val notis = apiService.getNotificacionesByCita(it)
                        if (notis.isSuccessful) {
                            todas.addAll(notis.body() ?: emptyList())
                        }
                    }
                }
                notificaciones = todas.sortedByDescending { n -> n.idNotificacion }
            }
        } catch (_: Exception) { }
        isLoading = false
    }

    Scaffold(
        containerColor = colores.fondo,
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones", fontWeight = FontWeight.Bold, color = colores.texto) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = colores.texto)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colores.superficie)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFC62828))
            }
        } else if (notificaciones.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Notifications, null, tint = colores.textoSub, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No hay notificaciones", color = colores.textoSub, fontSize = 16.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(notificaciones) { notif ->
                    val esAgendada = notif.tipo == TipoNotificacionEnum.CITA_AGENDADA
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
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape)
                                    .background(if (esAgendada) Color(0xFF4CAF50).copy(0.15f) else Color(0xFFC62828).copy(0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (esAgendada) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                    null,
                                    tint = if (esAgendada) Color(0xFF4CAF50) else Color(0xFFC62828),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (esAgendada) "Tu cita fue agendada" else "Tu cita fue cancelada",
                                    color = colores.texto,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    notif.mensaje ?: "",
                                    color = colores.textoSub,
                                    fontSize = 12.sp,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
