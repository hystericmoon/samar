package app.samar.ui.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

const val MAX_ZOOM = 3f
const val DOUBLE_TAP_ZOOM = 2.5f

/**
 * Where the photo sits on screen. The zoom and the photo point at the centre of the
 * view are kept in the ViewModel, so the editor and Add missed face share them even
 * though their photo areas are different sizes.
 */
@Immutable
class ViewTransform(
    private val imageW: Int,
    private val imageH: Int,
    private val viewW: Float,
    private val viewH: Float,
    val zoom: Float,
    center: Offset?,
) {
    val fit = minOf(viewW / imageW, viewH / imageH)
    /** View pixels per photo pixel. */
    val k = fit * zoom
    val fitOrigin = Offset((viewW - imageW * fit) / 2f, (viewH - imageH * fit) / 2f)

    /** Top-left of the photo in view pixels, clamped so the photo covers the view. */
    val origin: Offset = run {
        val c = center ?: Offset(imageW / 2f, imageH / 2f)
        clamp(Offset(viewW / 2f - c.x * k, viewH / 2f - c.y * k))
    }

    fun toView(x: Float, y: Float) = Offset(origin.x + x * k, origin.y + y * k)
    fun toImage(v: Offset) = Offset((v.x - origin.x) / k, (v.y - origin.y) / k)

    /** Photo point now at the centre of the view. */
    val center: Offset get() = toImage(Offset(viewW / 2f, viewH / 2f))

    /**
     * New (zoom, centre) after zooming to [newZoom] around [focus] and panning by [pan],
     * keeping the photo point under the fingers in place.
     */
    fun zoomedAround(focus: Offset, newZoom: Float, pan: Offset = Offset.Zero): Pair<Float, Offset> {
        val z = newZoom.coerceIn(1f, MAX_ZOOM)
        val newOrigin = focus - (focus - origin) * (z / zoom) + pan
        val next = ViewTransform(imageW, imageH, viewW, viewH, z, null)
        // Convert the wanted origin back into a centre point, via a clamp at the new zoom.
        val clamped = next.clamp(newOrigin)
        val newK = fit * z
        return z to Offset((viewW / 2f - clamped.x) / newK, (viewH / 2f - clamped.y) / newK)
    }

    private fun clamp(o: Offset): Offset {
        fun axis(view: Float, content: Float, value: Float): Float =
            if (content <= view) (view - content) / 2f else value.coerceIn(view - content, 0f)
        return Offset(axis(viewW, imageW * k, o.x), axis(viewH, imageH * k, o.y))
    }
}

/** Draws [image] placed by [t]. */
@Composable
fun TransformedPhoto(image: ImageBitmap, t: ViewTransform, description: String) {
    val density = LocalDensity.current
    Image(
        bitmap = image,
        contentDescription = description,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .offset { IntOffset(t.fitOrigin.x.roundToInt(), t.fitOrigin.y.roundToInt()) }
            .size(
                with(density) { (image.width * t.fit).toDp() },
                with(density) { (image.height * t.fit).toDp() },
            )
            .graphicsLayer {
                transformOrigin = TransformOrigin(0f, 0f)
                scaleX = t.zoom; scaleY = t.zoom
                translationX = t.origin.x - t.fitOrigin.x
                translationY = t.origin.y - t.fitOrigin.y
            },
    )
}
