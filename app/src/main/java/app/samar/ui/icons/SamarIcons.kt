package app.samar.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Line icons on a 24 grid, 2px stroke. Tint them with Icon(tint = …). */
object SamarIcons {

    val Sun: ImageVector by lazy {
        lineIcon(
            "Sun",
            "M12,8a4,4 0,1 0,0.01 0z",
            "M12,2v2", "M12,20v2", "M2,12h2", "M20,12h2",
            "M4.93,4.93l1.41,1.41", "M17.66,17.66l1.41,1.41",
            "M4.93,19.07l1.41,-1.41", "M17.66,6.34l1.41,-1.41",
        )
    }

    val Moon: ImageVector by lazy {
        lineIcon("Moon", "M12,3a6,6 0,0 0,9 9a9,9 0,1 1,-9 -9z")
    }

    val Photo: ImageVector by lazy {
        lineIcon(
            "Photo",
            "M5,3h14a2,2 0,0 1,2 2v14a2,2 0,0 1,-2 2h-14a2,2 0,0 1,-2 -2v-14a2,2 0,0 1,2 -2z",
            "M9,7a2,2 0,1 0,0.01 0z",
            "M21,15l-3.09,-3.09a2,2 0,0 0,-2.82 0L6,21",
        )
    }

    val Back: ImageVector by lazy { lineIcon("Back", "M19,12H5", "M12,19l-7,-7l7,-7") }

    val Undo: ImageVector by lazy {
        lineIcon(
            "Undo",
            "M9,14L4,9l5,-5",
            "M4,9h10.5a5.5,5.5 0,0 1,0 11H11",
        )
    }

    val Check: ImageVector by lazy { lineIcon("Check", "M20,6L9,17l-5,-5") }

    val Plus: ImageVector by lazy { lineIcon("Plus", "M12,5v14", "M5,12h14") }

    val Download: ImageVector by lazy { lineIcon("Download", "M12,4v11", "M7,10l5,5l5,-5", "M5,20h14") }

    val Share: ImageVector by lazy {
        lineIcon(
            "Share",
            "M18,2a3,3 0,1 0,0.01 0z", "M6,9a3,3 0,1 0,0.01 0z", "M18,16a3,3 0,1 0,0.01 0z",
            "M8.59,13.51l6.83,3.98", "M15.41,6.51l-6.82,3.98",
        )
    }

    val Close: ImageVector by lazy { lineIcon("Close", "M18,6L6,18", "M6,6l12,12") }

    private const val EYE = "M2.06,12.35a1,1 0,0 1,0 -0.7a10.75,10.75 0,0 1,19.88 0a1,1 0,0 1,0 0.7a10.75,10.75 0,0 1,-19.88 0z"
    private const val PUPIL = "M12,9a3,3 0,1 0,0.01 0z"

    val Eye: ImageVector by lazy { lineIcon("Eye", EYE, PUPIL) }

    val EyeOff: ImageVector by lazy { lineIcon("EyeOff", EYE, PUPIL, "M3,3l18,18") }

    private fun lineIcon(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            paths.forEach {
                addPath(
                    pathData = addPathNodes(it),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
}
