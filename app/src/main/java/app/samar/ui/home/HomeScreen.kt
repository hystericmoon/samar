package app.samar.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.samar.data.SelectionMode
import app.samar.ui.components.GlassTone
import app.samar.ui.components.HeroAnimation
import app.samar.ui.components.PillButton
import app.samar.ui.components.RecentThumb
import app.samar.ui.components.SamarLogoMark
import app.samar.ui.components.UpdateBanner
import app.samar.ui.components.glassCard
import app.samar.ui.components.glassHairline
import app.samar.ui.components.liquidGlass
import app.samar.ui.icons.SamarIcons
import app.samar.ui.theme.Overlay
import app.samar.ui.theme.Samar
import app.samar.ui.theme.SamarShapes
import app.samar.ui.theme.SamarTheme
import app.samar.ui.theme.samarBackdrop
import java.io.File

/** One saved photo in Recent. */
data class RecentPhoto(
    val id: String,
    val effectName: String,
    val savedLabel: String = "",
    val thumbnail: File? = null,
    val facesHidden: Int = 0,
)

/** Home shows the latest save as a highlight card plus the next three; "See all" shows the rest. */
const val HOME_RECENT_COUNT = 4

private val CardShape = RoundedCornerShape(22.dp)

@Composable
fun HomeScreen(
    isDark: Boolean,
    onThemeChange: (Boolean) -> Unit,
    selectionMode: SelectionMode,
    onSelectionModeChange: (SelectionMode) -> Unit,
    recent: List<RecentPhoto>,
    onChoosePhoto: () -> Unit,
    onOpenRecent: (RecentPhoto) -> Unit,
    onSeeAll: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
    recentTile: @Composable (RecentPhoto) -> Unit = { RecentThumb(it.thumbnail) },
) {
    val c = Samar.colors
    Column(
        modifier
            .fillMaxSize()
            .samarBackdrop()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        // Top bar: logo chip on the left, theme and About on the right.
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LogoChip()
            Spacer(Modifier.weight(1f))
            GlassCircle(
                icon = if (isDark) SamarIcons.Moon else SamarIcons.Sun,
                label = "Dark theme",
                toggled = isDark,
                onClick = { onThemeChange(!isDark) },
            )
            GlassCircle(icon = SamarIcons.Info, label = "About samar", onClick = onOpenAbout)
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            HeroAnimation(Modifier.align(Alignment.CenterHorizontally), size = 150.dp)
            Spacer(Modifier.height(20.dp))
            Text(
                "Blur faces\nbefore you share.",
                style = Samar.type.hero,
                color = c.text,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(8.dp))
            Text("Everything stays on your phone.", style = Samar.type.body, color = c.mutedText)
            Spacer(Modifier.height(20.dp))
            StartCard(selectionMode)
            Spacer(Modifier.height(14.dp))
            PillButton(text = "Choose photo", icon = SamarIcons.Photo, onClick = onChoosePhoto)
            Spacer(Modifier.height(20.dp))
            UpdateBanner(Modifier.padding(bottom = 16.dp))
            RecentSection(recent.take(HOME_RECENT_COUNT), onOpenRecent, onSeeAll, recentTile)
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Glass pill with the logo (neon mark on a navy disc) and the wordmark. */
@Composable
private fun LogoChip() {
    Row(
        Modifier
            .liquidGlass(SamarShapes.pill, GlassTone.Clear)
            .border(1.dp, glassHairline(), SamarShapes.pill)
            .clearAndSetSemantics { contentDescription = "samar" }
            .padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(32.dp)
                .background(Overlay.Ink, SamarShapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            SamarLogoMark(size = 20.dp, color = Overlay.Highlight)
        }
        Text("samar", style = Samar.type.buttonSmall, color = Samar.colors.text)
    }
}

/** 42dp glass circle in a 48dp target. With [toggled] it reads as a switch to TalkBack. */
@Composable
private fun GlassCircle(icon: ImageVector, label: String, onClick: () -> Unit, toggled: Boolean? = null) {
    val interaction = remember { MutableInteractionSource() }
    val target = if (toggled != null) {
        Modifier.toggleable(toggled, interaction, null, role = Role.Switch) { onClick() }
    } else {
        Modifier.clickable(interaction, null, role = Role.Button, onClick = onClick)
    }
    Box(
        Modifier.size(48.dp).then(target).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .liquidGlass(SamarShapes.pill, GlassTone.Clear, interaction)
                .border(1.dp, glassHairline(), SamarShapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Samar.colors.text, modifier = Modifier.size(20.dp))
        }
    }
}

/** Glass card explaining what happens next; follows the Auto / Manual choice. */
@Composable
private fun StartCard(mode: SelectionMode) {
    val c = Samar.colors
    Row(
        Modifier
            .fillMaxWidth()
            .glassCard(CardShape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(Color(0x595882E8), Color(0x591F06AC))))
                .border(1.dp, glassHairline(), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(SamarIcons.Photo, contentDescription = null, tint = c.text, modifier = Modifier.size(22.dp))
        }
        Column {
            Text("Start with", style = Samar.type.label, color = c.mutedText)
            Text(
                if (mode == SelectionMode.Auto) "A photo; every face gets blurred" else "A photo; you pick the faces",
                style = Samar.type.bodyStrong,
                color = c.text,
            )
        }
    }
}

