package app.samar.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colour tokens from DESIGN_BRIEF.md. Names match the brief's token table. */
@Immutable
data class SamarColors(
    val background: Color,
    val surface: Color,
    val text: Color,
    val mutedText: Color,
    val outline: Color,
    val divider: Color,
    val accent: Color,
    val accentTint: Color,
    val textOnAccent: Color,
    val isDark: Boolean,
)

val LightColors = SamarColors(
    background = Color(0xFFF2F2F6),   // Paper
    surface = Color(0xFFFFFFFF),      // White
    text = Color(0xFF15141C),         // Ink
    mutedText = Color(0xFF55536A),
    outline = Color(0xFF8C89A3),
    divider = Color(0xFFDEDCE8),
    accent = Color(0xFF5B3FF5),       // Ultraviolet
    accentTint = Color(0xFFE7E2FF),   // Haze
    textOnAccent = Color(0xFFFFFFFF),
    isDark = false,
)

val DarkColors = SamarColors(
    background = Color(0xFF121118),   // Night
    surface = Color(0xFF1C1B25),      // Panel
    text = Color(0xFFF3F2F8),         // Mist
    mutedText = Color(0xFFA9A6BF),
    outline = Color(0xFF6F6C86),
    divider = Color(0xFF2C2A38),
    accent = Color(0xFFA596FF),       // Lilac
    accentTint = Color(0xFF2D2850),   // Dusk
    textOnAccent = Color(0xFF14121F),
    isDark = true,
)

/** Photo placeholder colours (previews and empty states only; never theme-tinted). */
object PhotoPlaceholder {
    val A = Color(0xFFC9D6D3)
    val B = Color(0xFFBFD0CE)
}

val LocalSamarColors = staticCompositionLocalOf { LightColors }
