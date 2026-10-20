package app.samar.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Screen background for the glass look: a vertical fade from [SamarColors.backgroundTop] to
 * [SamarColors.background], with a soft cornflower glow at the top-left for the glass to catch.
 */
@Composable
fun Modifier.samarBackdrop(): Modifier {
    val c = Samar.colors
    return drawBehind {
        drawRect(Brush.verticalGradient(listOf(c.backgroundTop, c.background)))
        drawRect(
            Brush.radialGradient(
                listOf(c.glow, Color.Transparent),
                center = Offset(size.width * 0.2f, -size.height * 0.05f),
                radius = size.width * 1.2f,
            ),
        )
    }
}
