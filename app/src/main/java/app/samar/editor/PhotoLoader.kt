package app.samar.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

object PhotoLoader {

    /** Long edge of the live-preview copy. The full-resolution pass happens on export. */
    const val PREVIEW_MAX_SIDE = 1600

    /**
     * Decodes [uri] at most [maxSide] pixels on the long edge and turns it upright
     * using its EXIF orientation (so face boxes line up on portrait photos).
     */
    suspend fun load(context: Context, uri: Uri, maxSide: Int = PREVIEW_MAX_SIDE): Bitmap =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Not an image" }

            // Decode at the smallest power-of-two size that is still >= maxSide.
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = true
            }
            var bitmap = resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, opts) }
                ?: error("Could not decode image")

            // Exact scale down to maxSide.
            val long = max(bitmap.width, bitmap.height)
            if (long > maxSide) {
                val s = maxSide.toFloat() / long
                bitmap = Bitmap.createScaledBitmap(
                    bitmap, (bitmap.width * s).roundToInt(), (bitmap.height * s).roundToInt(), true,
                )
            }

            val orientation = resolver.openInputStream(uri).use { stream ->
                stream?.let {
                    ExifInterface(it).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL,
                    )
                }
            } ?: ExifInterface.ORIENTATION_NORMAL

            applyOrientation(bitmap, orientation)
        }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> { m.setRotate(180f); m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSPOSE -> { m.setRotate(90f); m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> m.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { m.setRotate(-90f); m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> m.setRotate(-90f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    }
}
