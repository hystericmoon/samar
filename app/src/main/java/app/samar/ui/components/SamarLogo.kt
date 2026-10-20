package app.samar.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.samar.R
import app.samar.ui.theme.Samar

/** The six-bar logo mark. Defaults to the accent colour. */
@Composable
fun SamarLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    color: Color = Samar.colors.accent,
) {
    Icon(
        painter = painterResource(R.drawable.ic_samar_mark),
        contentDescription = null,
        tint = color,
        modifier = modifier.size(size),
    )
}

/** Logo mark plus the lowercase "samar" wordmark, as in the Home top bar. */
@Composable
fun SamarWordmark(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = "samar" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SamarLogoMark(size = 32.dp)
        Text(text = "samar", style = Samar.type.wordmark, color = Samar.colors.text)
    }
}
