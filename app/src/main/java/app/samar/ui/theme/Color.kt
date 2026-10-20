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
    /** Strong highlight for numbers and the selected state (Neon in dark, Navy in light). */
    val emphasis: Color,
    /** Background: [backgroundTop] glows at the top-left and fades into [background]. */
    val backgroundTop: Color,
    val glow: Color,
    val isDark: Boolean,
)

val LightColors = SamarColors(
    background = Color(0xFFE9EDF9),   // Paper (blue-tinted)
    surface = Color(0xFFFFFFFF),      // White
    text = Color(0xFF15141C),         // Ink
    mutedText = Color(0xFF55536A),
    outline = Color(0xFF8C89A3),
    divider = Color(0xFFDEDCE8),
    accent = Color(0xFF1F06AC),       // Navy
    accentTint = Color(0xFFE4E1F7),   // Haze (navy tint)
    textOnAccent = Color(0xFFFFFFFF),
    emphasis = Color(0xFF1F06AC),
    backgroundTop = Color(0xFFF4F6FD),
    glow = Color(0x385882E8),
    isDark = false,
)

val DarkColors = SamarColors(
    background = Color(0xFF090B2A),   // Night (navy)
    surface = Color(0xFF151A4E),      // Panel
    text = Color(0xFFEEF0FF),         // Mist
    mutedText = Color(0xFF9AA3D6),
    outline = Color(0xFF5C67A8),
    divider = Color(0xFF242A63),
    accent = Color(0xFF5882E8),       // Cornflower
    accentTint = Color(0xFF1E2550),   // Dusk (blue)
    textOnAccent = Color(0xFF0B0A24),
    emphasis = Color(0xFFE3EF4F),     // Neon
    backgroundTop = Color(0xFF0E1240),
    glow = Color(0x475882E8),
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
