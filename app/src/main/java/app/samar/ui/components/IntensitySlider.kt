package app.samar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.samar.ui.theme.Samar
import app.samar.ui.theme.SamarShapes
import kotlin.math.roundToInt

/** "Intensity" label, percentage, and a slider (0..1). TalkBack reads "Intensity, 70 percent". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntensitySlider(
    value: Float,
    onChange: (Float) -> Unit,
    onChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    disabledNote: String = "",
) {
    val c = Samar.colors
    val percent = (value * 100).roundToInt()
    Column(modifier.alpha(if (enabled) 1f else 0.38f)) {
        Row(
            Modifier.fillMaxWidth().clearAndSetSemantics { },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Intensity", style = Samar.type.buttonSmall, color = c.text, modifier = Modifier.weight(1f))
            Text(if (enabled) "$percent%" else disabledNote, style = Samar.type.buttonSmall, color = if (enabled) c.emphasis else c.mutedText)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            onValueChangeFinished = onChangeFinished,
            valueRange = 0f..1f,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .semantics {
                    contentDescription = "Intensity"
                    stateDescription = if (enabled) "$percent percent" else disabledNote
                },
            thumb = {
                // Glass handle with an accent core.
                Box(
                    Modifier
                        .size(width = 34.dp, height = 24.dp)
                        .liquidGlass(SamarShapes.pill, GlassTone.Clear, elevation = 4.dp, outline = true),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(10.dp).background(c.accent, SamarShapes.pill))
                }
            },
            track = { state ->
                val fraction = (state.value - state.valueRange.start) /
                    (state.valueRange.endInclusive - state.valueRange.start)
                Canvas(Modifier.fillMaxWidth().height(8.dp)) {
                    val h = size.height
                    val r = CornerRadius(h / 2)
                    drawRoundRect(c.text.copy(alpha = 0.12f), size = size, cornerRadius = r)
                    drawRoundRect(c.outline, size = size, cornerRadius = r, style = Stroke(1.dp.toPx()))
                    drawRoundRect(
                        Brush.horizontalGradient(listOf(Color(0xFF0F42C0), Color(0xFF5882E8)), endX = size.width * fraction),
                        topLeft = Offset.Zero, size = Size(size.width * fraction, h), cornerRadius = r,
                    )
                }
            },
        )
    }
}
