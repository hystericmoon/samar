package app.samar.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import app.samar.editor.FaceRegion
import app.samar.editor.PhotoLoader
import app.samar.effects.Effect
import app.samar.effects.EffectRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Options from the Export screen. */
data class ExportOptions(
    val removeMetadata: Boolean = true,
    val keepOriginalResolution: Boolean = true,
)

/**
 * Builds the final photo on the device: full-resolution decode, the same effects as the
 * preview, JPEG encode, optional metadata copy. No network anywhere.
 */
object Exporter {

    /** Long edge when "Keep original resolution" is on (covers 50 MP sensors). */
    private const val ORIGINAL_MAX_SIDE = 8192
    /** Long edge when it is off ("a smaller file"). */
    private const val SMALL_MAX_SIDE = 2048
    private const val JPEG_QUALITY = 92

    /** Album folder in the gallery. */
    const val ALBUM = "samar"

    /**
     * Renders the export and writes it to a private temp JPEG.
     * @param previewWidth width of the preview bitmap the [faces] coordinates refer to.
     */
    suspend fun renderToTempFile(
        context: Context,
        source: Uri,
        faces: List<FaceRegion>,
        previewWidth: Int,
        effect: Effect,
        intensity: Float,
        options: ExportOptions,
        solidColor: Int = EffectRenderer.SOLID_FILL,
    ): File = withContext(Dispatchers.Default) {
        val maxSide = if (options.keepOriginalResolution) ORIGINAL_MAX_SIDE else SMALL_MAX_SIDE
        val full = try {
            PhotoLoader.load(context, source, maxSide)
        } catch (e: OutOfMemoryError) {
            // Very large photo on a phone with little memory: fall back to half size.
            PhotoLoader.load(context, source, maxSide / 2)
        }
        val photo = if (full.isMutable) full else full.copy(Bitmap.Config.ARGB_8888, true).also { full.recycle() }

        // Faces were found on the preview copy; scale them to this size.
        val s = photo.width.toFloat() / previewWidth
        val scaled = faces.map { f ->
            f.copy(bounds = RectF(f.bounds.left * s, f.bounds.top * s, f.bounds.right * s, f.bounds.bottom * s))
        }
        EffectRenderer.renderInPlace(photo, scaled, effect, intensity, solidColor)

        val file = File(exportDir(context), "samar_${timestamp()}.jpg")
        withContext(Dispatchers.IO) {
            file.outputStream().use { photo.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            // Bitmap.compress writes no EXIF at all, so the file is already clean.
            if (!options.removeMetadata) copyMetadata(context, source, file)
        }
        photo.recycle()
        file
    }

    /** Copies [file] into the gallery under Pictures/samar. Returns its gallery URI. */
    suspend fun saveToGallery(context: Context, file: File): Uri = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Could not create gallery entry")
            resolver.openOutputStream(uri)!!.use { out -> file.inputStream().use { it.copyTo(out) } }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } else {
            // Android 8–9: write the file directly (needs the storage permission), then index it.
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ALBUM)
            dir.mkdirs()
            val dest = File(dir, file.name)
            file.copyTo(dest, overwrite = true)
            MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), arrayOf("image/jpeg"), null)
            Uri.fromFile(dest)
        }
    }

    /** A content:// URI another app can read, for the share sheet. */
    fun shareUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.exports", file)

    /** Private folder for export files; old ones are removed each time. */
    private fun exportDir(context: Context): File =
        File(context.cacheDir, "exports").apply {
            mkdirs()
            listFiles()?.forEach { if (System.currentTimeMillis() - it.lastModified() > 60 * 60 * 1000) it.delete() }
        }

    private fun timestamp() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    /**
     * Only when "Remove location and camera info" is off: copy date, camera and
     * location tags from the original. Orientation stays "normal" because the
     * pixels are already upright.
     */
    private fun copyMetadata(context: Context, source: Uri, file: File) {
        val original = context.contentResolver.openInputStream(source)?.use { ExifInterface(it) } ?: return
        val out = ExifInterface(file)
        for (tag in KEPT_TAGS) original.getAttribute(tag)?.let { out.setAttribute(tag, it) }
        out.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
        out.saveAttributes()
    }

    private val KEPT_TAGS = listOf(
        ExifInterface.TAG_DATETIME, ExifInterface.TAG_DATETIME_ORIGINAL, ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_OFFSET_TIME, ExifInterface.TAG_OFFSET_TIME_ORIGINAL, ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
        ExifInterface.TAG_MAKE, ExifInterface.TAG_MODEL, ExifInterface.TAG_LENS_MAKE, ExifInterface.TAG_LENS_MODEL,
        ExifInterface.TAG_F_NUMBER, ExifInterface.TAG_EXPOSURE_TIME, ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
        ExifInterface.TAG_FOCAL_LENGTH, ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
        ExifInterface.TAG_WHITE_BALANCE, ExifInterface.TAG_FLASH,
        ExifInterface.TAG_GPS_LATITUDE, ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE, ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE, ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_TIMESTAMP, ExifInterface.TAG_GPS_DATESTAMP,
    )
}
