package app.samar.effects

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import app.samar.editor.FaceRegion
import app.samar.editor.RegionShape
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Smear: a long-exposure "head shake". Only the head moves, so each pixel shows the head's
 * colours averaged along a sideways streak (plus two faint ghost positions), weighted by a soft
 * head mask, and the head shows over the background in proportion to how much of the streak
 * passed over it. Nothing from the background is dragged into the face.
 *
 * Inside the face the result is fully opaque except across the outer padding rim, where it eases
 * to meet the outside smoothly. Streak tails spill a little past the head and fade out.
 *
 * [render] returns a patch whose colour is the smear and whose alpha says how much of it to show,
 * to be drawn over the photo at [Params.patch]. Android 13+ uses the AGSL version in
 * [SmearShader]; older phones use the CPU version here. Both follow the same recipe.
 */
object HeadSmear {

    class Params(region: FaceRegion, intensity: Float, imageW: Int, imageH: Int) {
        val b: RectF = region.bounds
        val fw = b.width()
        val fh = b.height()
        val cx = b.centerX()
        val cy = b.centerY()
        val ra = fw / 2f
        val rb = fh / 2f
        val rect = region.shape == RegionShape.Rectangle
        val strength = fw * (0.5f + 0.9f * intensity)   // streak length
        val spill = fw * (0.3f + 0.25f * intensity)     // how far tails run past the head
        val fine = fh / 80f                             // thin streak scale
        val coarse = fh / 14f                           // broad streak scale
        val up = fh * 0.2f                              // the head (hair) reaches above the face box
        val seed = region.id.toFloat()

        /** The area the smear can touch, clamped to the image. */
        val patch = Rect(
            floor(cx - ra - spill * 1.7f).toInt().coerceAtLeast(0),
            floor(cy - (rb + up) * 1.06f).toInt().coerceAtLeast(0),
            ceil(cx + ra + spill * 1.7f).toInt().coerceAtMost(imageW),
            ceil(cy + rb * 1.06f).toInt().coerceAtMost(imageH),
        )

        /** Pixels the streaks sample: the patch plus a streak length to each side. */
        val source = Rect(
            floor(patch.left - strength).toInt().coerceAtLeast(0),
            patch.top,
            ceil(patch.right + strength).toInt().coerceAtMost(imageW),
            patch.bottom,
        )
    }

    fun render(base: Bitmap, region: FaceRegion, intensity: Float, gpu: Boolean): Pair<Bitmap, Rect>? {
        val p = Params(region, intensity, base.width, base.height)
        if (p.patch.width() < 2 || p.patch.height() < 2) return null
        val patch = if (gpu) SmearShader.render(base, p) else renderCpu(base, p)
        return patch to p.patch
    }

    // --- CPU version (Android 8 to 12) ---

    private fun renderCpu(base: Bitmap, p: Params): Bitmap {
        val src = p.source
        val n = src.width()
        val pw = p.patch.width()
        val ph = p.patch.height()
        val out = IntArray(pw * ph)
        val row = IntArray(n)
        // Running sums along the row of mask, and mask x colour: any window's average in O(1).
        val pm = DoubleArray(n + 1)
        val pr = DoubleArray(n + 1)
        val pg = DoubleArray(n + 1)
        val pb = DoubleArray(n + 1)
        val win = FloatArray(4)

        for (py in 0 until ph) {
            val y = (p.patch.top + py).toFloat() + 0.5f
            base.getPixels(row, 0, n, src.left, p.patch.top + py, n, 1)
            for (i in 0 until n) {
                val m = headMask(p, src.left + i + 0.5f, y)
                val c = row[i]
                pm[i + 1] = pm[i] + m
                pr[i + 1] = pr[i] + m * ((c shr 16) and 0xFF)
                pg[i + 1] = pg[i] + m * ((c shr 8) and 0xFF)
                pb[i + 1] = pb[i] + m * (c and 0xFF)
            }
            val r = rowParams(p, y)
            for (px in 0 until pw) {
                val x = (p.patch.left + px).toFloat() + 0.5f
                val i = x - src.left
                var cov = 0f
                var cr = 0f
                var cg = 0f
                var cb = 0f
                for (k in 0 until 3) {
                    val centre = i + r.drift + GHOST_OFFSET[k] * p.strength
                    val len = max(1f, r.len * GHOST_LENGTH[k])
                    window(pm, pr, pg, pb, n, centre, len, win)
                    val w = GHOST_WEIGHT[k] * win[3]
                    cov += w
                    cr += w * win[0]; cg += w * win[1]; cb += w * win[2]
                }
                val a = opacity(p, x, y, cov, r.reach)
                if (a <= 0.002f || cov <= 1e-4f) continue
                val k = DARKEN / cov
                val ia = (a * 255f).roundToInt().coerceIn(0, 255)
                out[py * pw + px] = (ia shl 24) or
                    ((cr * k).roundToInt().coerceIn(0, 255) shl 16) or
                    ((cg * k).roundToInt().coerceIn(0, 255) shl 8) or
                    (cb * k).roundToInt().coerceIn(0, 255)
            }
        }
        return Bitmap.createBitmap(out, pw, ph, Bitmap.Config.ARGB_8888)
    }