/** "Recent": the latest save as a neon highlight card, then up to three more thumbnails. */
@Composable
private fun RecentSection(
    items: List<RecentPhoto>,
    onOpen: (RecentPhoto) -> Unit,
    onSeeAll: () -> Unit,
    tile: @Composable (RecentPhoto) -> Unit,
) {
    val c = Samar.colors
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Recent",
            style = Samar.type.sectionHeading,
            color = c.text,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (items.isNotEmpty()) {
            Box(
                Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onSeeAll).padding(start = 12.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("See all", style = Samar.type.buttonSmall, color = c.accent)
            }
        }
    }
    if (items.isEmpty()) {
        Box(
            Modifier.fillMaxWidth().glassCard(CardShape).padding(horizontal = 24.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Photos you save will show up here.", style = Samar.type.bodySmall, color = c.mutedText)
        }
        return
    }
    val first = items.first()
    val faces = if (first.facesHidden == 1) "1 face hidden" else "${first.facesHidden} faces hidden"
    Row(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .drawBehind {
                drawRect(Brush.linearGradient(listOf(Overlay.Highlight, Color(0xFFD2E244), Color(0xFFB8C93A))))
            }
            .border(1.dp, Color(0xB3F0FA8C), CardShape)
            .clickable(role = Role.Button, onClickLabel = "Open in editor") { onOpen(first) }
            .semantics { contentDescription = "Latest save, $faces, ${first.effectName}, ${first.savedLabel}" }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
        ) { tile(first) }
        Column {
            Text(first.savedLabel.replaceFirstChar { it.uppercase() }, style = Samar.type.label, color = Overlay.Ink.copy(alpha = 0.75f))
            Text(faces, style = Samar.type.screenTitle, color = Overlay.Ink)
            Text(first.effectName, style = Samar.type.label, color = Overlay.Ink.copy(alpha = 0.75f))
        }
    }
    val rest = items.drop(1)
    if (rest.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            rest.forEach { photo ->
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(1.4f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, glassHairline(), RoundedCornerShape(14.dp))
                        .clickable(role = Role.Button, onClickLabel = "Open in editor") { onOpen(photo) }
                        .semantics { contentDescription = "Saved photo, ${photo.effectName}, ${photo.savedLabel}" },
                ) { tile(photo) }
            }
            repeat(3 - rest.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

// --- Previews (Android Studio) ---

private val sampleRecent = listOf(
    RecentPhoto("1", "Smear", "saved 20 Oct", facesHidden = 3) to Color(0xFFC9D6D3),
    RecentPhoto("2", "Glass") to Color(0xFFE5C9B8),
    RecentPhoto("3", "Pixelate") to Color(0xFFB7C4DA),
    RecentPhoto("4", "Solid") to Color(0xFFD6D3C0),
)

@Composable
private fun HomePreview(dark: Boolean, withRecent: Boolean) = SamarTheme(darkTheme = dark) {
    HomeScreen(
        isDark = dark,
        onThemeChange = {},
        selectionMode = SelectionMode.Auto,
        onSelectionModeChange = {},
        recent = if (withRecent) sampleRecent.map { it.first } else emptyList(),
        onChoosePhoto = {},
        onOpenRecent = {},
        onSeeAll = {},
        onOpenAbout = {},
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
