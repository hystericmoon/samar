package app.samar.editor

import android.app.Application
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.samar.data.RecentEntry
import app.samar.data.RecentStore
import app.samar.data.SelectionMode
import app.samar.data.SelectionPreference
import app.samar.effects.Effect
import app.samar.effects.EffectRenderer
import app.samar.export.ExportOptions
import app.samar.export.Exporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Everything the user can undo. */
data class EditSnapshot(
    val faces: List<FaceRegion>,
    val effect: Effect,
    val intensity: Float,
)

private const val MIN_DRAFT = 16f

class EditorViewModel(app: Application) : AndroidViewModel(app) {

    var uri: Uri? by mutableStateOf(null); private set
    var loading by mutableStateOf(false); private set
    var failed by mutableStateOf(false); private set

    /** Upright, downscaled photo used for preview and detection. */
    var base: Bitmap? by mutableStateOf(null); private set
    /** [base] with the effect applied. */
    var rendered: Bitmap? by mutableStateOf(null); private set

    var faces by mutableStateOf(emptyList<FaceRegion>()); private set
    var effect by mutableStateOf(Effect.Gaussian); private set
    /** 0..1; default 70%. */
    var intensity by mutableStateOf(0.7f); private set
    var showOriginal by mutableStateOf(false); private set
    /** Preview: hides the face boxes and toggles so the result can be seen clean. */
    var hideOverlays by mutableStateOf(false); private set

    private val selectionPref = SelectionPreference(app)
    /** Auto blurs every face found; Manual finds them but leaves the choice to the user. Remembered. */
    var selectionMode by mutableStateOf(selectionPref.load()); private set

    /** Shared zoom (1..3) and the photo point at the centre of the view; null = photo centre. */
    var viewZoom by mutableFloatStateOf(1f); private set
    var viewCenter: Offset? by mutableStateOf(null); private set

    fun setViewport(zoom: Float, center: Offset) {
        viewZoom = zoom
        viewCenter = center
    }

    // --- Add missed face ---

    /** The blur area being placed, or null when not on the Add missed face screen. */
    var draft: FaceRegion? by mutableStateOf(null); private set
    /** Set when the placement screen is adjusting an existing face instead of adding one. */
    var editingId: Int? by mutableStateOf(null); private set
    /** Live effect preview for [draft]: the patch and the photo-px point it is drawn at. */
    var draftPatch: Pair<Bitmap, android.graphics.Point>? by mutableStateOf(null); private set
    private var draftJob: Job? = null

    /** Opens Add missed face with a shape centred on what the user is looking at. */
    fun startDraft() {
        val b = base ?: return
        val c = viewCenter ?: Offset(b.width / 2f, b.height / 2f)
        val s = minOf(b.width, b.height) * 0.22f / viewZoom
        val w = s * 0.82f
        setDraftBounds(RectF(c.x - w / 2, c.y - s / 2, c.x + w / 2, c.y + s / 2), RegionShape.Ellipse)
    }

    /** Opens the placement screen on an existing face, to resize, move or reshape its blur. */
    fun startEdit(id: Int) {
        val face = faces.firstOrNull { it.id == id } ?: return
        editingId = id
        setDraftBounds(face.bounds, face.shape)
    }

    fun setDraftShape(shape: RegionShape) {
        val d = draft ?: return
        setDraftBounds(d.bounds, shape)
    }

    /** Moves/resizes the draft, kept inside the photo and at least [MIN_DRAFT] px. */
    fun setDraftBounds(r: RectF, shape: RegionShape = draft?.shape ?: RegionShape.Ellipse) {
        val b = base ?: return
        val w = r.width().coerceIn(MIN_DRAFT, b.width.toFloat())
        val h = r.height().coerceIn(MIN_DRAFT, b.height.toFloat())
        val left = r.left.coerceIn(0f, b.width - w)
        val top = r.top.coerceIn(0f, b.height - h)
        val region = FaceRegion(id = editingId ?: nextId(), bounds = RectF(left, top, left + w, top + h), shape = shape)
        draft = region
        val e = effect; val i = intensity
        draftJob?.cancel()
        draftJob = viewModelScope.launch {
            draftPatch = withContext(Dispatchers.Default) {
                EffectRenderer.renderPatch(b, region, e, i)
            }
        }
    }

    fun commitDraft() {
        val d = draft ?: return
        remember()
        val id = editingId
        faces = if (id != null) {
            // Adjusting turns the blur on: the user is shaping it to hide that face.
            faces.map { if (it.id == id) it.copy(bounds = d.bounds, shape = d.shape, blurred = true) else it }
        } else {
            faces + d.copy(manual = true)
        }
        closeDraft()
        rerender()
    }

    fun cancelDraft() = closeDraft()

    private fun closeDraft() {
        draftJob?.cancel()
        draft = null
        draftPatch = null
        editingId = null
    }

    private fun nextId() = (faces.maxOfOrNull { it.id } ?: -1) + 1

    // --- Export ---

    var exportOpen by mutableStateOf(false); private set
    var exportOptions by mutableStateOf(ExportOptions())
    var exportStatus: ExportStatus by mutableStateOf(ExportStatus.Idle); private set

    /**
     * True once the current edits have been saved or shared. Any new edit (or undo)
     * clears it, so "Done" can warn before unsaved edits are thrown away.
     */
    var exportedSinceEdit by mutableStateOf(false); private set

    fun openExport() { exportOpen = true; exportStatus = ExportStatus.Idle }
    fun closeExport() { exportOpen = false }

