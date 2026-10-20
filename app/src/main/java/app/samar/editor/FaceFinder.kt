package app.samar.editor

import android.graphics.Bitmap
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Blur-area shape. Detected faces are ellipses; Rectangle arrives with "Add missed face". */
enum class RegionShape { Ellipse, Rectangle }

/** One area to hide, in preview-bitmap pixels. */
data class FaceRegion(
    val id: Int,
    val bounds: RectF,
    val blurred: Boolean = true,
    val shape: RegionShape = RegionShape.Ellipse,
    /** Placed by the user with Add missed face (not found by the detector). */
    val manual: Boolean = false,
)

object FaceFinder {

    /** The mask is about 20% larger than the detected box, to cover hairline and ears. */
    private const val PADDING = 1.2f

    /** Runs ML Kit on-device and returns padded regions, all blurred by default. */
    suspend fun detect(bitmap: Bitmap): List<FaceRegion> {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setMinFaceSize(0.05f)
            .build()
        val detector = FaceDetection.getClient(options)
        try {
            val faces = suspendCancellableCoroutine { cont ->
                detector.process(InputImage.fromBitmap(bitmap, 0))
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
            return faces
                .sortedBy { it.boundingBox.left }
                .mapIndexed { i, face -> FaceRegion(id = i, bounds = pad(face.boundingBox.toRectF(), bitmap)) }
        } finally {
            detector.close()
        }
    }

    private fun android.graphics.Rect.toRectF() = RectF(this)

    private fun pad(r: RectF, bitmap: Bitmap): RectF {
        val dw = r.width() * (PADDING - 1f) / 2f
        val dh = r.height() * (PADDING - 1f) / 2f
        return RectF(
            (r.left - dw).coerceAtLeast(0f),
            (r.top - dh).coerceAtLeast(0f),
            (r.right + dw).coerceAtMost(bitmap.width.toFloat()),
            (r.bottom + dh).coerceAtMost(bitmap.height.toFloat()),
        )
    }
}
