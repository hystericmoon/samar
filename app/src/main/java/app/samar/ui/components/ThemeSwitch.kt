package app.samar.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import app.samar.ui.icons.SamarIcons
import app.samar.ui.theme.SamarShapes
import kotlinx.coroutines.launch

/**
 * Light/dark switch from the Home top bar. Knob on the left with a sun = light,
 * on the right with a moon = dark (state shown by position and icon, not colour alone).
 * Glass track with a glass knob that stretches like a droplet while it slides.
 * Visual size 56 x 32dp inside a 48dp-tall touch target.
 */
@Composable
fun ThemeSwitch(
    isDark: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val position = remember { Animatable(if (isDark) 1f else 0f) }
    val stretch = remember { Animatable(0f) }
    LaunchedEffect(isDark) {
        val target = if (isDark) 1f else 0f
        if (position.value == target) return@LaunchedEffect
        launch {
            stretch.animateTo(1f, tween(110))
            stretch.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
        }
        position.animateTo(target, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow))
    }

    Box(
        modifier = modifier
            .toggleable(
                value = isDark, interactionSource = interaction, indication = null,
                role = Role.Switch, onValueChange = onChange,
            )
            // TalkBack reads "Dark theme, switch, on/off".
            .semantics { contentDescription = "Dark theme" }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 56.dp, height = 32.dp)
                .liquidGlass(SamarShapes.pill, GlassTone.Clear, outline = true),
            contentAlignment = Alignment.CenterStart,
        ) {
            // The knob widens mid-slide and springs back, so it reads as liquid.
            val width = lerp(26.dp, 34.dp, stretch.value)
            val x = lerp(3.dp, 56.dp - 3.dp - width, position.value)
            Box(
                Modifier
                    .offset(x = x)
                    .size(width = width, height = 26.dp)
                    .liquidGlass(SamarShapes.pill, GlassTone.Accent, interaction, elevation = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isDark) SamarIcons.Moon else SamarIcons.Sun,
                    contentDescription = null,
                    tint = glassContentColor(GlassTone.Accent),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
