package com.example.barberia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

data class BarberiaColores(
    val fondo: Color,
    val superficie: Color,
    val superficie2: Color,
    val borde: Color,
    val texto: Color,
    val textoSub: Color,
    val sombra: Float,
    val esModoOscuro: Boolean
)

val ColoresOscuros = BarberiaColores(
    fondo = Color(0xFF1A1A2E),
    superficie = Color(0xFF16213E),
    superficie2 = Color(0xFF0F3460),
    borde = Color(0xFF2A2A4A),
    texto = Color(0xFFF0F0F0),
    textoSub = Color(0xFF888888),
    sombra = 0f,
    esModoOscuro = true
)

val ColoresClaro = BarberiaColores(
    fondo = Color(0xFFF8F9FA),
    superficie = Color(0xFFFFFFFF),
    superficie2 = Color(0xFFF0F2F5),
    borde = Color(0xFFE0E4E8),
    texto = Color(0xFF1A1A1A),
    textoSub = Color(0xFF999999),
    sombra = 2f,
    esModoOscuro = false
)

object TemaManager {
    val modoOscuro = mutableStateOf<Boolean?>(null)

    fun toggleModo() {
        val actual = modoOscuro.value
        modoOscuro.value = if (actual == null) true else if (actual) false else null
    }
}

val LocalBarberiaColores = compositionLocalOf { ColoresOscuros }

private val EsquemaOscuro = darkColorScheme(
    primary = ColorRojo,
    secondary = ColorAzul,
    background = Color(0xFF1A1A2E),
    surface = Color(0xFF16213E),
    onPrimary = Color.White,
    onBackground = Color(0xFFF0F0F0),
    onSurface = Color(0xFFF0F0F0),
    error = ColorError
)

private val EsquemaClaro = lightColorScheme(
    primary = ColorRojo,
    secondary = ColorAzul,
    background = Color(0xFFF8F9FA),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A),
    error = ColorError
)

@Composable
fun BarberiaTheme(content: @Composable () -> Unit) {
    val sistemaOscuro = isSystemInDarkTheme()
    val usarModoOscuro = TemaManager.modoOscuro.value ?: sistemaOscuro
    val colores = if (usarModoOscuro) ColoresOscuros else ColoresClaro
    val esquema = if (usarModoOscuro) EsquemaOscuro else EsquemaClaro
    CompositionLocalProvider(LocalBarberiaColores provides colores) {
        MaterialTheme(colorScheme = esquema, content = content)
    }
}
