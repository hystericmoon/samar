package app.samar.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.samar.ui.components.HeroAnimation
import app.samar.ui.components.PillButton
import app.samar.ui.components.RecentGrid
import app.samar.ui.components.RecentThumb
import app.samar.ui.components.SamarWordmark
import app.samar.ui.components.ThemeSwitch
import app.samar.ui.icons.SamarIcons
import app.samar.ui.theme.Samar
import app.samar.ui.theme.SamarShapes
import app.samar.ui.theme.SamarTheme
import java.io.File

/** One saved photo in the Recent grid. */
data class RecentPhoto(
    val id: String,
    val effectName: String,
    val savedLabel: String = "",
    val thumbnail: File? = null,
)

/** Home shows the latest six (two rows); "See all" shows the rest. */
const val HOME_RECENT_COUNT = 6

@Composable
fun HomeScreen(
    isDark: Boolean,
    onThemeChange: (Boolean) -> Unit,
    recent: List<RecentPhoto>,
    onChoosePhoto: () -> Unit,
    onOpenRecent: (RecentPhoto) -> Unit,
    onSeeAll: () -> Unit,
    onOpenNotices: () -> Unit,
    modifier: Modifier = Modifier,
    recentTile: @Composable (RecentPhoto) -> Unit = { RecentThumb(it.thumbnail) },
) {
    val c = Samar.colors
    Column(
        modifier
            .fillMaxSize()
            .background(c.background)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        // Top bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SamarWordmark(Modifier.weight(1f))
            ThemeSwitch(isDark = isDark, onChange = onThemeChange)
        }

        // Scrolling content
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            HeroAnimation()
            Spacer(Modifier.height(24.dp))
            Text(
                "Blur faces before you share.",
                style = Samar.type.hero,
                color = c.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Everything stays on your phone.",
                style = Samar.type.body,
                color = c.mutedText,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            RecentGrid(
                items = recent.take(HOME_RECENT_COUNT),
                onOpen = onOpenRecent,
                onSeeAll = onSeeAll,
                tileLabel = { "Saved photo, ${it.effectName}, ${it.savedLabel}" },
                modifier = Modifier.fillMaxWidth(),
                tile = recentTile,
            )
            Spacer(Modifier.height(16.dp))
            NoticesLink(onClick = onOpenNotices)
            Spacer(Modifier.height(8.dp))
        }

        // Docked button
        Box(
            Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
        ) {
            PillButton(text = "Choose photo", icon = SamarIcons.Photo, onClick = onChoosePhoto)
        }
    }
}

/** Quiet link to the open-source notices, at the very end of the scrolling content. */
@Composable
private fun NoticesLink(onClick: () -> Unit) {
    Box(
        Modifier
            .heightIn(min = 48.dp)
            .clip(SamarShapes.pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("Open-source notices", style = Samar.type.label, color = Samar.colors.mutedText)
    }
}

// --- Previews (Android Studio) ---

private val sampleRecent = listOf(
    RecentPhoto("1", "Smear") to Color(0xFFC9D6D3),
    RecentPhoto("2", "Glass") to Color(0xFFE5C9B8),
    RecentPhoto("3", "Pixelate") to Color(0xFFB7C4DA),
    RecentPhoto("4", "Solid") to Color(0xFFD6D3C0),
    RecentPhoto("5", "Gaussian") to Color(0xFFADBAB2),
    RecentPhoto("6", "Smear") to Color(0xFFCAB8D6),
)

@Composable
private fun HomePreview(dark: Boolean, withRecent: Boolean) = SamarTheme(darkTheme = dark) {
    HomeScreen(
        isDark = dark,
        onThemeChange = {},
        recent = if (withRecent) sampleRecent.map { it.first } else emptyList(),
        onChoosePhoto = {},
        onOpenRecent = {},
        onSeeAll = {},
        onOpenNotices = {},
        recentTile = { p ->
            Box(Modifier.fillMaxSize().background(sampleRecent.first { it.first == p }.second))
        },
    )
}

@Preview(name = "Light, with recent", widthDp = 390, heightDp = 844)
@Composable private fun HomeLight() = HomePreview(dark = false, withRecent = true)

@Preview(name = "Dark, with recent", widthDp = 390, heightDp = 844)
@Composable private fun HomeDark() = HomePreview(dark = true, withRecent = true)

@Preview(name = "Light, empty", widthDp = 390, heightDp = 844)
@Composable private fun HomeEmpty() = HomePreview(dark = false, withRecent = false)
