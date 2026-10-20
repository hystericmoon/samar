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
    accent = Color(0xFF1F06AC),       // Navy
    accentTint = Color(0xFFE4E1F7),   // Haze (navy tint)
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
    accent = Color(0xFF5882E8),       // Cornflower
    accentTint = Color(0xFF1E2550),   // Dusk (blue)
    textOnAccent = Color(0xFF0B0A24),
    isDark = true,
)

/**
 * Controls drawn over photos (face boxes, face toggles, the blur-area shape, the Home hero badge).
 * Fixed in both themes: neon yellow reads on almost any photo, with navy marks and a dark halo
 * so it still holds 3:1 on light photos.
 */
object Overlay {
    val Highlight = Color(0xFFE3EF4F)   // Neon
    val Ink = Color(0xFF1F06AC)         // Navy
}

/** Photo placeholder colours (previews and empty states only; never theme-tinted). */
object PhotoPlaceholder {
    val A = Color(0xFFC9D6D3)
    val B = Color(0xFFBFD0CE)
}

val LocalSamarColors = staticCompositionLocalOf { LightColors }
