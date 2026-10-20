package app.samar.ui.components

import android.content.Context
import android.graphics.RectF
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.samar.editor.FaceRegion
import app.samar.effects.Effect
import app.samar.effects.EffectRenderer
import app.samar.ui.components.ClayPerson.drawClayPerson
import app.samar.ui.theme.Overlay
import app.samar.ui.theme.Samar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

// Timeline of one loop, in seconds.
private const val IDLE_END = 1.2f       // glance left and right, then hold still
private const val BOX_START = 1.2f      // face box draws itself
private const val BOX_END = 1.75f
private const val BADGE_START = 1.65f   // check badge pops in
private const val BADGE_END = 2.0f
private const val SWEEP_START = 2.0f    // blur sweeps across the face
private const val SWEEP_END = 2.8f
private const val FADE_START = 3.4f     // box and badge fade out
private const val FADE_END = 3.8f
private const val WIPE_START = 3.8f     // banded slit-scan wipe back to the clear face
private const val WIPE_END = 4.7f
private const val LOOP_MS = 5200L

/** Frame shown when the system's "Remove animations" setting is on: detected and blurred. */
private const val STILL_TIME = 3.0f

private const val BANDS = 6
private const val INTENSITY = 0.8f

/**
 * Home hero: a soft-3D person who gets detected and blurred, looping through the
 * five effects. The blurred faces are made once with the app's real effect code.
 * Decorative (hidden from TalkBack). Pauses when the app is in the background,
 * and holds a single frame when the system asks for no animations.
 */
@Composable
fun HeroAnimation(modifier: Modifier = Modifier, size: Dp = 196.dp) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val still = remember { animationsOff(context) }
    val px = with(density) { size.roundToPx() }

    var blurred by remember { mutableStateOf<Map<Effect, ImageBitmap>?>(null) }
    LaunchedEffect(px) { blurred = renderBlurredFaces(px, density, INTENSITY) }

    // Only advances while the app is on screen, so it picks up where it left off.
    var timeMs by remember { mutableLongStateOf(0L) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    if (!still) {
        LaunchedEffect(lifecycle) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                var last = -1L
                while (true) {
                    withFrameMillis { now ->
                        if (last >= 0) timeMs += now - last
                        last = now
                    }
                }
            }
        }
    }

    val c = Samar.colors
    Canvas(modifier.size(size)) {
        val t = if (still) STILL_TIME else (timeMs % LOOP_MS) / 1000f
        val effect = Effect.entries[if (still) 0 else ((timeMs / LOOP_MS) % Effect.entries.size).toInt()]
        drawFrame(t, blurred?.get(effect), gap = c.accentTint, badge = Overlay.Highlight, onBadge = Overlay.Ink)
    }
}

private fun DrawScope.drawFrame(t: Float, blurred: ImageBitmap?, gap: Color, badge: Color, onBadge: Color) {
    val u = size.width / ClayPerson.UNITS
    val face = ClayPerson.faceBounds().let { Rect(it.left * u, it.top * u, it.right * u, it.bottom * u) }
    val circle = Path().apply { addOval(Rect(Offset.Zero, size)) }

    clipPath(circle) {
        if (t >= WIPE_START && t < WIPE_END && blurred != null) {
            drawBandWipe(progress(t, WIPE_START, WIPE_END), blurred, gap)
        } else {
            val turn = if (t < IDLE_END) {
                val x = t / IDLE_END
                0.8f * sin(2 * PI.toFloat() * x) * sin(PI.toFloat() * x)
            } else 0f
            drawClayPerson(turn)

            if (blurred != null && t >= SWEEP_START && t < WIPE_START) {
                val sweep = FastOutSlowInEasing.transform(progress(t, SWEEP_START, SWEEP_END))
                val x = face.left + (face.right - face.left) * sweep
                clipRect(right = x) { drawImage(blurred) }
                if (sweep < 1f) {
                    // Soft light at the sweep edge.
                    val glow = sin(PI.toFloat() * sweep)
                    drawLine(
                        Color.White.copy(alpha = 0.25f * glow), Offset(x, face.top), Offset(x, face.bottom),
                        strokeWidth = 8f * u, cap = StrokeCap.Round,
                    )
                    drawLine(
                        Color.White.copy(alpha = 0.85f * glow), Offset(x, face.top), Offset(x, face.bottom),
                        strokeWidth = 1.6f * u, cap = StrokeCap.Round,
                    )
                }
            }
        }
    }

    // Face box and check badge, as in the editor.
    if (t >= BOX_START && t < FADE_END) {
        val alpha = 1f - progress(t, FADE_START, FADE_END)
        val draw = FastOutSlowInEasing.transform(progress(t, BOX_START, BOX_END))
        drawFaceBox(face, draw, alpha, u)
        if (t >= BADGE_START) {
            // On the box's top-right corner, so it stays inside the circle.
            drawBadge(Offset(face.right - 4f * u, face.top + 4f * u), easeOutBack(progress(t, BADGE_START, BADGE_END)), alpha, u, badge, onBadge)
        }
    }
}