    /** Full-resolution pass, then into Pictures/samar. */
    fun saveToGallery() = export(ExportStatus.Saving) { file ->
        Exporter.saveToGallery(getApplication(), file)
        file.delete()
        recordInRecent()
        exportedSinceEdit = true
        ExportStatus.Saved
    }

    /** Adds this photo (blurred thumbnail + edits) to Recent, on the device only. */
    private fun recordInRecent() {
        val source = uri ?: return
        val b = base ?: return
        RecentStore.get(getApplication()).add(
            source, effect, intensity, faces, b.width, b.height, rendered ?: b,
        )
    }

    /** Full-resolution pass, then [onReady] gets a URI for the share sheet. */
    fun share(onReady: (Uri) -> Unit) = export(ExportStatus.PreparingShare) { file ->
        onReady(Exporter.shareUri(getApplication(), file))
        exportedSinceEdit = true
        ExportStatus.Idle
    }

    private fun export(working: ExportStatus, then: suspend (java.io.File) -> ExportStatus) {
        val source = uri ?: return
        val preview = base ?: return
        if (exportStatus.isWorking) return
        exportStatus = working
        viewModelScope.launch {
            exportStatus = try {
                val file = Exporter.renderToTempFile(
                    getApplication(), source, faces, preview.width, effect, intensity, exportOptions,
                )
                then(file)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                ExportStatus.Failed
            }
        }
    }

    private val history = ArrayDeque<EditSnapshot>()
    var canUndo by mutableStateOf(false); private set

    private var intensityAtDragStart: Float? = null
    private var renderJob: Job? = null
    private var loadJob: Job? = null

    val blurredCount get() = faces.count { it.blurred }

    /** True when this photo was reopened from Recent (changes the "can't open" message). */
    var fromRecent by mutableStateOf(false); private set

    /**
     * Loads a photo. With [restore] (reopening from Recent) the saved blur areas,
     * effect and intensity come back instead of running detection again.
     */
    fun open(newUri: Uri, restore: RecentEntry? = null) {
        if (newUri == uri) return
        reset()
        uri = newUri
        fromRecent = restore != null
        loading = true
        loadJob = viewModelScope.launch {
            try {
                val bitmap = PhotoLoader.load(getApplication(), newUri)
                base = bitmap
                if (restore != null) {
                    faces = restore.regions.mapIndexed { i, r -> r.toRegion(i, bitmap.width, bitmap.height) }
                    effect = restore.effect
                    intensity = restore.intensity
                } else {
                    val auto = selectionMode == SelectionMode.Auto
                    faces = FaceFinder.detect(bitmap).map { it.copy(blurred = auto) }
                }
                rerender()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("samar", "Couldn't open photo", e)
                failed = true
            } finally {
                loading = false
            }
        }
    }

    fun close() = reset()

    fun toggleFace(id: Int) {
        remember()
        faces = faces.map { if (it.id == id) it.copy(blurred = !it.blurred) else it }
        rerender()
    }

    fun selectEffect(e: Effect) {
        if (e == effect || !e.available) return
        remember()
        effect = e
        rerender()
    }

    /** Called continuously while dragging; one undo step per drag. */
    fun changeIntensity(value: Float) {
        if (intensityAtDragStart == null) intensityAtDragStart = intensity
        intensity = value.coerceIn(0f, 1f)
        rerender()
    }

    fun finishIntensityChange() {
        val start = intensityAtDragStart ?: return
        intensityAtDragStart = null
        if (start != intensity) {
            history.addLast(EditSnapshot(faces, effect, start))
            exportedSinceEdit = false
            canUndo = true
        }
    }

    fun toggleOriginal() {
        showOriginal = !showOriginal
    }

    fun togglePreview() {
        hideOverlays = !hideOverlays
    }

    /** Switches Auto / Manual: detected faces are all blurred (Auto) or all cleared (Manual). Undoable. */
    fun changeSelectionMode(mode: SelectionMode) {
        if (mode == selectionMode) return
        selectionMode = mode
        selectionPref.save(mode)
        if (faces.none { !it.manual }) return
        remember()
        val auto = mode == SelectionMode.Auto
        faces = faces.map { if (it.manual) it else it.copy(blurred = auto) }
        rerender()
    }

    fun undo() {
        val s = history.removeLastOrNull() ?: return
        faces = s.faces; effect = s.effect; intensity = s.intensity
        canUndo = history.isNotEmpty()
        exportedSinceEdit = false
        rerender()
    }

    private fun remember() {
        exportedSinceEdit = false
        history.addLast(EditSnapshot(faces, effect, intensity))
        canUndo = true
    }

    private fun rerender() {
        val b = base ?: return
        val f = faces; val e = effect; val i = intensity
        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            val result = withContext(Dispatchers.Default) { EffectRenderer.render(b, f, e, i) }
            rendered = result
        }
    }

    private fun reset() {
        loadJob?.cancel(); renderJob?.cancel()
        uri = null; loading = false; failed = false; fromRecent = false
        base = null; rendered = null; faces = emptyList()
        effect = Effect.Gaussian; intensity = 0.7f; showOriginal = false; hideOverlays = false
        viewZoom = 1f; viewCenter = null
        history.clear(); canUndo = false; intensityAtDragStart = null
        closeDraft()
        exportOpen = false; exportStatus = ExportStatus.Idle; exportedSinceEdit = false
    }
}

sealed interface ExportStatus {
    val isWorking: Boolean get() = this == Saving || this == PreparingShare
    data object Idle : ExportStatus
    data object Saving : ExportStatus
    data object PreparingShare : ExportStatus
    data object Saved : ExportStatus
    data object Failed : ExportStatus
}
