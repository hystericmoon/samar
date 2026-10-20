package app.samar.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.samar.ui.theme.Overlay
import app.samar.ui.theme.Samar

/**
 * The "liquid glass" material used by every button and switch.
 *
 * - [Accent]: tinted purple glass for the main action (Choose photo, Export, Save).
 * - [Clear]: frosted glass for secondary buttons, tracks and icon buttons.
 * - [Dark]: smoky glass for controls that float over photos (eye button).
 * - [Frost]: light glass for controls over photos (face toggle "not blurred"). Fixed, not themed.
 * - [Highlight]: neon yellow glass for controls over photos (face toggle "blurred"). Fixed, not themed.
 *
 * Drawn in code (no background blur), so it looks the same on Android 8 and 16.
 */
enum class GlassTone { Accent, Clear, Dark, Frost, Highlight }

/** Content colour that keeps text and icons at 4.5:1 on each tone. */
@Composable
fun glassContentColor(tone: GlassTone): Color = when (tone) {
    GlassTone.Accent -> Samar.colors.textOnAccent
    GlassTone.Clear -> Samar.colors.text
    GlassTone.Dark -> Color.White
    GlassTone.Frost -> Color(0xFF15141C)
    GlassTone.Highlight -> Overlay.Ink
}

/**
 * Paints glass behind the content: a see-through body, a soft top highlight,
 * a bright rim (lit from above, with light bouncing back along the bottom edge)
 * and, when [elevation] > 0, a soft tinted shadow.
 *
 * Pass the control's [interaction] source so it squishes slightly and brightens when pressed.
 * [outline] adds a 1dp outer edge in the theme's outline colour, for controls whose edge must
 * read at 3:1 on a flat background (secondary buttons, the theme switch).
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape,
    tone: GlassTone,
    interaction: InteractionSource? = null,
    elevation: Dp = 0.dp,
    outline: Boolean = false,
): Modifier {
    val c = Samar.colors
    val dark = c.background.luminance() < 0.5f
    val pressed = interaction?.collectIsPressedAsState()?.value ?: false
    val press by animateFloatAsState(
        if (pressed) 1f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "glassPress",
    )

    val body = when (tone) {
        GlassTone.Accent -> c.accent.copy(alpha = 0.90f)
        GlassTone.Clear -> if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.62f)
        GlassTone.Dark -> Color(0xFF15141C).copy(alpha = 0.58f)
        GlassTone.Frost -> Color.White.copy(alpha = 0.80f)
        GlassTone.Highlight -> Overlay.Highlight.copy(alpha = 0.95f)
    }
    val highlight = when (tone) {
        GlassTone.Accent -> if (dark) 0.34f else 0.26f
        GlassTone.Clear -> if (dark) 0.16f else 0.70f
        GlassTone.Dark -> 0.20f
        GlassTone.Frost -> 0.75f
        GlassTone.Highlight -> 0.45f
    }
    val rimTop = if ((tone == GlassTone.Clear && !dark) || tone == GlassTone.Frost) 1f else 0.70f
    val shadowColor = when (tone) {
        GlassTone.Accent, GlassTone.Highlight -> Color(0xFF12056B).copy(alpha = if (dark) 0.6f else 0.5f)
        else -> Color.Black.copy(alpha = if (dark) 0.5f else 0.18f)
    }
    val edge = c.outline

    return this
        // Shadow first, outside the press layer: older Android clips a layer's drawing to its bounds.
        .then(if (elevation > 0.dp) Modifier.glassShadow(shape, elevation, shadowColor) else Modifier)
        .graphicsLayer {
            val s = 1f - 0.035f * press
            scaleX = s
            scaleY = s
        }
        .clip(shape)
        .drawWithCache {
            val outlineShape: Outline = shape.createOutline(size, layoutDirection, this)
            val path = Path().apply { addOutline(outlineShape) }
            val rimWidth = 1.dp.toPx()
            // Light falls in from the top and bounces back up from the bottom edge,
            // with no hard shapes inside, so it reads as a lens rather than a gloss sticker.
            val bodyBrush = Brush.verticalGradient(
                0f to Color.White.copy(alpha = highlight),
                0.42f to Color.White.copy(alpha = highlight * 0.18f),
                0.62f to Color.Transparent,
                0.86f to Color.Black.copy(alpha = if (tone == GlassTone.Clear || tone == GlassTone.Frost) 0.02f else 0.06f),
                1f to Color.White.copy(alpha = highlight * 0.35f),
            )
            val rimBrush = Brush.verticalGradient(
                0f to Color.White.copy(alpha = rimTop),
                0.45f to Color.White.copy(alpha = 0.12f),
                0.8f to Color.White.copy(alpha = 0.05f),
                1f to Color.White.copy(alpha = 0.45f),
            )
            onDrawBehind {
                drawPath(path, body)
                drawPath(path, bodyBrush)
                if (press > 0f) drawPath(path, Color.White.copy(alpha = 0.12f * press))
                // Rim: a double-width stroke clipped to the inside leaves a crisp inner edge.
                clipPath(path) {
                    if (outline) drawPath(path, edge, style = Stroke(rimWidth * 2f))
                    drawPath(path, rimBrush, style = Stroke(rimWidth * if (outline) 4f else 2f))
                    if (outline) drawPath(path, edge, style = Stroke(rimWidth * 2f))
                }
            }
        }
}

/**
 * Soft shadow drawn only outside the shape, so it never shows through the see-through glass
 * (a platform elevation shadow would). Stacked, growing outlines fake a blur on every Android version.
 */
private fun Modifier.glassShadow(shape: Shape, elevation: Dp, color: Color): Modifier = drawWithCache {
    val inner = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
    val e = elevation.toPx()
    val steps = 14
    // Grows outwards and drops lower layer by layer: a soft shadow that falls below the control.
    val layers = (1..steps).map { i ->
        val f = i.toFloat() / steps
        val grow = e * 0.7f * f
        val drop = e * (0.25f + 0.45f * f)
        val o = shape.createOutline(Size(size.width + grow * 2, size.height + grow * 2), layoutDirection, this)
        Path().apply { addOutline(o); translate(Offset(-grow, -grow + drop)) }
    }
    onDrawBehind {
        clipPath(inner, ClipOp.Difference) {
            // Each layer adds a little darkness, so the shadow is darkest close to the edge.
            layers.forEach { drawPath(it, color.copy(alpha = color.alpha * 0.55f / steps)) }
        }
    }
}

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue
