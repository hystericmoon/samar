package app.samar

import android.graphics.Color
import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import app.samar.data.ThemeMode
import app.samar.data.RecentEntry
import app.samar.data.RecentStore
import app.samar.data.ThemePreference
import app.samar.editor.EditorViewModel
import app.samar.ui.editor.AddFaceScreen
import app.samar.ui.editor.EditorScreen
import app.samar.ui.export.ExportScreen
import app.samar.ui.home.HomeScreen
import app.samar.ui.home.RecentPhoto
import app.samar.ui.notices.NoticesScreen
import app.samar.update.Updater
import app.samar.ui.recent.RecentScreen
import app.samar.ui.theme.SamarTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themePref = ThemePreference(this)
        Updater.maybeAutoCheck(this) // at most once a day; can be turned off in About

        setContent {
            var themeMode by remember { mutableStateOf(themePref.load()) }
            val isDark = when (themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }

            // Status/navigation bar icons follow the app theme, not just the system one.
            DisposableEffect(isDark) {
                val style = if (isDark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            // The photo being edited; null = Home. Survives rotation.
            var editing by rememberSaveable { mutableStateOf<Uri?>(null) }
            // Set when that photo was reopened from Recent (its edits get restored).
            var editingRecentId by rememberSaveable { mutableStateOf<String?>(null) }
            var showAllRecent by rememberSaveable { mutableStateOf(false) }
            var showNotices by rememberSaveable { mutableStateOf(false) }
            val editor: EditorViewModel = viewModel()
            val recentStore = remember { RecentStore.get(this) }
            val recent = recentStore.entries.map { it.toPhoto() }

            // System photo picker: no storage permission needed.
            val pickPhoto = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
                if (uri != null) {
                    editingRecentId = null
                    editing = uri
                }
            }
            val openRecent: (RecentPhoto) -> Unit = { photo ->
                recentStore.find(photo.id)?.let {
                    editingRecentId = it.id
                    editing = it.source
                }
            }

            SamarTheme(darkTheme = isDark) {
                val current = editing
                if (current != null) {
                    LaunchedEffect(current) { editor.open(current, recentStore.find(editingRecentId)) }
                    val closeEditor = {
                        editing = null
                        editingRecentId = null
                        editor.close()
                    }
                    if (editor.exportOpen) {
                        BackHandler(onBack = editor::closeExport)
                        ExportScreen(vm = editor, onDone = {
                            showAllRecent = false // Done always lands on Home
                            closeEditor()
                        })
                    } else if (editor.draft != null) {
                        BackHandler(onBack = editor::cancelDraft)
                        AddFaceScreen(vm = editor)
                    } else {
                        BackHandler(onBack = closeEditor)
                        EditorScreen(vm = editor, onBack = closeEditor)
                    }
                } else if (showNotices) {
                    NoticesScreen(onBack = { showNotices = false })
                } else if (showAllRecent) {
                    BackHandler { showAllRecent = false }
                    RecentScreen(
                        items = recent,
                        onBack = { showAllRecent = false },
                        onOpen = openRecent,
                        onRemove = { recentStore.remove(it.id) },
                        onClearAll = { recentStore.clear() },
                    )
                } else HomeScreen(
                    isDark = isDark,
                    onThemeChange = { dark ->
                        themeMode = if (dark) ThemeMode.Dark else ThemeMode.Light
                        themePref.save(themeMode)
                    },
                    selectionMode = editor.selectionMode,
                    onSelectionModeChange = editor::changeSelectionMode,
                    recent = recent,
                    onChoosePhoto = {
                        pickPhoto.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                    },
                    onOpenRecent = openRecent,
                    onSeeAll = { showAllRecent = true },
                    onOpenAbout = { showNotices = true },
                )
            }
        }
    }
}

private val savedDate = SimpleDateFormat("d MMM", Locale.getDefault())

private fun RecentEntry.toPhoto() = RecentPhoto(
    id = id,
    effectName = effect.label,
    savedLabel = "saved ${savedDate.format(Date(savedAt))}",
    thumbnail = thumbnail,
    facesHidden = regions.count { it.blurred },
)
