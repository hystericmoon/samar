package app.samar.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.samar.editor.FaceRegion
import app.samar.editor.RegionShape
import app.samar.effects.Effect
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/** A blur area saved relative to the photo size (0..1), so it fits any decode size. */
data class SavedRegion(
    val left: Float, val top: Float, val right: Float, val bottom: Float,
    val blurred: Boolean,
    val shape: RegionShape,
) {
    fun toRegion(id: Int, width: Int, height: Int) = FaceRegion(
        id = id,
        bounds = RectF(left * width, top * height, right * width, bottom * height),
        blurred = blurred,
        shape = shape,
    )
}

/** One saved photo in Recent. */
data class RecentEntry(
    val id: String,
    val savedAt: Long,
    val source: Uri,
    val effect: Effect,
    val intensity: Float,
    val regions: List<SavedRegion>,
    val thumbnail: File,
)

/**
 * The Recent list. Lives only in the app's private storage: a small JSON index and
 * one thumbnail per entry (the blurred result, never the original).
 */
class RecentStore private constructor(private val context: Context) {

    private val dir = File(context.filesDir, "recent").apply { mkdirs() }
    private val index = File(dir, "index.json")

    /** Newest first. */
    var entries by mutableStateOf(load()); private set

    fun find(id: String?) = entries.firstOrNull { it.id == id }

    /**
     * Records a save. Saving the same original again replaces its entry, so editing
     * and re-saving a photo does not create duplicates.
     */
    fun add(
        source: Uri,
        effect: Effect,
        intensity: Float,
        faces: List<FaceRegion>,
        imageWidth: Int,
        imageHeight: Int,
        result: Bitmap,
    ) {
        keepAccess(source)
        val id = UUID.randomUUID().toString()
        val thumb = File(dir, "$id.jpg")
        thumb.outputStream().use { thumbnailOf(result).compress(Bitmap.CompressFormat.JPEG, 85, it) }

        val regions = faces.map {
            SavedRegion(
                it.bounds.left / imageWidth, it.bounds.top / imageHeight,
                it.bounds.right / imageWidth, it.bounds.bottom / imageHeight,
                it.blurred, it.shape,
            )
        }
        val entry = RecentEntry(id, System.currentTimeMillis(), source, effect, intensity, regions, thumb)

        val replaced = entries.filter { it.source == source }
        replaced.forEach { it.thumbnail.delete() }
        var next = listOf(entry) + (entries - replaced.toSet())
        if (next.size > MAX_ENTRIES) {
            next.drop(MAX_ENTRIES).forEach { forget(it, next) }
            next = next.take(MAX_ENTRIES)
        }
        commit(next)
    }

    /** Removes one entry and its thumbnail. The saved photo in the gallery is untouched. */
    fun remove(id: String) {
        val entry = find(id) ?: return
        forget(entry, entries)
        commit(entries - entry)
    }

    fun clear() {
        entries.forEach { forget(it, emptyList()) }
        commit(emptyList())
    }

    private fun forget(entry: RecentEntry, remaining: List<RecentEntry>) {
        entry.thumbnail.delete()
        // Give back read access to the original once nothing in Recent needs it.
        if (remaining.none { it !== entry && it.source == entry.source }) {
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    entry.source, Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
    }

    /** Keeps read access to the original across restarts, so it can be reopened. */
    private fun keepAccess(source: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(source, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun thumbnailOf(b: Bitmap): Bitmap {
        val s = THUMB_SIDE.toFloat() / max(b.width, b.height)
        return if (s >= 1f) b
        else Bitmap.createScaledBitmap(b, (b.width * s).roundToInt(), (b.height * s).roundToInt(), true)
    }

    private fun commit(list: List<RecentEntry>) {
        entries = list
        val json = JSONArray()
        list.forEach { e ->
            json.put(JSONObject().apply {
                put("id", e.id)
                put("savedAt", e.savedAt)
                put("source", e.source.toString())
                put("effect", e.effect.name)
                put("intensity", e.intensity.toDouble())
                put("thumbnail", e.thumbnail.name)
                put("regions", JSONArray().apply {
                    e.regions.forEach { r ->
                        put(JSONObject().apply {
                            put("l", r.left.toDouble()); put("t", r.top.toDouble())
                            put("r", r.right.toDouble()); put("b", r.bottom.toDouble())
                            put("blurred", r.blurred); put("shape", r.shape.name)
                        })
                    }
                })
            })
        }
        index.writeText(json.toString())
    }

    private fun load(): List<RecentEntry> = runCatching {
        if (!index.exists()) return emptyList()
        val json = JSONArray(index.readText())
        (0 until json.length()).mapNotNull { i ->
            val o = json.getJSONObject(i)
            val thumb = File(dir, o.getString("thumbnail"))
            if (!thumb.exists()) return@mapNotNull null
            val regions = o.getJSONArray("regions").let { arr ->
                (0 until arr.length()).map { j ->
                    val r = arr.getJSONObject(j)
                    SavedRegion(
                        r.getDouble("l").toFloat(), r.getDouble("t").toFloat(),
                        r.getDouble("r").toFloat(), r.getDouble("b").toFloat(),
                        r.getBoolean("blurred"), RegionShape.valueOf(r.getString("shape")),
                    )
                }
            }
            RecentEntry(
                id = o.getString("id"),
                savedAt = o.getLong("savedAt"),
                source = Uri.parse(o.getString("source")),
                effect = Effect.valueOf(o.getString("effect")),
                intensity = o.getDouble("intensity").toFloat(),
                regions = regions,
                thumbnail = thumb,
            )
        }
    }.getOrDefault(emptyList())

    companion object {
        /** "A small local list." */
        private const val MAX_ENTRIES = 30
        private const val THUMB_SIDE = 480

        @Volatile private var instance: RecentStore? = null

        fun get(context: Context): RecentStore =
            instance ?: synchronized(this) {
                instance ?: RecentStore(context.applicationContext).also { instance = it }
            }
    }
}
