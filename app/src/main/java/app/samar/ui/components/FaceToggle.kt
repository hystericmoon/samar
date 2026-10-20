package app.samar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.samar.ui.icons.SamarIcons
import app.samar.ui.theme.Samar
import app.samar.ui.theme.SamarShapes

// Overlays sit on photos, so they use fixed colours that read on any image.
private val OverlayWhite = Color.White
private val OverlayInk = Color(0xFF15141C)

/**
 * The round badge above each face. Check on accent glass = blurred; plus on frosted glass = not blurred.
 * 36dp visual inside a 48dp touch target.
 */
@Composable
fun FaceToggle(
    faceNumber: Int,
    blurred: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(48.dp)
            .toggleable(value = blurred, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics {
                contentDescription = "Face $faceNumber"
                stateDescription = if (blurred) "Blurred" else "Not blurred"
            },
        contentAlignment = Alignment.Center,
    ) {
        // Glass badge; the solid edge keeps it at 3:1 on any photo.
        Box(
            Modifier
                .size(36.dp)
                .liquidGlass(SamarShapes.pill, if (blurred) GlassTone.Accent else GlassTone.Frost, elevation = 4.dp)
                .border(1.5.dp, if (blurred) OverlayWhite else OverlayInk, SamarShapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (blurred) SamarIcons.Check else SamarIcons.Plus,
                contentDescription = null,
                tint = if (blurred) Samar.colors.textOnAccent else OverlayInk,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
