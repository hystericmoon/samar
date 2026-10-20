package app.samar.ui.editor

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.samar.editor.EditorViewModel
import app.samar.editor.FaceRegion
import app.samar.editor.RegionShape
import app.samar.ui.components.OutlinedPillButton
import app.samar.ui.components.PillButton
import app.samar.ui.components.SegmentedControl
import app.samar.ui.components.GlassIconButton
import app.samar.ui.icons.SamarIcons
import app.samar.ui.theme.Samar
import app.samar.ui.theme.SamarShapes
import kotlin.math.roundToInt

private val SheetOverlap = 28.dp
private const val MIN_SIZE = 16f

private enum class Edge { Left, Right, Top, Bottom }

/** What a gesture is doing, decided by where the first finger lands. */
private sealed interface Drag {
    data object Shape : Drag          // move; pinch resizes the shape
    data class Handle(val edge: Edge) : Drag
    data object Photo : Drag          // pan; pinch zooms the photo
}

@Composable
fun AddFaceScreen(vm: EditorViewModel) {
    val c = Samar.colors
    val density = LocalDensity.current
    val draft = vm.draft ?: return
    var sheetHeight by remember { mutableStateOf(0.dp) }

    Box(Modifier.fillMaxSize().background(c.background)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(SamarIcons.Close, "Close", onClick = vm::cancelDraft)
                Text(
                    "Add missed face",
                    style = Samar.type.screenTitle,
                    color = c.text,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                Spacer(Modifier.size(48.dp))
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = (sheetHeight - SheetOverlap).coerceAtLeast(0.dp)),
            ) {
                PlacementArea(vm, draft, Modifier.fillMaxSize())
                Text(
                    "Drag to move, pinch to resize",
                    style = Samar.type.bodyStrong,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = SheetOverlap + 16.dp)
                        .clip(SamarShapes.pill)
                        .background(Color(0xE015141C))
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { sheetHeight = with(density) { it.height.toDp() } }
                .fillMaxWidth()
                .clip(SamarShapes.sheet)
                .background(c.surface)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 24.dp),
        ) {
            Text(
                "Blur area shape",
                style = Samar.type.sectionHeading,
                color = c.text,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(16.dp))
            SegmentedControl(
                options = RegionShape.entries,
                selected = draft.shape,
                label = { it.name },
                onSelect = vm::setDraftShape,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Use this for faces the detector missed, like profile views, masks, or faces " +
                    "that are partly covered. It uses the same effect and intensity as the rest of the photo.",
                style = Samar.type.body,
                color = c.mutedText,
            )
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedPillButton("Cancel", onClick = vm::cancelDraft, modifier = Modifier.weight(1f))
                PillButton("Add blur", onClick = vm::commitDraft, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** The photo with the draggable, resizable blur area on top. */
@Composable
private fun PlacementArea(vm: EditorViewModel, draft: FaceRegion, modifier: Modifier) {
    val base = vm.base ?: return
    val image = vm.rendered ?: base
    val accent = Samar.colors.accent
    val density = LocalDensity.current

    BoxWithConstraints(modifier.clipToBounds()) {
        val t = ViewTransform(
            base.width, base.height,
            constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat(),
            vm.viewZoom, vm.viewCenter,
        )
        val latestT by rememberUpdatedState(t)
        val latestDraft by rememberUpdatedState(draft)
        val handleReach = with(density) { 24.dp.toPx() } // 48dp touch target
        val bodySlop = with(density) { 12.dp.toPx() }

        fun viewRect(r: RectF, tr: ViewTransform): Rect {
            val tl = tr.toView(r.left, r.top)
            return Rect(tl, Size(r.width() * tr.k, r.height() * tr.k))
        }

        fun hitTest(p: Offset): Drag {
            val r = viewRect(latestDraft.bounds, latestT)
            val handles = mapOf(
                Edge.Left to Offset(r.left, r.center.y), Edge.Right to Offset(r.right, r.center.y),
                Edge.Top to Offset(r.center.x, r.top), Edge.Bottom to Offset(r.center.x, r.bottom),
            )
            // On small shapes the 48dp handle targets reach the middle: a handle only wins
            // when the finger is nearer to it than to the shape's centre.
            handles.entries.minByOrNull { (it.value - p).getDistance() }?.let {
                val toHandle = (it.value - p).getDistance()
                if (toHandle <= handleReach && toHandle < (r.center - p).getDistance()) {
                    return Drag.Handle(it.key)
                }
            }
            return if (r.inflate(bodySlop).contains(p)) Drag.Shape else Drag.Photo
        }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val mode = hitTest(down.position)
                        do {
                            val event = awaitPointerEvent()
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            val tr = latestT
                            val b = latestDraft.bounds
                            val dx = pan.x / tr.k
                            val dy = pan.y / tr.k
                            when (mode) {
                                Drag.Shape -> {
                                    // One finger moves; a pinch also scales around the shape's centre.
                                    val w = b.width() * zoom
                                    val h = b.height() * zoom
                                    val cx = b.centerX() + dx
                                    val cy = b.centerY() + dy
                                    vm.setDraftBounds(RectF(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2))
                                }
                                is Drag.Handle -> vm.setDraftBounds(
                                    dragEdge(b, mode.edge, dx, dy, base.width.toFloat(), base.height.toFloat()),
                                )
                                Drag.Photo -> if (centroid != Offset.Unspecified) {
                                    val (z, center) = tr.zoomedAround(centroid, tr.zoom * zoom, pan)
                                    vm.setViewport(z, center)
                                }
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        } while (event.changes.any { it.pressed })
                    }
                },
        ) {
            TransformedPhoto(image.asImageBitmap(), t, "Photo")
            Canvas(Modifier.fillMaxSize()) {
                vm.draftPatch?.let { (patch, at) ->
                    val tl = t.toView(at.x.toFloat(), at.y.toFloat())
                    drawImage(
                        patch.asImageBitmap(),
                        dstOffset = IntOffset(tl.x.roundToInt(), tl.y.roundToInt()),
                        dstSize = IntSize(
                            (patch.width * t.k).roundToInt().coerceAtLeast(1),
                            (patch.height * t.k).roundToInt().coerceAtLeast(1),
                        ),
                    )
                }
                drawShapeOutline(viewRect(draft.bounds, t), draft.shape, accent)
            }

            // TalkBack: the shape as one element with move and resize actions.
            val r = viewRect(draft.bounds, t)
            val step = minOf(base.width, base.height) * 0.04f
            Box(
                Modifier
                    .offset { IntOffset(r.left.roundToInt(), r.top.roundToInt()) }
                    .size(with(density) { r.width.toDp() }, with(density) { r.height.toDp() })
                    .semantics {
                        contentDescription = "Blur area, ${draft.shape.name.lowercase()}"
                        customActions = blurAreaActions(vm, step)
                    },
            )
        }
    }
}

