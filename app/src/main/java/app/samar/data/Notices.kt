package app.samar.data

import android.content.Context
import app.samar.R

/** One font or library samar ships, with the licence text to show for it. */
data class Notice(val name: String, val text: String)

data class Notices(val fonts: List<Notice>, val libraries: List<Notice>)

/**
 * Reads the notices bundled in the app. Fonts come from res/raw. Libraries come from
 * the list the OSS licenses Gradle plugin writes at build time (release builds only;
 * debug builds get a placeholder). Nothing is fetched from the network.
 */
object NoticesLoader {
    fun load(context: Context): Notices {
        val res = context.resources
        fun raw(id: Int) = res.openRawResource(id).use { it.readBytes() }

        val apache = raw(R.raw.apache_2_0).decodeToString()
        val fonts = listOf(
            Notice("Bricolage Grotesque", raw(R.raw.ofl_bricolage_grotesque).decodeToString()),
            Notice("Instrument Sans", raw(R.raw.ofl_instrument_sans).decodeToString()),
        )

        // Metadata lines are "start:length Name"; start and length are byte offsets.
        val texts = raw(R.raw.third_party_licenses)
        val libraries = raw(R.raw.third_party_license_metadata).decodeToString()
            .lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val (range, name) = line.split(' ', limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
                val (start, length) = range.split(':').map { it.toInt() }
                Notice(name, expand(texts.decodeToString(start, start + length).trim(), apache))
            }
            .distinctBy { it.name }
            .sortedBy { it.name.lowercase() }
            .toList()
        return Notices(fonts, libraries)
    }

    /** Many libraries only list a licence URL. Show the bundled text instead where we can. */
    private fun expand(text: String, apache: String): String = when {
        text.matches(Regex("https?://www\\.apache\\.org/licenses/LICENSE-2\\.0(\\.txt|\\.html)?")) -> apache
        text.startsWith("http") && !text.contains('\n') -> "This library is covered by these terms:\n$text"
        else -> text
    }
}
