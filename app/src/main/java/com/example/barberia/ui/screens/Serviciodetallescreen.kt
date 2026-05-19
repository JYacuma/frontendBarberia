package com.example.barberia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.barberia.model.ServicioDTO
import com.example.barberia.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicioDetalleScreen(
    servicio: ServicioDTO,
    onVolver: () -> Unit
) {
    val colores = LocalBarberiaColores.current

    Scaffold(
        containerColor = colores.fondo,
        topBar = {
            TopAppBar(
                title = {
                    Text(servicio.nombre, color = colores.texto,
                        fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                            tint = colores.texto)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colores.superficie)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding)
                .background(colores.fondo),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header con precio destacado ───────────────────────────────
            item {
                Box(modifier = Modifier.fillMaxWidth().background(
                    Brush.verticalGradient(
                        if (colores.esModoOscuro)
                            listOf(Color(0xFF001A0A), colores.fondo)
                        else
                            listOf(Color(0xFFF0FFF5), colores.fondo)
                    )
                )) {
                    Column(modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        // Ícono grande
                        Box(modifier = Modifier.size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(ColorVerde.copy(0.15f)),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.ContentCut, null,
                                tint = ColorVerde,
                                modifier = Modifier.size(40.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(servicio.nombre, color = colores.texto,
                            fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        // Precio destacado
                        Text("\$${servicio.precio.toInt()}",
                            color = ColorVerde, fontSize = 36.sp,
                            fontWeight = FontWeight.Bold)
                        Text("precio del servicio",
                            color = colores.textoSub, fontSize = 12.sp)
                    }
                }
            }

            // ── Stats rápidos ─────────────────────────────────────────────
            item {
                Row(modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ServicioStatCard("${servicio.duracionMinutos}",
                        "minutos", Icons.Filled.Schedule,
                        ColorAzul, colores, Modifier.weight(1f))
                    ServicioStatCard("\$${servicio.precio.toInt()}",
                        "precio", Icons.Filled.AttachMoney,
                        ColorVerde, colores, Modifier.weight(1f))
                }
            }

            // ── Descripción ───────────────────────────────────────────────
            if (!servicio.descripcion.isNullOrBlank()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        DetalleSeccionTitulo("Descripción", ColorVerde, colores)
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = colores.superficie),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = colores.sombra.dp)) {
                            Text(servicio.descripcion,
                                modifier = Modifier.padding(16.dp),
                                color = colores.texto, fontSize = 14.sp,
                                lineHeight = 22.sp)
                        }
                    }
                }
            }

            // ── Info adicional ────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    DetalleSeccionTitulo("Información", ColorAzul, colores)
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = colores.superficie),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = colores.sombra.dp)) {
                        Column(modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            FilaInfo("Duración estimada",
                                "${servicio.duracionMinutos} minutos",
                                Icons.Filled.Schedule, colores)
                            HorizontalDivider(color = colores.borde)
                            FilaInfo("Precio",
                                "\$${servicio.precio.toInt()} COP",
                                Icons.Filled.AttachMoney, colores)
                            HorizontalDivider(color = colores.borde)
                            FilaInfo("ID del servicio",
                                "#${servicio.idServicio}",
                                Icons.Filled.Tag, colores)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

// ── Componentes privados ──────────────────────────────────────────────────────

@Composable
private fun ServicioStatCard(
    numero: String, label: String,
    icono: ImageVector, color: Color,
    colores: BarberiaColores, modifier: Modifier = Modifier
) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colores.superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = colores.sombra.dp)) {
        Column(modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icono, null, tint = color, modifier = Modifier.size(24.dp))
            Text(numero, color = color, fontSize = 22.sp,
                fontWeight = FontWeight.Bold)
            Text(label, color = colores.textoSub, fontSize = 11.sp)
        }
    }
}

@Composable
private fun FilaInfo(
    label: String, valor: String,
    icono: ImageVector, colores: BarberiaColores
) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icono, null, tint = colores.textoSub,
                modifier = Modifier.size(18.dp))
            Text(label, color = colores.textoSub, fontSize = 13.sp)
        }
        Text(valor, color = colores.texto,
            fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}