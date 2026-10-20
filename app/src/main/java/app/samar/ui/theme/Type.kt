package app.samar.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.samar.R

/** Headings and wordmark. Bundled in the app (no network, always renders correctly). */
val Bricolage = FontFamily(
    Font(R.font.bricolage_semibold, FontWeight.SemiBold),
    Font(R.font.bricolage_bold, FontWeight.Bold),
    Font(R.font.bricolage_extrabold, FontWeight.ExtraBold),
)

/** All interface text. */
val InstrumentSans = FontFamily(
    Font(R.font.instrument_sans_medium, FontWeight.Medium),
    Font(R.font.instrument_sans_semibold, FontWeight.SemiBold),
    Font(R.font.instrument_sans_bold, FontWeight.Bold),
)

/** Type scale from DESIGN_BRIEF.md ("Sizes used"). */
@Immutable
data class SamarType(
    val wordmark: TextStyle,
    val hero: TextStyle,
    val screenTitle: TextStyle,
    val sectionHeading: TextStyle,
    val button: TextStyle,
    val buttonSmall: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val bodyStrong: TextStyle,
    val label: TextStyle,
)

val DefaultType = SamarType(
    wordmark = TextStyle(
        fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp, lineHeight = 28.sp, letterSpacing = (-0.03).em,
    ),
    hero = TextStyle(
        fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp, lineHeight = 36.sp, letterSpacing = (-0.03).em,
    ),
    screenTitle = TextStyle(
        fontFamily = Bricolage, fontWeight = FontWeight.Bold,
        fontSize = 19.sp, lineHeight = 24.sp, letterSpacing = (-0.02).em,
    ),
    sectionHeading = TextStyle(
        fontFamily = Bricolage, fontWeight = FontWeight.Bold,
        fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = (-0.02).em,
    ),
    button = TextStyle(
        fontFamily = Bricolage, fontWeight = FontWeight.Bold,
        fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.01).em,
    ),
    buttonSmall = TextStyle(
        fontFamily = Bricolage, fontWeight = FontWeight.Bold,
        fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = (-0.01).em,
    ),
    body = TextStyle(
        fontFamily = InstrumentSans, fontWeight = FontWeight.Medium,
        fontSize = 15.sp, lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = InstrumentSans, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 19.sp,
    ),
    bodyStrong = TextStyle(
        fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, lineHeight = 21.sp,
    ),
    label = TextStyle(
        fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp, lineHeight = 17.sp,
    ),
)

val LocalSamarType = staticCompositionLocalOf { DefaultType }
