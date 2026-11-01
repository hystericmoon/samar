package app.samar.ui.export

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.border
import app.samar.ui.components.glassHairline
import app.samar.ui.components.glassCard
import app.samar.ui.theme.samarBackdrop
import android.graphics.Bitmap
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import app.samar.ui.components.GlassTone
import app.samar.ui.components.liquidGlass
import app.samar.ui.editor.DOUBLE_TAP_ZOOM
import app.samar.ui.editor.MAX_ZOOM
import app.samar.ui.editor.TransformedPhoto
import app.samar.ui.editor.ViewTransform
import android.Manifest
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.samar.editor.EditorViewModel
import app.samar.editor.ExportStatus
import app.samar.export.Exporter
import app.samar.ui.components.OutlinedPillButton
import app.samar.ui.components.PillButton
import app.samar.ui.components.SegmentedControl
import app.samar.ui.components.SettingSwitch
import app.samar.ui.components.GlassIconButton
import app.samar.ui.icons.SamarIcons
import app.samar.ui.theme.Samar
import app.samar.ui.theme.SamarShapes

private enum class Compare { Before, After }

/** A hidden face narrower than this on screen gets the zoom hint. */
private val SMALL_FACE = 56.dp

/**
 * @param onDone back to Home. Asks first if the current edits were never saved or shared.
 */
