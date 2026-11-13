package com.cenotes.app.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import com.cenotes.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Same update system as Cemail (docs/DESIGN.md): each version is a GitHub release with its APK, and the app offers it.

/** A newer version of Cenotes published on GitHub. */
data class AppUpdate(val version: String, val apkUrl: String, val pageUrl: String, val notes: String = "")

/** Version numbers as "1.10.2" (a leading "v" is fine), compared number by number. */
object Versions {
    fun parse(text: String): List<Int>? {
        val parts = text.trim().removePrefix("v").removePrefix("V").split('.')
        if (parts.isEmpty()) return null
        return parts.map { it.toIntOrNull() ?: return null }
    }

    /** true if [candidate] is a later version than [current]. */
    fun isNewer(candidate: String, current: String): Boolean {
        val a = parse(candidate) ?: return false
        val b = parse(current) ?: return false
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}

/**
 * Looks on GitHub for a newer version of Cenotes (the latest release of its repository, with its APK), at most twice a
 * day, and downloads it for Android to install. GitHub sees the address of the connection and nothing else: no
 * account, no notes. Nothing is installed without the user: Android always asks.
 */
class Updates(private val context: Context) {
    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)

    private val _available = MutableStateFlow(stored())
    /** The newer version there is (and the user has not set aside), or null. */
    val available: StateFlow<AppUpdate?> = _available.asStateFlow()

