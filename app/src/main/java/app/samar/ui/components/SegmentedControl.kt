package app.samar.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.samar.ui.theme.Samar
import app.samar.ui.theme.SamarShapes

/**
 * Pill-shaped two-or-more option switch (Ellipse / Rectangle, Before / After).
 * Clear glass track; the selected option is an accent glass pill that slides over.
 * TalkBack reads each as a radio button.
 */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") trackColor: Color = Samar.colors.background,
    height: Dp = 52.dp,
) {
    val c = Samar.colors
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(height)
            .liquidGlass(SamarShapes.pill, GlassTone.Clear, outline = true)
            .padding(4.dp),
    ) {
        val slot = maxWidth / options.size
        val index = options.indexOf(selected).coerceAtLeast(0)
        val x by animateDpAsState(
            slot * index,
            spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
            label = "segment",
        )
        Box(
            Modifier
                .offset(x = x)
                .width(slot)
                .fillMaxHeight()
                .liquidGlass(SamarShapes.pill, GlassTone.Accent, elevation = 3.dp),
        )
        Row(Modifier.fillMaxSize().selectableGroup()) {
            options.forEach { option ->
                val isSelected = option == selected
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(SamarShapes.pill)
                        .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(option) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label(option),
                        style = Samar.type.buttonSmall,
                        color = if (isSelected) c.textOnAccent else c.text,
                    )
                }
            }
        }
    }
}
