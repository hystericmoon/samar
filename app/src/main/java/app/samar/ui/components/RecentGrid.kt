package app.samar.ui.components

import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.samar.ui.theme.Samar
import app.samar.ui.theme.SamarShapes

private const val COLUMNS = 3
private val Gap = 4.dp

/**
 * "Recent" section: heading, "See all", and a 3-column grid (4dp gaps, 6dp corners).
 * [tile] draws one item's thumbnail; [tileLabel] is what TalkBack reads for it.
 * Shows a quiet empty state when [items] is empty.
 */
@Composable
fun <T> RecentGrid(
    items: List<T>,
    onOpen: (T) -> Unit,
    onSeeAll: () -> Unit,
    tileLabel: (T) -> String,
    modifier: Modifier = Modifier,
    tile: @Composable (T) -> Unit,
) {
    val c = Samar.colors
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Recent",
                style = Samar.type.sectionHeading,
                color = c.text,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            if (items.isNotEmpty()) {
                Box(
                    Modifier
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Button, onClick = onSeeAll)
                        .padding(start = 12.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Text("See all", style = Samar.type.buttonSmall, color = c.accent)
                }
            }
        }
        Spacer(Modifier.padding(top = 4.dp))

        if (items.isEmpty()) {
            RecentEmpty()
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Gap)) {
                items.chunked(COLUMNS).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
                        row.forEach { item ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(SamarShapes.thumbnail)
                                    .clickable(
                                        role = Role.Button,
                                        onClickLabel = "Open in editor",
                                    ) { onOpen(item) }
                                    .semantics { contentDescription = tileLabel(item) },
                            ) { tile(item) }
                        }
                        // Keep tiles the same size in a short last row.
                        repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentEmpty() {
    val c = Samar.colors
    Box(
        Modifier
            .fillMaxWidth()
            .border(1.dp, c.divider, SamarShapes.thumbnail)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Photos you save will show up here.",
            style = Samar.type.bodySmall,
            color = c.mutedText,
            textAlign = TextAlign.Center,
        )
    }
}