    private class Row(val len: Float, val drift: Float, val reach: Float)

    /** Streak length, a small sideways drift and the tail reach vary row by row (thin speed lines). */
    private fun rowParams(p: Params, y: Float): Row {
        val v = y - p.cy
        val n1 = noise(v / p.fine, 1f, p.seed)
        val n2 = noise(v / p.coarse, 2f, p.seed)
        val n3 = noise(v / p.coarse, 3f, p.seed)
        val len = p.strength * (0.5f + 0.7f * n1 + 0.3f * n2)
        val drift = p.strength * 0.18f * (n3 - 0.5f) * 2f
        val reach = p.spill * (0.7f + 0.45f * noise(v / p.coarse, 4f, p.seed) + 0.15f * noise(v / p.fine, 5f, p.seed))
        return Row(len, drift, reach)
    }

    /** Soft head mask: the face shape, taller upwards for the hair. */
    private fun headMask(p: Params, x: Float, y: Float): Float {
        val rbm = if (y < p.cy) p.rb + p.up else p.rb
        val d = shapeDistance(p.rect, (x - p.cx) / p.ra, (y - p.cy) / rbm)
        return 1f - smoothstep(0.85f, 1.05f, d)
    }

    private fun opacity(p: Params, x: Float, y: Float, cov: Float, reach: Float): Float {
        val dx = (x - p.cx) / p.ra
        val dy = (y - p.cy) / p.rb
        val d = shapeDistance(p.rect, dx, dy)
        val shown = min(1f, cov * 1.15f)
        return if (d <= 1f) {
            // Face: fully covered, easing only across the padding rim.
            shown + (1f - shown) * smoothstep(0f, 0.25f, 1f - d)
        } else {
            val half = if (abs(dy) < 1f) (if (p.rect) p.ra else p.ra * sqrt(1f - dy * dy)) else 0f
            shown * (1f - smoothstep(0f, reach * 1.3f, abs(x - p.cx) - half))
        }
    }

    private fun window(pm: DoubleArray, pr: DoubleArray, pg: DoubleArray, pb: DoubleArray, n: Int, c: Float, len: Float, out: FloatArray) {
        val l = (c - len / 2f).roundToInt().coerceIn(0, n - 1)
        val r = (c + len / 2f).roundToInt().coerceIn(l + 1, n)
        val m = pm[r] - pm[l]
        if (m < 1e-6) { out[0] = 0f; out[1] = 0f; out[2] = 0f; out[3] = 0f; return }
        out[0] = ((pr[r] - pr[l]) / m).toFloat()
        out[1] = ((pg[r] - pg[l]) / m).toFloat()
        out[2] = ((pb[r] - pb[l]) / m).toFloat()
        out[3] = min(1.0, m / max(1f, len)).toFloat()
    }

    // Main streak, then the head caught at two other positions (ghosts).
    internal val GHOST_OFFSET = floatArrayOf(0f, 0.3f, -0.26f)
    internal val GHOST_LENGTH = floatArrayOf(1f, 0.2f, 0.2f)
    internal val GHOST_WEIGHT = floatArrayOf(0.6f, 0.22f, 0.18f)
    internal const val DARKEN = 0.93f

    private fun shapeDistance(rect: Boolean, dx: Float, dy: Float) =
        if (rect) max(abs(dx), abs(dy)) else sqrt(dx * dx + dy * dy)

    private fun smoothstep(e0: Float, e1: Float, x: Float): Float {
        val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** Smooth 1D value noise in 0..1 (same formula as the shader). */
    private fun noise(t: Float, channel: Float, seed: Float): Float {
        val i = floor(t)
        val f = t - i
        val u = f * f * (3f - 2f * f)
        val a = hash(i, channel, seed)
        return a + (hash(i + 1f, channel, seed) - a) * u
    }

    private fun hash(n: Float, channel: Float, seed: Float): Float {
        val s = sin(n * 12.9898f + channel * 78.233f + seed * 37.719f) * 43758.5453f
        return s - floor(s)
    }
}
