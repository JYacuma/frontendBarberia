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

// ── Colores fijos de identidad — nunca cambian entre modos ───────────────────
val ColorRojo      = Color(0xFFC0272D)
val ColorRojoClaro = Color(0xFFE05555)
val ColorAzul      = Color(0xFF1B4F9B)
val ColorAzulClaro = Color(0xFF4A80D4)
val ColorBlanco    = Color(0xFFFFFFFF)
val ColorError     = Color(0xFFCF6679)
val ColorVerde     = Color(0xFF3CB86A)
val ColorDorado    = Color(0xFFD4A017)

// ── Colores que cambian según el modo ─────────────────────────────────────────
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
    fondo        = Color(0xFF0A0A0A),
    superficie   = Color(0xFF141414),
    superficie2  = Color(0xFF1E1E1E),
    borde        = Color(0xFF2A2A2A),
    texto        = Color(0xFFF0F0F0),
    textoSub     = Color(0xFF888888),
    sombra       = 0f,
    esModoOscuro = true
)

val ColoresClaro = BarberiaColores(
    fondo        = Color(0xFFF5F0EB),
    superficie   = Color(0xFFFFFFFF),
    superficie2  = Color(0xFFF0EBE4),
    borde        = Color(0xFFE8E0D8),
    texto        = Color(0xFF1A1A1A),
    textoSub     = Color(0xFF999999),
    sombra       = 2f,
    esModoOscuro = false
)

// ── Estado del tema con 3 modos ───────────────────────────────────────────────
// null  → sigue automáticamente el sistema del celular (por defecto)
// true  → forzar modo oscuro sin importar el sistema
// false → forzar modo claro sin importar el sistema
//
// Al presionar el botón en la app cicla entre los 3 estados:
//   Sistema → Oscuro forzado → Claro forzado → Sistema → ...
object TemaManager {
    val modoOscuro = mutableStateOf<Boolean?>(null)

    // Cicla entre los 3 estados al presionar el botón de tema
    // Se llama desde LoginScreen, RegisterScreen y ClienteScreen
    fun toggleModo(sistemaEsOscuro: Boolean) {
        modoOscuro.value = when (modoOscuro.value) {
            null  -> if (sistemaEsOscuro) false else true  // invierte el sistema
            true  -> false                                  // oscuro → claro
            false -> null                                   // claro → sistema
        }
    }
}

val LocalBarberiaColores = compositionLocalOf { ColoresOscuros }

// ── Esquemas Material3 ────────────────────────────────────────────────────────
private val EsquemaOscuro = darkColorScheme(
    primary      = ColorRojo,
    secondary    = ColorAzul,
    background   = Color(0xFF0A0A0A),
    surface      = Color(0xFF141414),
    onPrimary    = Color.White,
    onBackground = Color(0xFFF0F0F0),
    onSurface    = Color(0xFFF0F0F0),
    error        = ColorError
)

private val EsquemaClaro = lightColorScheme(
    primary      = ColorRojo,
    secondary    = ColorAzul,
    background   = Color(0xFFF5F0EB),
    surface      = Color(0xFFFFFFFF),
    onPrimary    = Color.White,
    onBackground = Color(0xFF1A1A1A),
    onSurface    = Color(0xFF1A1A1A),
    error        = ColorError
)

@Composable
fun BarberiaTheme(content: @Composable () -> Unit) {
    // isSystemInDarkTheme() lee el ajuste del celular en tiempo real
    // Si el usuario cambia el modo en el sistema, la app lo detecta automáticamente
    val sistemaOscuro = isSystemInDarkTheme()

    // Decide qué modo usar:
    // null  → usa el sistema del celular
    // true  → oscuro forzado por el usuario
    // false → claro forzado por el usuario
    val usarModoOscuro = TemaManager.modoOscuro.value ?: sistemaOscuro

    val colores = if (usarModoOscuro) ColoresOscuros else ColoresClaro
    val esquema = if (usarModoOscuro) EsquemaOscuro  else EsquemaClaro

    CompositionLocalProvider(LocalBarberiaColores provides colores) {
        MaterialTheme(colorScheme = esquema, content = content)
    }
}