package app.samar.data

import android.content.Context

/** Auto: every face found is blurred. Manual: faces are found but none blurred; the user picks. */
enum class SelectionMode { Auto, Manual }

/** Remembers the editor's Auto / Manual choice on the device. Defaults to Auto. */
class SelectionPreference(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load(): SelectionMode =
        prefs.getString(KEY, null)
            ?.let { runCatching { SelectionMode.valueOf(it) }.getOrNull() }
            ?: SelectionMode.Auto

    fun save(mode: SelectionMode) {
        prefs.edit().putString(KEY, mode.name).apply()
    }

    private companion object {
        const val KEY = "selection"
    }
}
