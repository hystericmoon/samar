package app.samar.ui.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.samar.editor.EditorViewModel
import app.samar.effects.Effect
import app.samar.editor.FaceRegion
import app.samar.ui.components.EffectRow
import app.samar.ui.components.GlassIconButton
import app.samar.ui.components.GlassTone
import app.samar.ui.components.liquidGlass
import app.samar.ui.components.FaceToggle
import app.samar.ui.components.IntensitySlider
import app.samar.ui.components.PillButton
import app.samar.ui.icons.SamarIcons
import app.samar.ui.theme.Samar
import app.samar.ui.theme.SamarShapes

private val SheetOverlap = 28.dp

@Composable
fun EditorScreen(vm: EditorViewModel, onBack: () -> Unit) {
    val c = Samar.colors
    val density = LocalDensity.current
    var sheetHeight by remember { mutableStateOf(0.dp) }

    Box(Modifier.fillMaxSize().background(c.background)) {
        Column(Modifier.fillMaxSize()) {
            EditorTopBar(canUndo = vm.canUndo, onBack = onBack, onUndo = vm::undo)

            // Photo area runs under the sheet's rounded top corners.
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = (sheetHeight - SheetOverlap).coerceAtLeast(0.dp)),
            ) {
                val base = vm.base
                when {
                    vm.failed -> Text(
                        if (vm.fromRecent) "The original photo isn't available any more. " +
                            "It may have been deleted or moved."
                        else "Couldn't open this photo.",
                        style = Samar.type.body, color = c.mutedText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                    )
                    base == null -> CircularProgressIndicator(
                        color = c.accent, modifier = Modifier.align(Alignment.Center),
                    )
                    else -> {
                        PhotoWithFaces(
                            image = if (vm.showOriginal) base else (vm.rendered ?: base),
                            faces = vm.faces,
                            showOverlays = !vm.showOriginal,
                            onToggle = vm::toggleFace,
                            zoom = vm.viewZoom,
                            center = vm.viewCenter,
                            onViewportChange = vm::setViewport,
                            modifier = Modifier.fillMaxSize(),
                        )
                        ShowOriginalButton(
                            showing = vm.showOriginal,
                            onToggle = vm::toggleOriginal,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = SheetOverlap + 16.dp),
                        )
                    }
                }
            }
        }

        EditorSheet(
            vm = vm,
            onAddMissedFace = vm::startDraft,
            onExport = vm::openExport,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { sheetHeight = with(density) { it.height.toDp() } },
        )
    }
}

@Composable
private fun EditorTopBar(canUndo: Boolean, onBack: () -> Unit, onUndo: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIconButton(SamarIcons.Back, "Back", onClick = onBack)
        Text(
            "Edit photo",
            style = Samar.type.screenTitle,
            color = Samar.colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        GlassIconButton(SamarIcons.Undo, "Undo", enabled = canUndo, onClick = onUndo)
    }
}

/**
 * The photo, fitted to the area, with a box and a FaceToggle on every face.
 * Pinch or double-tap to zoom (up to 3x); drag to pan when zoomed.
 * Boxes follow the zoom; toggles stay full size so they remain 48dp targets.
 * Zoom lives in the ViewModel: it resets for a new photo, not when edits change.
 */
@Composable
private fun PhotoWithFaces(
    image: Bitmap,
    faces: List<FaceRegion>,
    showOverlays: Boolean,
    onToggle: (Int) -> Unit,
    zoom: Float,
    center: Offset?,
    onViewportChange: (Float, Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.clipToBounds()) {
        val density = LocalDensity.current
        val t = ViewTransform(
            image.width, image.height,
            constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat(),
            zoom, center,
        )
        val latest by rememberUpdatedState(t)
        val onChange by rememberUpdatedState(onViewportChange)

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
                        val target = if (latest.zoom > 1.01f) 1f else DOUBLE_TAP_ZOOM
                        val (z, c) = latest.zoomedAround(p, target)
                        onChange(z, c)
                    })
                },
        ) {
            TransformedPhoto(image.asImageBitmap(), t, "Photo being edited")
            if (showOverlays) {
                FaceBoxes(faces, t::toView, t.k, Modifier.fillMaxSize())
                val toggleHalf = with(density) { 24.dp.toPx() }
                val toggleLift = with(density) { 52.dp.toPx() }
                faces.forEachIndexed { index, face ->
                    val tl = t.toView(face.bounds.left, face.bounds.top)
                    val fw = face.bounds.width() * t.k
                    val fh = face.bounds.height() * t.k
                    // Tap anywhere on the face box too (TalkBack uses the toggle instead).
                    Box(
                        Modifier
                            .offset { IntOffset(tl.x.roundToInt(), tl.y.roundToInt()) }
                            .size(with(density) { fw.toDp() }, with(density) { fh.toDp() })
                            .clearAndSetSemantics { }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onToggle(face.id) },
                    )
                    // Above the box; pinned to the top edge while the face is still on screen.
                    var toggleY = tl.y - toggleLift
                    if (tl.y + fh > 0f) toggleY = toggleY.coerceAtLeast(0f)
                    FaceToggle(
                        faceNumber = index + 1,
                        blurred = face.blurred,
                        onToggle = { onToggle(face.id) },
                        modifier = Modifier.offset {
                            IntOffset((tl.x + fw / 2f - toggleHalf).roundToInt(), toggleY.roundToInt())
                        },
                    )
                }
            }
        }
    }
}

