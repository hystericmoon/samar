package app.samar.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.samar.ui.theme.Samar
import app.samar.ui.theme.SamarShapes

/** Primary full-width pill button: tinted accent glass. Optional leading icon. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) = GlassPill(text, onClick, GlassTone.Accent, modifier, icon, elevation = 10.dp, outline = false)

/** Secondary pill button: clear glass with a visible edge (Cancel, Share, Clear all). */
@Composable
fun OutlinedPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) = GlassPill(text, onClick, GlassTone.Clear, modifier, icon, elevation = 4.dp, outline = true)

@Composable
private fun GlassPill(
    text: String,
    onClick: () -> Unit,
    tone: GlassTone,
    modifier: Modifier,
    icon: ImageVector?,
    elevation: Dp,
    outline: Boolean,
) {
    val interaction = remember { MutableInteractionSource() }
    val content = glassContentColor(tone)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .liquidGlass(SamarShapes.pill, tone, interaction, elevation, outline)
            .clickable(interaction, ripple(color = content), role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(24.dp))
        }
        Text(text, style = Samar.type.button, color = content)
    }
}

/**
 * Round glass icon button for top bars (Back, Undo, Close): a 40dp glass circle inside a
 * 48dp touch target. [label] is what TalkBack reads.
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: GlassTone = GlassTone.Clear,
) {
    val interaction = remember { MutableInteractionSource() }
    val content = glassContentColor(tone)
    Box(
        modifier
            .size(48.dp)
            .clickable(interaction, null, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .liquidGlass(SamarShapes.pill, tone, interaction, elevation = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon, contentDescription = null,
                tint = if (enabled) content else content.copy(alpha = 0.38f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