private fun dragEdge(b: RectF, edge: Edge, dx: Float, dy: Float, maxW: Float, maxH: Float): RectF =
    when (edge) {
        Edge.Left -> RectF((b.left + dx).coerceIn(0f, b.right - MIN_SIZE), b.top, b.right, b.bottom)
        Edge.Right -> RectF(b.left, b.top, (b.right + dx).coerceIn(b.left + MIN_SIZE, maxW), b.bottom)
        Edge.Top -> RectF(b.left, (b.top + dy).coerceIn(0f, b.bottom - MIN_SIZE), b.right, b.bottom)
        Edge.Bottom -> RectF(b.left, b.top, b.right, (b.bottom + dy).coerceIn(b.top + MIN_SIZE, maxH))
    }

private fun blurAreaActions(vm: EditorViewModel, step: Float): List<CustomAccessibilityAction> {
    fun act(label: String, change: (RectF) -> RectF) = CustomAccessibilityAction(label) {
        vm.draft?.let { vm.setDraftBounds(change(RectF(it.bounds))) }
        true
    }
    return listOf(
        act("Move up") { it.apply { offset(0f, -step) } },
        act("Move down") { it.apply { offset(0f, step) } },
        act("Move left") { it.apply { offset(-step, 0f) } },
        act("Move right") { it.apply { offset(step, 0f) } },
        act("Make larger") { it.apply { inset(-step / 2, -step / 2) } },
        act("Make smaller") { it.apply { inset(step / 2, step / 2) } },
    )
}

/** White halo + accent outline, plus four round handles (small visual, 48dp touch area). */
private fun DrawScope.drawShapeOutline(r: Rect, shape: RegionShape, accent: Color) {
    val halo = Stroke(5.dp.toPx())
    val line = Stroke(2.5.dp.toPx())
    when (shape) {
        RegionShape.Ellipse -> {
            drawOval(Color.White, r.topLeft, r.size, style = halo)
            drawOval(accent, r.topLeft, r.size, style = line)
        }
        RegionShape.Rectangle -> {
            drawRect(Color.White, r.topLeft, r.size, style = halo)
            drawRect(accent, r.topLeft, r.size, style = line)
        }
    }
    val handleR = 8.dp.toPx()
    listOf(
        Offset(r.left, r.center.y), Offset(r.right, r.center.y),
        Offset(r.center.x, r.top), Offset(r.center.x, r.bottom),
    ).forEach { p ->
        drawCircle(Color.White, handleR, p)
        drawCircle(accent, handleR, p, style = Stroke(2.5.dp.toPx()))
    }
}
