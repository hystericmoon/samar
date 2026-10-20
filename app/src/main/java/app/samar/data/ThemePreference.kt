package app.samar.data

import android.content.Context

enum class ThemeMode { System, Light, Dark }

/** Remembers the Home theme switch on the device. Defaults to following the system. */
class ThemePreference(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load(): ThemeMode =
        prefs.getString(KEY, null)
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.System

    fun save(mode: ThemeMode) {
        prefs.edit().putString(KEY, mode.name).apply()
    }

    private companion object {
        const val KEY = "theme"
    }
}
