package app.samar.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.samar.update.UpdatePreference
import app.samar.update.UpdateStatus
import app.samar.update.Updater
import app.samar.ui.theme.Samar
import kotlin.math.roundToInt

/** One line describing [status] for people, or null when there's nothing to say. */
private fun UpdateStatus.message(): String? = when (this) {
    UpdateStatus.Idle -> null
    UpdateStatus.Checking -> "Checking for updates…"
    UpdateStatus.UpToDate -> "You have the latest version."
    is UpdateStatus.Available -> "samar ${release.version} is available."
    is UpdateStatus.Downloading ->
        "Downloading samar ${release.version}" + (progress?.let { " · ${(it * 100).roundToInt()}%" } ?: "…")
    is UpdateStatus.Failed -> message
}

/**
 * Home banner shown only when a newer samar is found (or while it downloads).
 * A glass card under the trust line: what's new and an Update button.
 */
@Composable
fun UpdateBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val status = Updater.status
    val release = when (status) {
        is UpdateStatus.Available -> status.release
        is UpdateStatus.Downloading -> status.release
        is UpdateStatus.Failed -> status.release
        else -> null
    } ?: return
    val c = Samar.colors
    Row(
        modifier
            .fillMaxWidth()
            .liquidGlass(RoundedCornerShape(20.dp), GlassTone.Clear, outline = true)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text("Update available", style = Samar.type.label, color = c.mutedText)
            Text(status.message() ?: "", style = Samar.type.bodyStrong, color = c.text)
        }
        if (status !is UpdateStatus.Downloading) {
            PillButton(
                text = if (status is UpdateStatus.Failed) "Retry" else "Update",
                onClick = { Updater.downloadAndInstall(context, release) },
                modifier = Modifier.fillMaxWidth(0.42f),
            )
        }
    }
}

/** About screen section: version, automatic checks switch, Check now, and the latest status. */
@Composable
fun UpdateSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { UpdatePreference(context) }
    var auto by remember { mutableStateOf(prefs.autoCheck) }
    val status = Updater.status
    val c = Samar.colors
    Column(modifier) {
        Text("Version ${remember { Updater.currentVersion(context) }}", style = Samar.type.bodyStrong, color = c.text)
        Spacer(Modifier.height(8.dp))
        SettingSwitch(
            title = "Check for updates automatically",
            description = "Once a day, samar asks GitHub if there's a newer version. Nothing else is sent.",
            checked = auto,
            onChange = { auto = it; prefs.autoCheck = it },
        )
        Spacer(Modifier.height(12.dp))
        val release = (status as? UpdateStatus.Available)?.release ?: (status as? UpdateStatus.Failed)?.release
        if (release != null) {
            PillButton(
                text = "Update to ${release.version}",
                onClick = { Updater.downloadAndInstall(context, release) },
            )
        } else {
            OutlinedPillButton(
                text = "Check for updates",
                onClick = { Updater.check(context) },
            )
        }
        status.message()?.let {
            Text(
                it,
                style = Samar.type.bodySmall,
                color = c.mutedText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}
