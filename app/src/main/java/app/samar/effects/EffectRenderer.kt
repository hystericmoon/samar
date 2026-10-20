package app.samar.effects

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import app.samar.editor.FaceRegion
import app.samar.editor.RegionShape
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Draws the chosen effect inside every blurred region of [base] and returns a new bitmap.
 * Works the same on the preview copy and (in step 6) the full-resolution photo,
 * because strength is relative to each face's size, not to pixels.
 *
 * @param intensity 0..1 (the slider's 0..100%).
 */
object EffectRenderer {

    suspend fun render(base: Bitmap, regions: List<FaceRegion>, effect: Effect, intensity: Float): Bitmap {
        val out = base.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        for (region in regions) {
            if (!region.blurred) continue
            currentCoroutineContext().ensureActive() // stop early if the slider moved again
            drawEffect(base, canvas, region, effect, intensity)
        }
        return out
    }

    /**
     * Export version: draws onto [photo] itself (it must be mutable), so a large photo
     * is not held in memory twice. Each effect copies its crop before drawing over it.
     */
    suspend fun renderInPlace(photo: Bitmap, regions: List<FaceRegion>, effect: Effect, intensity: Float) {
        val canvas = Canvas(photo)
        for (region in regions) {
            if (!region.blurred) continue
            currentCoroutineContext().ensureActive()
            drawEffect(photo, canvas, region, effect, intensity)
        }
    }

    /**
     * Renders just one region into a small transparent bitmap (for the live preview while
     * placing a missed face), including its soft edge. Returns the patch and its top-left in photo px.
     */
    fun renderPatch(base: Bitmap, region: FaceRegion, effect: Effect, intensity: Float): Pair<Bitmap, android.graphics.Point> {
        val area = if (effect == Effect.Smear) {
            RectF(HeadSmear.Params(region, intensity, base.width, base.height).patch)
        } else {
            featheredBounds(region)
        }
        val o = android.graphics.Point(floor(area.left).toInt().coerceAtLeast(0), floor(area.top).toInt().coerceAtLeast(0))
        val w = max(1, ceil(area.right).toInt().coerceAtMost(base.width) - o.x)
        val h = max(1, ceil(area.bottom).toInt().coerceAtMost(base.height) - o.y)
        val patch = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(patch)
        canvas.translate(-o.x.toFloat(), -o.y.toFloat())
        drawEffect(base, canvas, region, effect, intensity)
        return patch to o
    }

    /** The soft edge fades out over this share of the face's size, outside the area. */
    private const val FEATHER = 0.15f

    private fun featheredBounds(region: FaceRegion): RectF {
        val f = faceSize(region) * FEATHER
        return RectF(region.bounds).apply { inset(-f, -f) }
    }

    /**
     * Draws the effect over the area plus a ring around it, then fades that ring out with a
     * soft mask. Inside the area the effect stays fully opaque, so the face is never less covered.
     */
    private fun drawEffect(base: Bitmap, canvas: Canvas, region: FaceRegion, effect: Effect, intensity: Float) {
        if (effect == Effect.Smear) {
            // Smear draws its own soft, streaky edge (its tails run further than the other effects' fade).
            val gpu = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !forceSmearFallback
            val (patch, at) = HeadSmear.render(base, region, intensity, gpu) ?: return
            canvas.drawBitmap(patch, at.left.toFloat(), at.top.toFloat(), null)
            patch.recycle()
            return
        }
        val size = faceSize(region)
        val outer = featheredBounds(region)
        val patchRect = Rect(
            floor(outer.left).toInt().coerceAtLeast(0),
            floor(outer.top).toInt().coerceAtLeast(0),
            ceil(outer.right).toInt().coerceAtMost(base.width),
            ceil(outer.bottom).toInt().coerceAtMost(base.height),
        )
        if (patchRect.width() < 2 || patchRect.height() < 2) return

        // 1. The effect, filling the whole grown rectangle. Strength still comes from the real face size.
        val patch = Bitmap.createBitmap(patchRect.width(), patchRect.height(), Bitmap.Config.ARGB_8888)
        val pc = Canvas(patch)
        pc.translate(-patchRect.left.toFloat(), -patchRect.top.toFloat())
        val area = Area(RectF(outer), RegionShape.Rectangle, size)
        when (effect) {
            Effect.Gaussian -> gaussian(base, pc, area, gaussianRadius(size, intensity))
            Effect.Glass -> glass(base, pc, area, intensity)
            Effect.Smear -> Unit // drawn above
            Effect.Pixelate -> pixelate(base, pc, area, intensity)
            Effect.Solid -> solid(pc, area)
        }

        // 2. Mask: the shape blurred outwards, plus the sharp shape on top so the inside is solid.
        val f = size * FEATHER
        val mask = Bitmap.createBitmap(patchRect.width(), patchRect.height(), Bitmap.Config.ARGB_8888)
        val mc = Canvas(mask)
        mc.translate(-patchRect.left.toFloat(), -patchRect.top.toFloat())
        val soft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            maskFilter = BlurMaskFilter(max(0.5f, f / 2f), BlurMaskFilter.Blur.NORMAL)
        }
        drawShape(mc, region.shape, RectF(region.bounds).apply { inset(-f / 2f, -f / 2f) }, soft)
        drawShape(mc, region.shape, region.bounds, Paint(Paint.ANTI_ALIAS_FLAG))
        // Apply it pixel by pixel: blend modes on bitmap canvases differ between Android versions.
        val n = patch.width * patch.height
        val px = IntArray(n)
        val mk = IntArray(n)
        patch.getPixels(px, 0, patch.width, 0, 0, patch.width, patch.height)
        mask.getPixels(mk, 0, patch.width, 0, 0, patch.width, patch.height)
        for (i in 0 until n) {
            val a = ((px[i] ushr 24) * (mk[i] ushr 24) + 127) / 255
            px[i] = (a shl 24) or (px[i] and 0xFFFFFF)
        }
        patch.setPixels(px, 0, patch.width, 0, 0, patch.width, patch.height)
        mask.recycle()

        canvas.drawBitmap(patch, patchRect.left.toFloat(), patchRect.top.toFloat(), null)
        patch.recycle()
    }

    /**
     * Where an effect draws: [bounds] (with [shape]) is the area to fill; [size] is the actual
     * face's size, which sets the strength so the soft edge doesn't change it.
     */
    private class Area(val bounds: RectF, val shape: RegionShape, val size: Float)

    private fun faceSize(region: FaceRegion) = min(region.bounds.width(), region.bounds.height())

    private fun gaussianRadius(size: Float, intensity: Float) = max(2f, size * (0.04f + 0.16f * intensity))

    /** Soft blur. */
    private fun gaussian(base: Bitmap, canvas: Canvas, area: Area, radius: Float) {
        val crop = cropAround(base, area.bounds, margin = radius) ?: return

        // Blur a shrunken copy (fast), then stretch it back with smoothing.
        val shrink = max(1f, radius / 8f)
        val sw = max(2, (crop.width() / shrink).roundToInt())
        val sh = max(2, (crop.height() / shrink).roundToInt())
        val piece = Bitmap.createBitmap(base, crop.left, crop.top, crop.width(), crop.height())
        val small = Bitmap.createScaledBitmap(piece, sw, sh, true)
        boxBlur3(small, max(1, (radius / shrink).roundToInt()))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.shader = placedShader(small, crop)
        drawShape(canvas, area, paint)
    }

    /** Frosted blur with a light white tint. */
    private fun glass(base: Bitmap, canvas: Canvas, area: Area, intensity: Float) {
        gaussian(base, canvas, area, gaussianRadius(area.size, intensity) * 1.2f)
        val tint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = (255 * (0.14f + 0.16f * intensity)).roundToInt()
        }
        drawShape(canvas, area, tint)
    }

    /** Blocky mosaic, grid aligned to the face area. */
    private fun pixelate(base: Bitmap, canvas: Canvas, area: Area, intensity: Float) {
        val block = max(3f, area.size * (0.05f + 0.15f * intensity))
        val crop = cropAround(base, area.bounds, margin = 0f) ?: return
        val cols = max(1, ceil(crop.width() / block).toInt())
        val rows = max(1, ceil(crop.height() / block).toInt())
        val piece = Bitmap.createBitmap(base, crop.left, crop.top, crop.width(), crop.height())
        // Filtering on the way down averages each block; none on the way up keeps hard edges.
        val small = Bitmap.createScaledBitmap(piece, cols, rows, true)
        val shader = BitmapShader(small, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(Matrix().apply {
                setScale(block, block)
                postTranslate(crop.left.toFloat(), crop.top.toFloat())
            })
        }
        // Android 9+ turns bitmap smoothing on by default, which would soften the blocks into a blur.
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = false
            this.shader = shader
        }
        drawShape(canvas, area, paint)
    }

    /** Flat dark fill. Always fully opaque: intensity does not apply. */
    private fun solid(canvas: Canvas, area: Area) {
        drawShape(canvas, area, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SOLID_FILL })
    }

    /** Debug switch to check the older-phone Smear on a new phone. Always false in the app. */
    @Volatile var forceSmearFallback = false

    private fun cropAround(base: Bitmap, b: RectF, margin: Float) = cropAround(base, b, margin, margin)

    private fun cropAround(base: Bitmap, b: RectF, marginX: Float, marginY: Float): Rect? {
        val crop = Rect(
            (b.left - marginX).toInt().coerceAtLeast(0),
            (b.top - marginY).toInt().coerceAtLeast(0),
            ceil(b.right + marginX).toInt().coerceAtMost(base.width),
            ceil(b.bottom + marginY).toInt().coerceAtMost(base.height),
        )
        return if (crop.width() < 2 || crop.height() < 2) null else crop
    }

    /** A shader that draws [bitmap] stretched over [crop] in photo coordinates. */
    private fun placedShader(bitmap: Bitmap, crop: Rect) =
        BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(Matrix().apply {
                setScale(crop.width() / bitmap.width.toFloat(), crop.height() / bitmap.height.toFloat())
                postTranslate(crop.left.toFloat(), crop.top.toFloat())
            })
        }

    private val SOLID_FILL = Color.rgb(0x22, 0x25, 0x2B)

    private fun drawShape(canvas: Canvas, area: Area, paint: Paint) = drawShape(canvas, area.shape, area.bounds, paint)

    private fun drawShape(canvas: Canvas, shape: RegionShape, r: RectF, paint: Paint) {
        when (shape) {
            RegionShape.Ellipse -> canvas.drawOval(r, paint)
            RegionShape.Rectangle -> canvas.drawRect(r, paint)
        }
    }

    /** Three box-blur passes approximate a Gaussian. Modifies [bitmap] in place. */
    internal fun boxBlur3(bitmap: Bitmap, r: Int) {
        val w = bitmap.width
        val h = bitmap.height
        val a = IntArray(w * h)
        val tmp = IntArray(w * h)
        bitmap.getPixels(a, 0, w, 0, 0, w, h)
        repeat(3) {
            blurLine(a, tmp, lines = h, length = w, r = r, lineStride = w, step = 1)
            blurLine(tmp, a, lines = w, length = h, r = r, lineStride = 1, step = w)
        }
        bitmap.setPixels(a, 0, w, 0, 0, w, h)
    }

    /** Sliding-window average along rows (step 1) or columns (step = width). */
    internal fun blurLine(src: IntArray, dst: IntArray, lines: Int, length: Int, r: Int, lineStride: Int, step: Int) {
        val div = 2 * r + 1
        for (line in 0 until lines) {
            val start = line * lineStride
            var rs = 0; var gs = 0; var bs = 0
            for (i in -r..r) {
                val p = src[start + i.coerceIn(0, length - 1) * step]
                rs += (p shr 16) and 0xFF; gs += (p shr 8) and 0xFF; bs += p and 0xFF
            }
            for (i in 0 until length) {
                dst[start + i * step] = (0xFF shl 24) or ((rs / div) shl 16) or ((gs / div) shl 8) or (bs / div)
                val add = src[start + (i + r + 1).coerceAtMost(length - 1) * step]
                val rem = src[start + (i - r).coerceAtLeast(0) * step]
                rs += ((add shr 16) and 0xFF) - ((rem shr 16) and 0xFF)
                gs += ((add shr 8) and 0xFF) - ((rem shr 8) and 0xFF)
                bs += (add and 0xFF) - (rem and 0xFF)
            }
        }
    }
}
