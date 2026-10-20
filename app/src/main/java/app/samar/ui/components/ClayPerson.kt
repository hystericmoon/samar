package app.samar.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect

/**
 * Soft-3D "clay" portrait used by the Home hero. Drawn on a 196-unit square
 * (scaled to whatever size it is drawn at). Colours are fixed: like photos,
 * the illustration is never recoloured by the theme.
 *
 * @param turn head turn, -1 (looking left) .. 1 (looking right). Features move
 *   further than the head, which reads as a gentle 3D turn.
 */
internal object ClayPerson {

    const val UNITS = 196f

    // Head centre and size at rest, in units.
    private const val HEAD_CX = 98f
    private const val HEAD_CY = 80f
    private const val HEAD_W = 76f
    private const val HEAD_H = 86f

    private val Bg = listOf(Color(0xFFDDE7E4), Color(0xFFC9D6D3), Color(0xFFAFC1BD))
    private val SkinLight = Color(0xFFF2C6A6)
    private val Skin = Color(0xFFDBA98A)
    private val SkinShadow = Color(0xFFB57D5E)
    private val ShirtTop = Color(0xFF3D5F71)
    private val ShirtBottom = Color(0xFF223744)
    private val HairTop = Color(0xFF5A4038)
    private val HairBottom = Color(0xFF2E211D)
    private val Ink = Color(0xFF2B2220)
    private val Lip = Color(0xFF9A5546)

    /** The face area to hide, in units: the head, about 20% larger (like the app's masks). */
    fun faceBounds(): Rect {
        val w = HEAD_W * 1.2f
        val h = HEAD_H * 1.15f
        return Rect(Offset(HEAD_CX - w / 2, HEAD_CY - h / 2 - 2f), Size(w, h))
    }

    fun DrawScope.drawClayPerson(turn: Float = 0f) {
        val u = size.width / UNITS
        fun o(x: Float, y: Float) = Offset(x * u, y * u)
        fun s(w: Float, h: Float) = Size(w * u, h * u)

        // Background: lit from the top left.
        drawRect(Brush.radialGradient(Bg, center = o(64f, 52f), radius = 190f * u))

        // Shoulders, with a soft highlight on the near shoulder.
        val bodyLeft = 22f - turn * 2f
        drawRoundRect(
            Brush.verticalGradient(listOf(ShirtTop, ShirtBottom), startY = 126f * u, endY = 200f * u),
            topLeft = o(bodyLeft, 126f), size = s(152f, 150f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(72f * u),
        )
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                center = o(62f, 140f), radius = 56f * u,
            ),
            radius = 56f * u, center = o(62f, 140f),
        )

        val cx = HEAD_CX + turn * 4f
        val cy = HEAD_CY
        val ft = turn * 7f // features move more than the head

        // Neck and the shadow the chin casts on it.
        drawRoundRect(
            Brush.horizontalGradient(listOf(SkinShadow, Skin, SkinShadow), startX = 84f * u, endX = 112f * u),
            topLeft = o(84f, 110f), size = s(28f, 26f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * u),
        )
        drawOval(Color.Black.copy(alpha = 0.18f), topLeft = o(cx - 20f, 114f), size = s(40f, 12f))

        // Ears: the far ear tucks behind the head as it turns.
        val earY = cy + 2f
        drawOval(SkinShadow, topLeft = o(cx - HEAD_W / 2 - 6f + turn * 3f, earY - 10f), size = s(14f, 20f))
        drawOval(SkinShadow, topLeft = o(cx + HEAD_W / 2 - 8f + turn * 3f, earY - 10f), size = s(14f, 20f))

        // Head: rounded shading with the light from the top left.
        val head = Rect(o(cx - HEAD_W / 2, cy - HEAD_H / 2), s(HEAD_W, HEAD_H))
        drawOval(
            Brush.radialGradient(
                listOf(SkinLight, Skin, SkinShadow),
                center = o(cx - 14f + turn * 6f, cy - 22f), radius = 66f * u,
            ),
            topLeft = head.topLeft, size = head.size,
        )

        // Hair: a rounded cap, clipped to the top of the head.
        val headPath = Path().apply { addOval(head.inflate(3f * u)) }
        clipPath(headPath) {
            clipRect(bottom = (cy - 14f) * u) {
                drawOval(
                    Brush.verticalGradient(listOf(HairTop, HairBottom), startY = (cy - 48f) * u, endY = (cy - 10f) * u),
                    topLeft = o(cx - 44f + turn * 3f, cy - 50f), size = s(88f, 66f),
                )
            }
            // Side fringe sweeping down on one side.
            drawOval(HairBottom, topLeft = o(cx - 40f + ft * 0.5f, cy - 30f), size = s(30f, 22f))
        }
        drawOval(Color.White.copy(alpha = 0.18f), topLeft = o(cx - 26f, cy - 44f), size = s(28f, 10f))

        // Face.
        val eyeY = cy + 4f
        for (side in listOf(-1f, 1f)) {
            val ex = cx + side * 14f + ft
            // Brow
            drawLine(
                HairBottom.copy(alpha = 0.8f), o(ex - 6f, eyeY - 11f), o(ex + 6f, eyeY - 12f - side * 0.5f),
                strokeWidth = 2.6f * u, cap = StrokeCap.Round,
            )
            // Eye with a tiny highlight
            drawOval(Ink, topLeft = o(ex - 3.5f, eyeY - 4.5f), size = s(7f, 9f))
            drawCircle(Color.White.copy(alpha = 0.9f), radius = 1.3f * u, center = o(ex - 1.2f, eyeY - 2f))
            // Cheek
            drawCircle(Color(0xFFE88A7A).copy(alpha = 0.28f), radius = 6.5f * u, center = o(ex + side * 5f, eyeY + 13f))
        }
        // Nose: a soft shadow below a highlight.
        drawOval(SkinShadow.copy(alpha = 0.55f), topLeft = o(cx - 4f + ft * 1.2f, cy + 14f), size = s(9f, 6f))
        drawOval(Color.White.copy(alpha = 0.25f), topLeft = o(cx - 3f + ft * 1.2f, cy + 9f), size = s(4f, 5f))
        // Smile
        drawArc(
            Lip, startAngle = 20f, sweepAngle = 140f, useCenter = false,
            topLeft = o(cx - 9f + ft, cy + 18f), size = s(18f, 12f),
            style = Stroke(2.6f * u, cap = StrokeCap.Round),
        )
    }
}