@Composable
fun ExportScreen(vm: EditorViewModel, onDone: () -> Unit) {
    val c = Samar.colors
    val context = LocalContext.current
    var compare by rememberSaveable { mutableStateOf(Compare.After) }
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    fun done() = if (vm.exportedSinceEdit) onDone() else confirmLeave = true
    val options = vm.exportOptions
    val status = vm.exportStatus

    // Android 8–9 only: ask for storage access the first time someone saves.
    val askStorage = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.saveToGallery()
    }
    fun save() {
        val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) askStorage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else vm.saveToGallery()
    }
    fun share() = vm.share { uri ->
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, null))
    }

    Column(
        Modifier
            .fillMaxSize()
            .samarBackdrop()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(SamarIcons.Back, "Back", onClick = vm::closeExport)
            Text(
                "Export",
                style = Samar.type.screenTitle,
                color = c.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            // Done in a glass pill (48dp tall target).
            Box(
                Modifier
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClickLabel = "Go to Home") { done() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Done",
                    style = Samar.type.buttonSmall,
                    color = c.accent,
                    modifier = Modifier.glassCard(SamarShapes.pill).padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }

        Column(Modifier.weight(1f).padding(horizontal = 20.dp)) {
            SegmentedControl(
                options = Compare.entries,
                selected = compare,
                label = { it.name },
                onSelect = { compare = it },
                height = 48.dp,
            )
            val before = vm.base
            val after = vm.rendered ?: before
            val shown = if (compare == Compare.Before) before else after
            // Zoom is shared by Before and After, so switching compares the same spot.
            var zoom by remember { mutableFloatStateOf(1f) }
            var center by remember { mutableStateOf<Offset?>(null) }
            // Once someone has zoomed here, they know how; the hint stays away for this visit.
            var hasZoomed by remember { mutableStateOf(false) }
            Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                if (shown != null) {
                    ZoomablePreview(
                        image = shown,
                        description = if (compare == Compare.Before) "Original photo" else "Photo with faces hidden",
                        zoom = zoom,
                        center = center,
                        smallestFaceWidth = vm.faces.filter { it.blurred }.minOfOrNull { it.bounds.width() },
                        showHint = !hasZoomed,
                        onViewportChange = { z, c ->
                            zoom = z; center = c
                            if (z > 1.01f) hasZoomed = true
                        },
                    )
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .glassCard(RoundedCornerShape(22.dp))
                    .padding(horizontal = 16.dp, vertical = 2.dp),
            ) {
                SettingSwitch(
                    title = "Remove location and camera info",
                    description = "GPS and EXIF data are stripped from the saved copy.",
                    checked = options.removeMetadata,
                    onChange = { vm.exportOptions = options.copy(removeMetadata = it) },
                )
                HorizontalDivider(color = glassHairline())
                SettingSwitch(
                    title = "Keep original resolution",
                    description = "Turn off to save a smaller file.",
                    checked = options.keepOriginalResolution,
                    onChange = { vm.exportOptions = options.copy(keepOriginalResolution = it) },
                )
            }
        }

        // Bottom dock: what will be saved, then Save and Share.
        val hidden = vm.blurredCount
        Column(
            Modifier
                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
                .fillMaxWidth()
                .glassCard(RoundedCornerShape(26.dp), elevation = 6.dp)
                .padding(start = 18.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
                    Text(
                        if (hidden == 1) "1 face hidden" else "$hidden faces hidden",
                        style = Samar.type.screenTitle.copy(fontSize = 21.sp),
                        color = c.emphasis,
                    )
                    val detail = when (status) {
                        ExportStatus.Saved -> "Saved to Pictures/${Exporter.ALBUM}."
                        ExportStatus.Failed -> "Couldn't export this photo. Try again."
                        else -> "${vm.effect.label} · JPEG" + if (options.removeMetadata) " · no location" else ""
                    }
                    Text(detail, style = Samar.type.label, color = c.mutedText)
                }
                PillButton(
                    text = when (status) {
                        ExportStatus.Saving -> "Saving…"
                        ExportStatus.Saved -> "Saved"
                        else -> "Save"
                    },
                    icon = if (status == ExportStatus.Saved) SamarIcons.Check else SamarIcons.Download,
                    onClick = { if (!status.isWorking) save() },
                    modifier = Modifier.width(150.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedPillButton(
                text = if (status == ExportStatus.PreparingShare) "Preparing…" else "Share",
                icon = SamarIcons.Share,
                onClick = { if (!status.isWorking) share() },
            )
        }
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Leave without saving?", style = Samar.type.sectionHeading) },
            text = { Text("Your edits to this photo will be lost.", style = Samar.type.body) },
            confirmButton = {
                TextButton(onClick = { confirmLeave = false; onDone() }) {
                    Text("Leave", style = Samar.type.buttonSmall, color = c.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false }) {
                    Text("Keep editing", style = Samar.type.buttonSmall, color = c.text)
                }
            },
            containerColor = if (c.isDark) Color(0xFF151A4E) else Color.White,
            titleContentColor = c.text,
            textContentColor = c.mutedText,
        )
    }
}

/**
 * The export preview: the photo in a rounded frame that fits it exactly.
 * Pinch or double-tap to zoom (up to 3x) and drag to pan, as in the editor.
 * When zoomed, a smoky glass button in the corner zooms back out to fit.
 * TalkBack: Zoom in / Zoom out actions on the photo.
 *
 * When a hidden face shows smaller than [SMALL_FACE] on screen, a hint chip points to zoom,
 * since the blur on a face that small is hard to judge at a glance.
 * [smallestFaceWidth] is in photo pixels; null when nothing is hidden.
 */
@Composable
private fun ZoomablePreview(
    image: Bitmap,
    description: String,
    zoom: Float,
    center: Offset?,
    smallestFaceWidth: Float?,
    showHint: Boolean,
    onViewportChange: (Float, Offset) -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val zoomed = zoom > 1.01f
    Box(
        Modifier
            .aspectRatio(image.width / image.height.toFloat())
            .clip(shape)
            .border(1.dp, glassHairline(), shape),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
            val viewW = constraints.maxWidth.toFloat()
            val viewH = constraints.maxHeight.toFloat()
            val t = ViewTransform(image.width, image.height, viewW, viewH, zoom, center)
            val latest by rememberUpdatedState(t)
            val onChange by rememberUpdatedState(onViewportChange)
            val middle = Offset(viewW / 2f, viewH / 2f)
            fun zoomTo(target: Float, focus: Offset = middle) {
                val (z, c) = latest.zoomedAround(focus, target)
                onChange(z, c)
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoomChange, _ ->
                            val (z, c) = latest.zoomedAround(centroid, latest.zoom * zoomChange, pan)
                            onChange(z, c)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { p ->
                            zoomTo(if (latest.zoom > 1.01f) 1f else DOUBLE_TAP_ZOOM, p)
                        })
                    }
                    .semantics {
                        customActions = buildList {
                            if (zoom < MAX_ZOOM - 0.01f) {
                                add(CustomAccessibilityAction("Zoom in") { zoomTo(latest.zoom * 1.5f); true })
                            }
                            if (zoomed) {
                                add(CustomAccessibilityAction("Zoom out to fit") { zoomTo(1f); true })
                            }
                        }
                    },
            ) {
                TransformedPhoto(image.asImageBitmap(), t, description)
            }

            val smallFaces = smallestFaceWidth != null &&
                smallestFaceWidth * t.fit < with(LocalDensity.current) { SMALL_FACE.toPx() }
            if (showHint && !zoomed && smallFaces) {
                Text(
                    "Pinch to check small faces",
                    style = Samar.type.label,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        // Same smoky glass and light ring as the photo buttons: readable on any photo.
                        .liquidGlass(SamarShapes.pill, GlassTone.Dark, elevation = 4.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.4f), SamarShapes.pill)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .semantics { contentDescription = "Some faces are small. Use the Zoom in action to check them." },
                )
            }

            if (zoomed) {
                val interaction = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .size(48.dp)
                        // Smoky glass with a light ring, like the editor's photo buttons: 3:1 on any photo.
                        .liquidGlass(SamarShapes.pill, GlassTone.Dark, interaction, elevation = 4.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.4f), SamarShapes.pill)
                        .clickable(interaction, null, role = Role.Button) { zoomTo(1f) }
                        .semantics { contentDescription = "Zoom out to fit" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(SamarIcons.ZoomOut, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
