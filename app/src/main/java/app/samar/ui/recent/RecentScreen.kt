package app.samar.ui.recent

import app.samar.ui.theme.samarBackdrop
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.samar.ui.components.OutlinedPillButton
import app.samar.ui.components.RecentThumb
import app.samar.ui.home.RecentPhoto
import app.samar.ui.components.GlassIconButton
import app.samar.ui.components.GlassTone
import app.samar.ui.components.liquidGlass
import app.samar.ui.icons.SamarIcons
import app.samar.ui.theme.Samar
import app.samar.ui.theme.SamarShapes

/**
 * All Recent photos. Tap to reopen in the editor. "Edit" shows a remove button on
 * each photo plus "Clear all". Removing never touches the copy in the gallery.
 */
@Composable
fun RecentScreen(
    items: List<RecentPhoto>,
    onBack: () -> Unit,
    onOpen: (RecentPhoto) -> Unit,
    onRemove: (RecentPhoto) -> Unit,
    onClearAll: () -> Unit,
) {
    val c = Samar.colors
    var editMode by rememberSaveable { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val editing = editMode && items.isNotEmpty()

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
            GlassIconButton(SamarIcons.Back, "Back", onClick = onBack)
            Text(
                "Recent",
                style = Samar.type.screenTitle,
                color = c.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            Box(
                Modifier
                    .heightIn(min = 48.dp)
                    .size(width = 64.dp, height = 48.dp)
                    .clip(SamarShapes.pill)
                    .clickable(enabled = items.isNotEmpty(), role = Role.Button) { editMode = !editMode },
                contentAlignment = Alignment.Center,
            ) {
                if (items.isNotEmpty()) {
                    Text(if (editing) "Done" else "Edit", style = Samar.type.buttonSmall, color = c.accent)
                }
            }
        }

        if (items.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Photos you save will show up here.",
                    style = Samar.type.body, color = c.mutedText, textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(items, key = { it.id }) { photo ->
                    RecentTile(photo, editing, onOpen = { onOpen(photo) }, onRemove = { onRemove(photo) })
                }
            }
            if (editing) {
                Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 16.dp)) {
                    Text(
                        "Removing a photo here keeps the saved copy in your gallery.",
                        style = Samar.type.bodySmall, color = c.mutedText,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedPillButton("Clear all", onClick = { confirmClear = true })
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear Recent?", style = Samar.type.sectionHeading) },
            text = {
                Text(
                    "This removes all photos from samar's list. The saved copies stay in your gallery.",
                    style = Samar.type.body,
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; onClearAll() }) {
                    Text("Clear all", style = Samar.type.buttonSmall, color = c.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("Cancel", style = Samar.type.buttonSmall, color = c.text)
                }
            },
            containerColor = if (c.isDark) androidx.compose.ui.graphics.Color(0xFF151A4E) else androidx.compose.ui.graphics.Color.White,
            titleContentColor = c.text,
            textContentColor = c.mutedText,
        )
    }
}

@Composable
private fun RecentTile(photo: RecentPhoto, editing: Boolean, onOpen: () -> Unit, onRemove: () -> Unit) {
    val label = "Saved photo, ${photo.effectName}, ${photo.savedLabel}"
    Box(Modifier.aspectRatio(1f)) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(SamarShapes.thumbnail)
                .clickable(enabled = !editing, role = Role.Button, onClickLabel = "Open in editor", onClick = onOpen)
                .semantics { contentDescription = label },
        ) {
            RecentThumb(photo.thumbnail)
        }
        if (editing) {
            // 28dp visual inside a 48dp target, in the top-right corner.
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(48.dp)
                    .clickable(role = Role.Button, onClick = onRemove)
                    .clearAndSetSemantics { contentDescription = "Remove $label from Recent" },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .liquidGlass(SamarShapes.pill, GlassTone.Frost, elevation = 3.dp)
                        .border(1.5.dp, Color(0xFF15141C), SamarShapes.pill),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(SamarIcons.Close, contentDescription = null, tint = Color(0xFF15141C), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