    /** What a check found: the newer version, null when this one is the latest. Throws when GitHub could not be asked. */
    suspend fun check(force: Boolean = false): AppUpdate? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!force && now - prefs.getLong(KEY_CHECKED, 0) < EVERY) return@withContext _available.value
        val connection = URL(LATEST).openConnection() as HttpURLConnection
        val release = try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "Cenotes") // GitHub wants one; nothing about the user goes in it
            if (connection.responseCode !in 200..299) throw java.io.IOException("GitHub ${connection.responseCode}")
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
        prefs.edit().putLong(KEY_CHECKED, now).apply()
        val assets = release.optJSONArray("assets")
        val apk = (0 until (assets?.length() ?: 0)).map { assets!!.getJSONObject(it) }
            .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
        val version = release.optString("tag_name").removePrefix("v")
        val usable = !release.optBoolean("draft") && !release.optBoolean("prerelease") && apk != null
        val found = if (usable && Versions.isNewer(version, BuildConfig.VERSION_NAME)) {
            AppUpdate(
                version,
                apk!!.optString("browser_download_url"),
                release.optString("html_url"),
                if (release.isNull("body")) "" else release.optString("body"),
            )
        } else {
            null
        }
        Log.i(TAG, "checked: latest ${release.optString("tag_name")}, ${if (found != null) "newer" else "up to date"}")
        prefs.edit().apply {
            if (found == null) remove(KEY_VERSION).remove(KEY_APK).remove(KEY_PAGE).remove(KEY_NOTES)
            else putString(KEY_VERSION, found.version).putString(KEY_APK, found.apkUrl).putString(KEY_PAGE, found.pageUrl).putString(KEY_NOTES, found.notes)
        }.apply()
        // Asking by hand (Settings) offers it again even if it was set aside.
        if (force) prefs.edit().remove(KEY_IGNORED).apply()
        _available.value = found?.takeIf { it.version != prefs.getString(KEY_IGNORED, null) }
        found
    }

    /** "Not now": this version is not offered again (a later one will be). */
    fun setAside(update: AppUpdate) {
        prefs.edit().putString(KEY_IGNORED, update.version).apply()
        _available.value = null
    }

    /**
     * Downloads the APK with Android's download manager (its notification shows the progress) and opens the installer
     * when it is done. Returns false if the download could not even start.
     */
    fun download(update: AppUpdate, title: String): Boolean = try {
        val manager = context.getSystemService(DownloadManager::class.java)
        val name = "Cenotes-${update.version}.apk"
        java.io.File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), name).delete() // an earlier, unfinished try
        val id = manager.enqueue(
            DownloadManager.Request(Uri.parse(update.apkUrl))
                .setTitle(title)
                .setMimeType(APK)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, name),
        )
        prefs.edit().putLong(KEY_DOWNLOAD, id).putString(KEY_DOWNLOADED, update.version).apply()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) != id) return
                context.unregisterReceiver(this)
                if (!install(update)) Log.e(TAG, "download of ${update.version} failed")
            }
        }
        // The download manager is another app: its broadcast has to be let in.
        ContextCompat.registerReceiver(context, receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED)
        Log.i(TAG, "downloading ${update.version}")
        true
    } catch (e: Exception) {
        Log.e(TAG, "could not start the download", e)
        false
    }

    /** Whether Android already lets Cenotes install apps. Granting it can restart the app, so it is asked before downloading. */
    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    /** Opens the Android screen where the user allows installing apps from Cenotes. */
    fun askToAllowInstall(): Boolean = try {
        context.startActivity(
            Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        true
    } catch (e: Exception) {
        Log.e(TAG, "could not open the install permission", e)
        false
    }

    /**
     * Opens Android's installer with the downloaded APK of [update] (also again, after the installer was cancelled).
     * false while it is not fully downloaded. Android shows what is being installed and asks first.
     */
    fun install(update: AppUpdate): Boolean {
        if (prefs.getString(KEY_DOWNLOADED, null) != update.version) {
            Log.w(TAG, "install: ${update.version} was not downloaded")
            return false
        }
        val manager = context.getSystemService(DownloadManager::class.java)
        // The download manager's link to the file, or (when it does not give one, as on some emulators) the file
        // itself, which is in Cenotes' own folder, through our FileProvider.
        val file = manager.getUriForDownloadedFile(prefs.getLong(KEY_DOWNLOAD, -1))
            ?: downloadedApk(update)?.let { androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.files", it) }
        if (file == null) {
            Log.w(TAG, "install: the APK of ${update.version} is not there (yet)")
            return false
        }
        return try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW).setDataAndType(file, APK)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "could not open the installer", e)
            false
        }
    }

    /** The downloaded APK of [update] in Cenotes' download folder, once it is complete. */
    private fun downloadedApk(update: AppUpdate): java.io.File? {
        val manager = context.getSystemService(DownloadManager::class.java)
        val done = runCatching {
            manager.query(DownloadManager.Query().setFilterById(prefs.getLong(KEY_DOWNLOAD, -1))).use { c ->
                c.moveToFirst() && c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL
            }
        }.getOrDefault(false)
        val file = java.io.File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Cenotes-${update.version}.apk")
        Log.i(TAG, "install: download finished=$done, file=${file.length()} bytes")
        return file.takeIf { done && it.length() > 0 }
    }

    private fun stored(): AppUpdate? {
        val version = prefs.getString(KEY_VERSION, null) ?: return null
        if (!Versions.isNewer(version, BuildConfig.VERSION_NAME) || version == prefs.getString(KEY_IGNORED, null)) return null
        return AppUpdate(version, prefs.getString(KEY_APK, null) ?: return null, prefs.getString(KEY_PAGE, "").orEmpty(), prefs.getString(KEY_NOTES, "").orEmpty())
    }

    private companion object {
        const val TAG = "CenotesUpdates"
        const val LATEST = "https://api.github.com/repos/CactusRitter/cenotes/releases/latest"
        const val APK = "application/vnd.android.package-archive"
        const val EVERY = 12L * 60 * 60 * 1000
        const val KEY_CHECKED = "checked_at"
        const val KEY_VERSION = "version"
        const val KEY_APK = "apk"
        const val KEY_PAGE = "page"
        const val KEY_NOTES = "notes"
        const val KEY_IGNORED = "ignored"
        const val KEY_DOWNLOAD = "download_id"
        const val KEY_DOWNLOADED = "download_version"
    }
}
