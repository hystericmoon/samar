package app.samar.ui.notices

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.samar.data.Notice
import app.samar.data.Notices
import app.samar.data.NoticesLoader
import app.samar.ui.components.GlassIconButton
import app.samar.ui.components.UpdateSection
import app.samar.ui.icons.SamarIcons
import app.samar.ui.theme.Samar
import app.samar.ui.theme.SamarShapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Open-source notices: the bundled fonts and every library in the app, each with its
 * licence, under the version and update controls (About samar). Reached from the link at the bottom of Home.
 */
@Composable
fun NoticesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val notices by produceState<Notices?>(null) {
        value = withContext(Dispatchers.IO) { NoticesLoader.load(context.applicationContext) }
    }
    // Which notice is open, by name; null = the list.
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    val all = notices?.let { it.fonts + it.libraries }.orEmpty()
    val selected = all.firstOrNull { it.name == open }

    if (selected != null) {
        BackHandler { open = null }
        NoticeText(selected, onBack = { open = null })
    } else {
        BackHandler(onBack = onBack)
        NoticeScaffold("About samar", onBack) {
            item(key = "update") {
                UpdateSection(Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            }
            item(key = "notices-heading") {
                Text(
                    "Open-source notices",
                    style = Samar.type.sectionHeading,
                    color = Samar.colors.text,
                    modifier = Modifier
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp)
                        .semantics { heading() },
                )
            }
            item {
                Text(
                    "samar is made with these fonts and open-source libraries. Tap one to read its licence.",
                    style = Samar.type.body,
                    color = Samar.colors.mutedText,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
            notices?.let {
                section("Fonts", it.fonts) { n -> open = n.name }
                section("Libraries", it.libraries) { n -> open = n.name }
            }
        }
    }
}

private fun LazyListScope.section(title: String, items: List<Notice>, onOpen: (Notice) -> Unit) {
    item(key = "h-$title") {
        Text(
            title,
            style = Samar.type.sectionHeading,
            color = Samar.colors.text,
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 4.dp)
                .semantics { heading() },
        )
    }
    items(items, key = { "$title-${it.name}" }) { notice ->
        NoticeRow(notice.name, onClick = { onOpen(notice) })
    }
}

@Composable
private fun NoticeRow(name: String, onClick: () -> Unit) {
    val c = Samar.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = "Read licence", onClick = onClick),
    ) {
        Text(
            name,
            style = Samar.type.body,
            color = c.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .padding(horizontal = 24.dp, vertical = 13.dp),
        )
        HorizontalDivider(Modifier.padding(horizontal = 24.dp), thickness = 1.dp, color = c.divider)
    }
}

@Composable
private fun NoticeText(notice: Notice, onBack: () -> Unit) {
    // Split into paragraphs so very long licences scroll smoothly.
    // Licence files are hard-wrapped at ~80 characters; rejoin lines so they wrap to the screen.
    val paragraphs = notice.text.split(Regex("\n\\s*\n"))
        .map { reflow(it.trim('\n')) }
        .filter { it.isNotBlank() }
    NoticeScaffold(notice.name, onBack) {
        items(paragraphs) { p ->
            Text(
                p,
                style = Samar.type.bodySmall,
                color = Samar.colors.text,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
            )
        }
    }
}

/**
 * Joins wrapped lines. Drops rulers (----), and keeps list items (1. / a) / - / *) and
 * all-caps headings (PREAMBLE) on their own line.
 */
private fun reflow(paragraph: String): String {
    val ruler = Regex("^[-=_*\\s]{4,}$")
    val listItem = Regex("^\\s*(\\d+[.)]|[a-z][.)]|\\([a-z0-9]+\\)|[-*•])\\s")
    fun isHeading(line: String) = line.length <= 40 && line.any { it.isLetter() } && line == line.uppercase()
    val out = StringBuilder()
    var breakNext = false
    paragraph.lines().map { it.trim() }.filterNot { it.matches(ruler) }.forEach { line ->
        when {
            out.isEmpty() -> out.append(line)
            breakNext || isHeading(line) || listItem.containsMatchIn(line) -> out.append('\n').append(line)
            else -> out.append(' ').append(line)
        }
        breakNext = isHeading(line)
    }
    return out.toString()
}

@Composable
private fun NoticeScaffold(title: String, onBack: () -> Unit, content: LazyListScope.() -> Unit) {
    val c = Samar.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(SamarIcons.Back, "Back", onClick = onBack)
            Text(
                title,
                style = Samar.type.screenTitle,
                color = c.text,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            // Balances the back button so the title stays centred.
            Box(Modifier.size(48.dp))
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            content()
        }
        Box(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
    }
}
