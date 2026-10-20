package app.samar.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import app.samar.ui.theme.SamarShapes
import kotlinx.coroutines.launch

/**
 * Visual-only glass switch (the parent row handles taps and TalkBack).
 * On = accent glass track, knob on the right; off = clear glass track, knob on the left.
 * The knob stretches like a droplet while it slides, as in [ThemeSwitch].
 */
@Composable
fun GlassSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val position = remember { Animatable(if (checked) 1f else 0f) }
    val stretch = remember { Animatable(0f) }
    LaunchedEffect(checked) {
        val target = if (checked) 1f else 0f
        if (position.value == target) return@LaunchedEffect
        launch {
            stretch.animateTo(1f, tween(110))
            stretch.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
        }
        position.animateTo(target, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow))
    }
    Box(
        modifier
            .size(width = 52.dp, height = 32.dp)
            .liquidGlass(SamarShapes.pill, if (checked) GlassTone.Accent else GlassTone.Clear, outline = !checked),
        contentAlignment = Alignment.CenterStart,
    ) {
        val width = lerp(26.dp, 34.dp, stretch.value)
        Box(
            Modifier
                .offset(x = lerp(3.dp, 52.dp - 3.dp - width, position.value))
                .size(width = width, height = 26.dp)
                .liquidGlass(SamarShapes.pill, GlassTone.Frost, elevation = 3.dp),
        )
    }
}