/** Six bands slide sideways one after another, swapping the blurred face for the clear one. */
private fun DrawScope.drawBandWipe(p: Float, blurred: ImageBitmap, gap: Color) {
    val u = size.width / ClayPerson.UNITS
    val gapPx = 2f * u
    val bandH = (size.height - gapPx * (BANDS - 1)) / BANDS
    val stagger = 0.08f
    for (i in 0 until BANDS) {
        val q = ((p - i * stagger) / (1f - (BANDS - 1) * stagger)).coerceIn(0f, 1f)
        val shift = sin(PI.toFloat() * q) * 16f * u * if (i % 2 == 0) 1f else -1f
        val top = i * (bandH + gapPx)
        clipRect(top = top, bottom = top + bandH) {
            translate(left = shift) {
                if (q < 0.5f) drawImage(blurred) else drawClayPerson(0f)
            }
        }
    }
    // Thin gaps between bands, strongest mid-wipe.
    val gapAlpha = sin(PI.toFloat() * p)
    for (i in 1 until BANDS) {
        val y = i * (bandH + gapPx) - gapPx
        drawRect(gap.copy(alpha = gapAlpha), topLeft = Offset(0f, y), size = Size(size.width, gapPx))
    }
}

private fun DrawScope.drawFaceBox(face: Rect, drawn: Float, alpha: Float, u: Float) {
    val path = Path().apply { addRoundRect(RoundRect(face, CornerRadius(14f * u))) }
    val partial = Path()
    PathMeasure().apply {
        setPath(path, false)
        getSegment(0f, length * drawn, partial, true)
    }
    drawPath(partial, Color.Black.copy(alpha = 0.45f * alpha), style = Stroke(4f * u, cap = StrokeCap.Round))
    drawPath(partial, Overlay.Highlight.copy(alpha = alpha), style = Stroke(2f * u, cap = StrokeCap.Round))
}

private fun DrawScope.drawBadge(center: Offset, scale: Float, alpha: Float, u: Float, fill: Color, mark: Color) {
    val r = 13f * u * scale
    if (r <= 0f) return
    drawCircle(fill.copy(alpha = alpha), radius = r, center = center)
    drawCircle(Overlay.Ink.copy(alpha = alpha), radius = r, center = center, style = Stroke(2f * u))
    val k = r / 13f
    val check = Path().apply {
        moveTo(center.x - 5.5f * k, center.y + 0.5f * k)
        lineTo(center.x - 1.5f * k, center.y + 4.5f * k)
        lineTo(center.x + 6f * k, center.y - 4f * k)
    }
    drawPath(check, mark.copy(alpha = alpha), style = Stroke(2.4f * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/**
 * Draws the still person once and runs every effect over the face with the real renderer.
 * Also used by the editor's effect swatches.
 */
internal suspend fun renderBlurredFaces(px: Int, density: Density, intensity: Float): Map<Effect, ImageBitmap> =
    withContext(Dispatchers.Default) {
        val image = ImageBitmap(px, px)
        CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(image), Size(px.toFloat(), px.toFloat())) {
            drawClayPerson(0f)
        }
        val base = image.asAndroidBitmap()
        val u = px / ClayPerson.UNITS
        val f = ClayPerson.faceBounds()
        val region = FaceRegion(id = 0, bounds = RectF(f.left * u, f.top * u, f.right * u, f.bottom * u))
        Effect.entries.associateWith { effect ->
            EffectRenderer.render(base, listOf(region), effect, intensity).asImageBitmap()
        }
    }

private fun progress(t: Float, start: Float, end: Float) = ((t - start) / (end - start)).coerceIn(0f, 1f)

/** Overshoots slightly, then settles: the badge "pops". */
private fun easeOutBack(x: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    return 1f + c3 * (x - 1f).pow(3) + c1 * (x - 1f).pow(2)
}

/** True when the system's "Remove animations" accessibility setting is on. */
internal fun animationsOff(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
