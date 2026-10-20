package app.samar.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** A samar release on GitHub with an APK attached. */
data class Release(val version: String, val apkUrl: String, val apkSize: Long, val pageUrl: String)

sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data object UpToDate : UpdateStatus
    data class Available(val release: Release) : UpdateStatus
    /** [progress] 0..1, or null while the size is unknown. */
    data class Downloading(val release: Release, val progress: Float?) : UpdateStatus
    data class Failed(val message: String, val release: Release? = null) : UpdateStatus
}

/**
 * Keeps samar up to date from its GitHub releases.
 *
 * - Talks only to GitHub (api.github.com and the release download). Nothing about the user or
 *   their photos is sent; GitHub sees an ordinary request for the latest release.
 * - Checks at most once a day on its own (can be turned off in About), or when asked.
 * - Before installing, the downloaded APK must be samar, newer, and signed with the same key as
 *   the installed app. Android's installer then asks the user to confirm.
 */
object Updater {
    private const val LATEST = "https://api.github.com/repos/hystericmoon/samar/releases/latest"
    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val ALREADY_CURRENT = "You already have this version."

    var status: UpdateStatus by mutableStateOf(UpdateStatus.Idle); private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    fun currentVersion(context: Context): String = packageInfo(context).versionName ?: "?"

    /** Called when the app opens: checks if automatic checks are on and a day has passed. */
    fun maybeAutoCheck(context: Context) {
        val prefs = UpdatePreference(context)
        if (!prefs.autoCheck) return
        if (System.currentTimeMillis() - prefs.lastCheck < DAY_MS) return
        check(context, quiet = true)
    }

    /** [quiet]: an automatic check stays silent unless a newer version is found. */
    fun check(context: Context, quiet: Boolean = false) {
        if (job?.isActive == true) return
        val app = context.applicationContext
        if (!quiet) status = UpdateStatus.Checking
        job = scope.launch {
            status = try {
                val release = withContext(Dispatchers.IO) { fetchLatest() }
                UpdatePreference(app).lastCheck = System.currentTimeMillis()
                when {
                    release != null && isNewer(release.version, currentVersion(app)) -> UpdateStatus.Available(release)
                    quiet -> UpdateStatus.Idle
                    else -> UpdateStatus.UpToDate
                }
            } catch (e: Exception) {
                if (quiet) UpdateStatus.Idle else UpdateStatus.Failed("Couldn't reach GitHub. Check your connection and try again.")
            }
        }
    }

    /** Downloads [release], checks it, and opens Android's installer. */
    fun downloadAndInstall(context: Context, release: Release) {
        if (job?.isActive == true) return
        val app = context.applicationContext
        // Android 8+: the user must allow samar to install apps once. Send them to that setting first.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !app.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            status = UpdateStatus.Available(release)
            return
        }
        status = UpdateStatus.Downloading(release, null)
        job = scope.launch {
            try {
                val apk = withContext(Dispatchers.IO) {
                    download(app, release) { p -> scope.launch { status = UpdateStatus.Downloading(release, p) } }
                }
                val problem = withContext(Dispatchers.IO) { verify(app, apk) }
                if (problem != null) {
                    apk.delete()
                    status = if (problem == ALREADY_CURRENT) UpdateStatus.UpToDate else UpdateStatus.Failed(problem, release)
                    return@launch
                }
                status = UpdateStatus.Available(release)
                install(context, apk)
            } catch (e: Exception) {
                status = UpdateStatus.Failed("The download didn't finish. Try again.", release)
            }
        }
    }

    // --- Network ---

    private fun fetchLatest(): Release? {
        val json = JSONObject(get(LATEST).bufferedReader().use { it.readText() })
        val tag = json.getString("tag_name")
        val assets = json.getJSONArray("assets")
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (a.getString("name").endsWith(".apk")) {
                return Release(
                    version = tag.removePrefix("v"),
                    apkUrl = a.getString("browser_download_url"),
                    apkSize = a.optLong("size", -1),
                    pageUrl = json.optString("html_url"),
                )
            }
        }
        return null
    }

    private fun download(context: Context, release: Release, onProgress: (Float?) -> Unit): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // only ever keep the latest download
        val out = File(dir, "samar-${release.version}.apk")
        get(release.apkUrl).use { input ->
            out.outputStream().use { output ->
                val buf = ByteArray(64 * 1024)
                var total = 0L
                var lastReport = 0L
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    output.write(buf, 0, n)
                    total += n
                    if (total - lastReport > 256 * 1024) {
                        lastReport = total
                        onProgress(if (release.apkSize > 0) total.toFloat() / release.apkSize else null)
                    }
                }
            }
        }
        return out
    }

    private fun get(url: String): java.io.InputStream {
        var target = URL(url)
        repeat(5) { // follow redirects (GitHub hands downloads to its file servers), HTTPS only
            require(target.protocol == "https") { "Insecure URL" }
            val c = target.openConnection() as HttpURLConnection
            c.instanceFollowRedirects = false
            c.connectTimeout = 15_000
            c.readTimeout = 30_000
            c.setRequestProperty("Accept", "application/vnd.github+json")
            c.setRequestProperty("User-Agent", "samar-android")
            when (c.responseCode) {
                in 200..299 -> return c.inputStream
                301, 302, 303, 307, 308 -> {
                    target = URL(target, c.getHeaderField("Location"))
                    c.disconnect()
                }
                else -> error("HTTP ${c.responseCode}")
            }
        }
        error("Too many redirects")
    }

    // --- Checks and install ---

    /** Returns null when the APK is a newer samar signed with our key, else a message for the user. */
    private fun verify(context: Context, apk: File): String? {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
        }
        val candidate = pm.getPackageArchiveInfo(apk.path, flags)
            ?: return "The download isn't a valid app. Try again."
        if (candidate.packageName != context.packageName) return "The download isn't samar."
        if (versionCode(candidate) <= versionCode(packageInfo(context))) return ALREADY_CURRENT
        if (signers(candidate) != signers(packageInfo(context, flags))) {
            return "The download isn't signed by samar's developer, so it wasn't installed."
        }
        return null
    }

    private fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.exports", apk)
        @Suppress("DEPRECATION")
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun signers(info: PackageInfo): Set<String> {
        val sigs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION") info.signatures
        } ?: return emptySet()
        val sha = MessageDigest.getInstance("SHA-256")
        return sigs.map { s -> sha.digest(s.toByteArray()).joinToString("") { "%02x".format(it) } }.toSet()
    }

    private fun packageInfo(context: Context, flags: Int = 0): PackageInfo =
        context.packageManager.getPackageInfo(context.packageName, flags)

    @Suppress("DEPRECATION")
    private fun versionCode(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()

    /** "1.10" > "1.9"; missing parts count as 0. */
    internal fun isNewer(candidate: String, current: String): Boolean {
        val a = candidate.split('.').map { it.toIntOrNull() ?: 0 }
        val b = current.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}

/** Remembers whether to check automatically, and when the last check happened. */
class UpdatePreference(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var autoCheck: Boolean
        get() = prefs.getBoolean("update_auto", true)
        set(v) { prefs.edit().putBoolean("update_auto", v).apply() }

    var lastCheck: Long
        get() = prefs.getLong("update_last_check", 0L)
        set(v) { prefs.edit().putLong("update_last_check", v).apply() }
}
