package app.samar.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.samar.effects.Effect
import app.samar.ui.components.ClayPerson.drawClayPerson
import app.samar.ui.theme.Samar

/** Swatch circle diameter. */
private val SwatchSize = 56.dp

/** Strength used for the swatch previews; a bit above default so it reads at this size. */
private const val SWATCH_INTENSITY = 0.85f

/**
 * Horizontally scrolling row of effect swatches. The row bleeds to the screen edges,
 * so the last swatch is cut off as a "scrolls" cue.
 */
@Composable
fun EffectRow(
    selected: Effect,
    onSelect: (Effect) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The soft-3D clay person from the Home hero, rendered once per effect with the real effect code.
    val density = LocalDensity.current
    val px = with(density) { SwatchSize.roundToPx() }
    var faces by remember { mutableStateOf<Map<Effect, ImageBitmap>?>(null) }
    LaunchedEffect(px) { faces = renderBlurredFaces(px, density, SWATCH_INTENSITY) }

    LazyRow(
        modifier = modifier.selectableGroup(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(Effect.entries) { effect ->
            EffectSwatch(
                effect = effect,
                selected = effect == selected,
                onClick = { onSelect(effect) },
                face = faces?.get(effect),
            )
        }
    }
}

/**
 * 56dp clay-person circle in a 72dp slot, with a glass sheen like a marble.
 * 56dp rounded-square clay-person tile in a 72dp slot, with a glass sheen.
 * Selected = 3dp gap + 3dp ring with a soft glow, in the emphasis colour (Neon in dark, Navy in light); on selection it pops up slightly
 * and the person glances left and right once with a small head tilt. Still when "Remove animations" is on.
 */
@Composable
fun EffectSwatch(
    effect: Effect,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    face: ImageBitmap? = null,
) {
    val c = Samar.colors
    val context = LocalContext.current
    val still = remember { animationsOff(context) }

    val scale by animateFloatAsState(
        if (selected) 1.06f else 1f,
        if (still) spring(stiffness = Spring.StiffnessHigh)
        else spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow),
        label = "swatchPop",
    )
    val ring by animateFloatAsState(if (selected) 1f else 0f, label = "swatchRing")
    val turn = remember { Animatable(0f) }
    // Skip the glance for the swatch that is selected when the editor first opens.
    var seen by remember { mutableStateOf(false) }
    LaunchedEffect(selected) {
        if (selected && seen && !still) {
            turn.animateTo(0f, keyframes {
                durationMillis = 620
                -0.8f at 140 using FastOutSlowInEasing
                0.8f at 380 using FastOutSlowInEasing
                0f at 620 using FastOutSlowInEasing
            })
        }
        seen = true
    }

    Column(
        modifier
            .width(72.dp)
            .alpha(if (effect.available) 1f else 0.38f)
            .selectable(
                selected = selected,
                enabled = effect.available,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics { if (!effect.available) stateDescription = "Not available" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
            val accent = c.emphasis
            val outline = glassHairline()
            Canvas(
                Modifier
                    .size(68.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale },
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val r = (SwatchSize / 2).toPx()
                // Rounded square ("squircle-ish"), 18dp corners on the 56dp tile.
                val corner = 18.dp.toPx()
                if (ring > 0f) {
                    // 3dp gap is just empty space; the ring sits outside it, with a soft glow.
                    val g = 4.5.dp.toPx()
                    drawRoundRect(
                        accent.copy(alpha = 0.25f * ring), topLeft = Offset(center.x - r - g - 3.dp.toPx(), center.y - r - g - 3.dp.toPx()),
                        size = Size((r + g + 3.dp.toPx()) * 2, (r + g + 3.dp.toPx()) * 2), cornerRadius = CornerRadius(corner + g + 3.dp.toPx()),
                        style = Stroke(4.dp.toPx()),
                    )
                    drawRoundRect(
                        accent.copy(alpha = ring), topLeft = Offset(center.x - r - g, center.y - r - g),
                        size = Size((r + g) * 2, (r + g) * 2), cornerRadius = CornerRadius(corner + g),
                        style = Stroke(3.dp.toPx() * (0.6f + 0.4f * ring)),
                    )
                }
                val circle = Path().apply { addRoundRect(RoundRect(Rect(center, r), CornerRadius(corner))) }
                clipPath(circle) {
                    translate(left = center.x - r, top = center.y - r) {
                        drawSwatchPerson(r * 2, turn.value, face)
                    }
                    drawMarbleSheen(center, r)
                }
                drawRoundRect(
                    if (selected) Color.White.copy(alpha = 0.35f) else outline,
                    topLeft = Offset(center.x - r, center.y - r), size = Size(r * 2, r * 2),
                    cornerRadius = CornerRadius(corner), style = Stroke(1.dp.toPx()),
                )
            }
        }
        Text(
            effect.label,
            style = Samar.type.label,
            color = if (selected) c.emphasis else c.mutedText,
            maxLines = 1,
        )
        Spacer(Modifier.height(4.dp))
    }
}

/** The clay person at [s] px, with the effect's face pasted over the head (it follows the turn). */
private fun DrawScope.drawSwatchPerson(s: Float, turn: Float, face: ImageBitmap?) {
    // drawClayPerson scales to the current draw size, so draw into an s x s area.
    withDrawSize(Size(s, s)) {
        // The face is blurred, so a glance of the eyes alone wouldn't show: the whole portrait
        // tilts a few degrees with the turn (a little wobble), around the circle's centre so no
        // edge is ever exposed.
        rotate(degrees = turn * 7f, pivot = Offset(s / 2f, s / 2f)) {
            drawClayPerson(turn)
            if (face != null) {
                val u = s / ClayPerson.UNITS
                val f = ClayPerson.faceBounds()
                val mask = Path().apply {
                    addOval(Rect(f.left * u, f.top * u, f.right * u, f.bottom * u).translate(turn * 4f * u, 0f))
                }
                clipPath(mask) {
                    // The pre-rendered face is for a still head; shift it with the head as it turns.
                    translate(left = turn * 4f * u) { drawImage(face) }
                }
            }
        }
    }
}

private inline fun DrawScope.withDrawSize(area: Size, block: DrawScope.() -> Unit) {
    val old = drawContext.size
    drawContext.size = area
    block()
    drawContext.size = old
}

/** Glossy top highlight and a soft inner shade at the bottom: reads as a glass marble. */
private fun DrawScope.drawMarbleSheen(center: Offset, r: Float) {
    drawCircle(
        Brush.radialGradient(
            listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.22f)),
            center = Offset(center.x, center.y - r * 0.25f), radius = r * 1.25f,
        ),
        radius = r, center = center,
    )
    drawOval(
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0f)),
            startY = center.y - r * 0.92f, endY = center.y - r * 0.15f,
        ),
        topLeft = Offset(center.x - r * 0.62f, center.y - r * 0.92f),
        size = Size(r * 1.24f, r * 0.78f),
    )
}