/** Solid outline = blurred, dashed = not blurred (state is never colour alone). */
@Composable
private fun FaceBoxes(
    faces: List<FaceRegion>,
    toView: (Float, Float) -> Offset,
    scale: Float,
    modifier: Modifier,
) {
    Canvas(modifier) {
        val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))
        faces.forEach { f ->
            val topLeft = toView(f.bounds.left, f.bounds.top)
            val box = Size(f.bounds.width() * scale, f.bounds.height() * scale)
            val effect = if (f.blurred) null else dash
            // 14dp on big faces, but never more than a quarter of the box (small faces stay boxes).
            val corner = CornerRadius(minOf(14.dp.toPx(), minOf(box.width, box.height) / 4f))
            // Dark halo keeps the white line visible on light photos (3:1 contrast).
            drawRoundRect(
                Color.Black.copy(alpha = 0.35f), topLeft, box, corner,
                style = Stroke(4.dp.toPx(), pathEffect = effect),
            )
            drawRoundRect(Color.White, topLeft, box, corner, style = Stroke(2.dp.toPx(), pathEffect = effect))
        }
    }
}

@Composable
private fun ShowOriginalButton(showing: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(48.dp)
            // Smoky glass; the light ring keeps it visible on dark backgrounds and dark photos.
            .liquidGlass(SamarShapes.pill, GlassTone.Dark, interaction, elevation = 4.dp)
            .border(1.dp, Color.White.copy(alpha = 0.4f), SamarShapes.pill)
            .toggleable(
                value = showing, interactionSource = interaction, indication = null,
                role = Role.Switch, onValueChange = { onToggle() },
            )
            .semantics { contentDescription = "Show original photo" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (showing) SamarIcons.EyeOff else SamarIcons.Eye,
            contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun EditorSheet(
    vm: EditorViewModel,
    onAddMissedFace: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Samar.colors
    val total = vm.faces.size
    val blurred = vm.blurredCount
    // Strong line: how many faces. Quiet line under it: what is hidden.
    val faces = if (total == 1) "1 face" else "$total faces"
    val visible = total - blurred
    val (shown, detail, spoken) = when {
        vm.loading -> Triple("Finding faces…", "", "Finding faces")
        vm.failed -> Triple("", "", "")
        total == 0 -> Triple("No faces found", "Use Add missed face to blur one.", "No faces found. Use Add missed face to blur one.")
        visible == 0 -> Triple(faces, "All blurred", "$faces found, $blurred blurred")
        blurred == 0 -> Triple(faces, "None blurred", "$faces found, $blurred blurred")
        else -> Triple(faces, "$blurred blurred · $visible visible", "$faces found, $blurred blurred")
    }

    Column(
        modifier
            .fillMaxWidth()
            .clip(SamarShapes.sheet)
            .background(c.surface)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(top = 20.dp, bottom = 24.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .clearAndSetSemantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = spoken
                    },
            ) {
                Text(shown, style = Samar.type.buttonSmall, color = c.text)
                if (detail.isNotEmpty()) {
                    Text(detail, style = Samar.type.label, color = c.mutedText)
                }
            }
            // Secondary action: a small glass chip, quieter than the count, 48dp touch target.
            val chipInteraction = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .heightIn(min = 48.dp)
                    .clickable(chipInteraction, null, role = Role.Button, onClick = onAddMissedFace),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    Modifier
                        .height(36.dp)
                        .liquidGlass(SamarShapes.pill, GlassTone.Clear, chipInteraction, elevation = 2.dp, outline = true)
                        .padding(start = 10.dp, end = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(SamarIcons.Plus, contentDescription = null, tint = c.accent, modifier = Modifier.size(18.dp))
                    Text("Add missed face", style = Samar.type.label, color = c.accent)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        EffectRow(selected = vm.effect, onSelect = vm::selectEffect, modifier = Modifier.fillMaxWidth())
        if (vm.effect == Effect.Smear) {
            Text(
                "Smear is a stylistic effect. Solid or Pixelate hide identity more reliably.",
                style = Samar.type.bodySmall,
                color = c.mutedText,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(top = 4.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Spacer(Modifier.height(16.dp))
        IntensitySlider(
            value = vm.intensity,
            onChange = vm::changeIntensity,
            onChangeFinished = vm::finishIntensityChange,
            enabled = vm.effect.usesIntensity,
            disabledNote = "Always full",
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(12.dp))
        PillButton(text = "Export", onClick = onExport, modifier = Modifier.padding(horizontal = 24.dp))
    }
}
