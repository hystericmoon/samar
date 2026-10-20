package app.samar.ui.export

import android.Manifest
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
            .background(c.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(SamarIcons.Back, "Back", onClick = vm::closeExport)
            Spacer(Modifier.size(width = 16.dp, height = 48.dp))
            Text(
                "Export",
                style = Samar.type.screenTitle,
                color = c.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            // Same width as the back button so the title stays centred.
            Box(
                Modifier
                    .size(width = 64.dp, height = 48.dp)
                    .clip(SamarShapes.pill)
                    .clickable(role = Role.Button, onClickLabel = "Go to Home") { done() },
                contentAlignment = Alignment.Center,
            ) {
                Text("Done", style = Samar.type.buttonSmall, color = c.accent)
            }
        }

        Column(Modifier.weight(1f).padding(horizontal = 24.dp)) {
            val before = vm.base
            val after = vm.rendered ?: before
            val shown = if (compare == Compare.Before) before else after
            Box(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                if (shown != null) {
                    Image(
                        bitmap = shown.asImageBitmap(),
                        contentDescription = if (compare == Compare.Before) "Original photo" else "Photo with faces hidden",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .aspectRatio(shown.width / shown.height.toFloat())
                            .clip(RoundedCornerShape(24.dp)),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            SegmentedControl(
                options = Compare.entries,
                selected = compare,
                label = { it.name },
                onSelect = { compare = it },
                trackColor = c.surface,
            )
            Spacer(Modifier.height(8.dp))
            SettingSwitch(
                title = "Remove location and camera info",
                description = "GPS and EXIF data are stripped from the saved copy.",
                checked = options.removeMetadata,
                onChange = { vm.exportOptions = options.copy(removeMetadata = it) },
            )
            HorizontalDivider(color = c.divider)
            SettingSwitch(
                title = "Keep original resolution",
                description = "Turn off to save a smaller file.",
                checked = options.keepOriginalResolution,
                onChange = { vm.exportOptions = options.copy(keepOriginalResolution = it) },
            )
        }

        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 16.dp)) {
            PillButton(
                text = when (status) {
                    ExportStatus.Saving -> "Saving…"
                    ExportStatus.Saved -> "Saved"
                    else -> "Save to gallery"
                },
                icon = if (status == ExportStatus.Saved) SamarIcons.Check else SamarIcons.Download,
                onClick = { if (!status.isWorking) save() },
            )
            Spacer(Modifier.height(12.dp))
            OutlinedPillButton(
                text = if (status == ExportStatus.PreparingShare) "Preparing…" else "Share",
                icon = SamarIcons.Share,
                onClick = { if (!status.isWorking) share() },
            )
            val message = when (status) {
                ExportStatus.Saved -> "Saved to Pictures/${Exporter.ALBUM}."
                ExportStatus.Failed -> "Couldn't export this photo. Try again."
                else -> ""
            }
            Text(
                message,
                style = Samar.type.bodySmall,
                color = c.mutedText,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
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
            containerColor = c.surface,
            titleContentColor = c.text,
            textContentColor = c.mutedText,
        )
    }
}
