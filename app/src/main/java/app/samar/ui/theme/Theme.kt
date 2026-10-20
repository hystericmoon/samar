package app.samar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.dp

/** Shapes from DESIGN_BRIEF.md ("Shape"). */
object SamarShapes {
    val pill = CircleShape
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val thumbnail = RoundedCornerShape(6.dp)
}

/**
 * App theme. [darkTheme] defaults to the system setting; the Home ThemeSwitch
 * (step 2) will pass an override here.
 */
@Composable
fun SamarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors

    // Map our tokens onto Material so stock controls (Slider, Switch) match.
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.accent, onPrimary = colors.textOnAccent,
            primaryContainer = colors.accentTint, onPrimaryContainer = colors.text,
            background = colors.background, onBackground = colors.text,
            surface = colors.surface, onSurface = colors.text,
            surfaceVariant = colors.divider, onSurfaceVariant = colors.mutedText,
            outline = colors.outline, outlineVariant = colors.divider,
        )
    } else {
        lightColorScheme(
            primary = colors.accent, onPrimary = colors.textOnAccent,
            primaryContainer = colors.accentTint, onPrimaryContainer = colors.text,
            background = colors.background, onBackground = colors.text,
            surface = colors.surface, onSurface = colors.text,
            surfaceVariant = colors.divider, onSurfaceVariant = colors.mutedText,
            outline = colors.outline, outlineVariant = colors.divider,
        )
    }

    CompositionLocalProvider(
        LocalSamarColors provides colors,
        LocalSamarType provides DefaultType,
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/** Shorthand: `Samar.colors.accent`, `Samar.type.hero`. */
object Samar {
    val colors: SamarColors
        @Composable @ReadOnlyComposable get() = LocalSamarColors.current
    val type: SamarType
        @Composable @ReadOnlyComposable get() = LocalSamarType.current
}
