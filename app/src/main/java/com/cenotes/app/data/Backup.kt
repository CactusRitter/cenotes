package com.cenotes.app.data

import android.content.Context
import android.net.Uri
import com.cenotes.app.BuildConfig
import com.cenotes.app.ui.markdown.imageNames
import com.cenotes.app.ui.markdown.withoutImages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backup container (shared with Cemail): a .zip with a `manifest.json` and the data in separate, clearly named entries:
 *
 * - `manifest.json`: `{"app":"cenotes","container":1,"appVersion":"…","createdAt":ms,"contents":["notes","settings"]}`
 * - `notes.json` (+ `images/<file>`): the notes, format 9 (see [Format]); images are optional.
 * - `settings.json`: the settings, exactly what [SettingsRepository.exportJson] writes.
 *
 * Importing reads the zip, looks at what is in it and lets the user choose what to restore. Only this container is
 * understood (no older formats). Importing notes merges: a note that is already here (same uuid) is kept or updated,
 * never duplicated.
 */
class Backup(
    private val context: Context,
    private val dao: NoteDao,
    private val images: ImageStore,
    private val settings: SettingsRepository,
) {

    data class ExportResult(val notes: Int, val images: Int)

    /** What a backup file carries (and so what the import can restore). */
    data class Contents(val notes: Boolean, val settings: Boolean)

    /**
     * [added] new notes; [updated] notes here replaced by a newer copy from the backup; [skipped] notes already here
     * (or empty); [kept] notes the backup has but that were deleted for good here, so they stay deleted.
     */
    data class ImportResult(val added: Int, val skipped: Int, val images: Int, val updated: Int = 0, val kept: Int = 0, val settingsRestored: Boolean = false, val notesRestored: Boolean = false)

    /** Not a Cenotes backup (another app's zip, or anything else). */
    class NotABackupException : Exception()

    /** A backup made by a newer container version than this app understands. */
    class TooNewException : Exception()

    // ---- Export ----

    /**
     * One backup zip: `manifest.json` + `notes.json` (+ `images/…` if [includeImages]) if [includeNotes] + `settings.json`
     * if [includeSettings]. `contents` in the manifest says what went in.
     */
    suspend fun export(uri: Uri, includeNotes: Boolean, includeImages: Boolean, includeSettings: Boolean): ExportResult = withContext(Dispatchers.IO) {
        require(includeNotes || includeSettings) { "Nothing to back up" }
        val includeImages = includeNotes && includeImages
        val notes = if (includeNotes) dao.all() else emptyList()
        val placeById = dao.places().associateBy { it.id }
        val labels = dao.labels()
        val labelName = labels.associate { it.id to it.name }
        val labelsOf = dao.links().groupBy({ it.noteId }, { labelName[it.labelId] })
        var imageCount = 0
        val out = context.contentResolver.openOutputStream(uri, "wt") ?: error("No se pudo escribir el archivo")
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putJson(ManifestEntry, manifest(listOfNotNull(ContentNotes.takeIf { includeNotes }, ContentSettings.takeIf { includeSettings })))

            if (includeNotes) {
            val array = JSONArray()
            notes.forEach { n ->
                array.put(
                    NoteSerializer.toJson(
                        n,
                        content = if (includeImages) n.content else withoutImages(n.content),
                        labelNames = labelsOf[n.id].orEmpty().filterNotNull(),
                        place = n.placeId?.let(placeById::get),
                    ),
                )
            }
            val root = JSONObject()
                .put("app", AppId)
                .put("format", Format)
                .put("exportedAt", System.currentTimeMillis())
                .put("includesImages", includeImages)
                // Labels by name, so they also survive when no note uses them.
                .put("labels", JSONArray(labels.map { it.name }))
                // Chosen colours by label name (0 = automatic, left out).
                .put("labelColors", JSONObject().also { o -> labels.filter { it.color != 0 }.forEach { l -> o.put(l.name, l.color) } })
                .put("notes", array)
                // Notes deleted for good, so an old backup or another phone does not bring them back.
                .put("deleted", JSONArray(dao.deletedNotes().map { JSONObject().put("uuid", it.uuid).put("deletedAt", it.deletedAt) }))
            zip.putText(NotesEntry, root.toString(2))
            }
            // The settings as the old "Settings backup" wrote them; never anything that looks like a secret (the file is plain text).
            if (includeSettings) zip.putText(SettingsEntry, settings.exportJson())

            if (includeImages) {
                notes.flatMap { imageNames(it.content) }.distinct().forEach { name ->
                    val f = images.file(name)
                    if (f.exists()) {
                        zip.putNextEntry(ZipEntry("$ImagesDir$name"))
                        f.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                        imageCount++
                    }
                }
            }
        }
        ExportResult(notes.size, imageCount)
    }

    private fun manifest(contents: List<String>) = JSONObject()
        .put("app", ManifestApp)
        .put("container", Container)
        .put("appVersion", BuildConfig.VERSION_NAME)
        .put("createdAt", System.currentTimeMillis())
        .put("contents", JSONArray(contents))

    private fun ZipOutputStream.putJson(name: String, json: JSONObject) = putText(name, json.toString(2))

    private fun ZipOutputStream.putText(name: String, text: String) {
        putNextEntry(ZipEntry(name))
        write(text.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    // ---- Import ----

    /**
     * Looks inside the file and says what it carries, so the import can ask what to restore. Accepts the zip container,
     * old zips with only `notes.json`, and the old loose settings `.json`. Throws [NotABackupException] for anything else
     * (another app's zip included) and [TooNewException] for a newer container.
     */
    suspend fun inspect(uri: Uri): Contents = withContext(Dispatchers.IO) {
        val input = BufferedInputStream(context.contentResolver.openInputStream(uri) ?: error("No se pudo leer el archivo"))
        input.use {
            var manifest: JSONObject? = null
            var hasNotes = false
            var hasSettings = false
            var notesApp = ""
            var entries = 0
            ZipInputStream(it).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (++entries > MaxEntries) throw NotABackupException()
                    when {
                        entry.isDirectory -> Unit
                        entry.name == ManifestEntry -> manifest = runCatching { JSONObject(readLimited(zip, MaxJsonBytes).toString(Charsets.UTF_8)) }.getOrNull()
                        entry.name == NotesEntry -> {
                            hasNotes = true
                            notesApp = runCatching { JSONObject(readLimited(zip, MaxJsonBytes).toString(Charsets.UTF_8)) }.getOrNull()?.optString("app").orEmpty()
                        }
                        entry.name == SettingsEntry -> hasSettings = looksLikeSettings(readLimited(zip, MaxJsonBytes).toString(Charsets.UTF_8))
                    }
                    zip.closeEntry()
                }
            }
            // The manifest says whose zip it is: another app's (or no manifest at all) is not a backup of ours.
            val m = manifest ?: throw NotABackupException()
            if (!m.optString("app").equals(ManifestApp, ignoreCase = true)) throw NotABackupException()
            if (m.optInt("container", 0) > Container) throw TooNewException()
            if (hasNotes && !notesApp.equals(AppId, ignoreCase = true)) hasNotes = false
            if (!hasNotes && !hasSettings) throw NotABackupException()
            Contents(hasNotes, hasSettings)
        }
    }

    /** Restores what the user chose ([notes] and/or [settings]) from the file; what it does not carry is ignored. */
    suspend fun import(uri: Uri, notes: Boolean, settings: Boolean): ImportResult = withContext(Dispatchers.IO) {
        var notesJson: String? = null
        var settingsJson: String? = null
        var imageCount = 0
        val input = BufferedInputStream(context.contentResolver.openInputStream(uri) ?: error("No se pudo leer el archivo"))
        input.use {
            var entries = 0
            ZipInputStream(it).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (++entries > MaxEntries) throw NotABackupException()
                    when {
                        entry.isDirectory -> Unit
                        entry.name == NotesEntry -> if (notes) notesJson = readLimited(zip, MaxJsonBytes).toString(Charsets.UTF_8)
                        entry.name == SettingsEntry -> if (settings) settingsJson = readLimited(zip, MaxJsonBytes).toString(Charsets.UTF_8)
                        notes && entry.name.startsWith(ImagesDir) -> {
                            // Only plain file names: nothing in the archive can write outside our folder.
                            val name = entry.name.removePrefix(ImagesDir)
                            if (SafeName.matches(name)) {
                                val f = images.file(name)
                                if (!f.exists()) {
                                    try {
                                        f.outputStream().use { out -> copyLimited(zip, out, MaxImageBytes) }
                                    } catch (e: IOException) {
                                        f.delete() // no half-written picture is left behind
                                        throw e
                                    }
                                    imageCount++
                                }
                            }
                        }
                    }
                    zip.closeEntry()
                }
            }
        }
        var result = ImportResult(0, 0, imageCount)
        if (notes) notesJson?.let { result = importNotes(it, imageCount) }
        var settingsRestored = false
        if (settings) settingsJson?.let { settingsRestored = this@Backup.settings.importJson(it) }
        result.copy(settingsRestored = settingsRestored)
    }

    private suspend fun importNotes(json: String, imageCount: Int): ImportResult {
        val root = runCatching { JSONObject(json) }.getOrNull()
        if (root == null || !root.optString("app").equals(AppId, ignoreCase = true)) throw NotABackupException()

        // Labels are matched by name (case-insensitive); missing ones are created.
        root.optJSONArray("labels")?.let { names ->
            for (i in 0 until names.length()) names.optString(i).trim().takeIf { it.isNotEmpty() }?.let { dao.labelIdFor(it) }
        }
        // Colours only fill in labels that are still automatic; the phone's own choices win.
        root.optJSONObject("labelColors")?.let { colors ->
            dao.labels().filter { it.color == 0 }.forEach { l ->
                val c = colors.optInt(l.name, 0)
                if (c != 0) dao.setLabelColor(l.id, c)
            }
        }

        // Notes the backup says were deleted for good are remembered here too (only so they do not come back; nothing is deleted).
        val tombstones = HashMap<String, Long>()
        dao.deletedNotes().forEach { tombstones[it.uuid] = it.deletedAt }
        root.optJSONArray("deleted")?.let { list ->
            val fresh = ArrayList<DeletedNote>()
            for (i in 0 until list.length()) {
                val d = list.optJSONObject(i) ?: continue
                val uuid = d.optString("uuid").trim()
                val at = d.optLong("deletedAt", 0L)
                if (uuid.isNotEmpty() && at > (tombstones[uuid] ?: -1L)) {
                    tombstones[uuid] = at
                    fresh += DeletedNote(uuid, at)
                }
            }
            if (fresh.isNotEmpty()) dao.insertDeleted(fresh)
        }

        val list = root.getJSONArray("notes")
        var added = 0
        var skipped = 0
        var updated = 0
        var kept = 0
        for (i in 0 until list.length()) {
            val p = NoteSerializer.fromJson(list.getJSONObject(i))
            val note = p.note
            if (note.title.isBlank() && note.content.isBlank()) {
                skipped++
                continue
            }
            val id: Long
            val local = dao.byUuid(note.uuid)
            if (local != null) {
                // Same note: the most recently changed copy wins.
                if (note.modifiedAt <= local.modifiedAt) {
                    skipped++
                    continue
                }
                id = dao.insert(note.copy(id = local.id))
                updated++
            } else {
                // Deleted for good here after (or when) the backup's copy was last changed: it stays deleted.
                val deletedAt = tombstones[note.uuid]
                if (deletedAt != null && deletedAt >= note.modifiedAt) {
                    kept++
                    continue
                }
                id = dao.insert(note)
                added++
            }
            dao.setLabels(id, p.labels.map { dao.labelIdFor(it) })
            p.place?.let { pl ->
                val placeId = dao.placeByName(pl.name)?.id ?: dao.insertPlace(Place(name = pl.name, latitude = pl.latitude, longitude = pl.longitude))
                dao.setPlace(id, placeId, pl.leave)
            }
            // The label and place calls above stamp "now"; the note keeps the time it had in the backup.
            dao.setModified(id, note.modifiedAt)
        }
        return ImportResult(added, skipped, imageCount, updated, kept, notesRestored = true)
    }

    // ---- Helpers ----

    /** A settings file ([SettingsRepository.exportJson]): app "cenotes" with a "settings" object. */
    private fun looksLikeSettings(text: String): Boolean {
        val o = runCatching { JSONObject(text) }.getOrNull() ?: return false
        return o.optString("app").equals(ManifestApp, ignoreCase = true) && o.optJSONObject("settings") != null
    }

    /** Reads at most [max] bytes of an entry; a bigger one is not a backup (guards against zip bombs). */
    private fun readLimited(input: InputStream, max: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            if (out.size() + n > max) throw NotABackupException()
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    private fun copyLimited(input: InputStream, out: java.io.OutputStream, max: Long) {
        val buffer = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            total += n
            if (total > max) throw IOException("Entry too big")
            out.write(buffer, 0, n)
        }
    }

    private companion object {
        /** The app's name inside the old `notes.json` ("Cenotes") and in the manifest and settings file ("cenotes"); compared ignoring case. */
        const val AppId = "Cenotes"
        const val ManifestApp = "cenotes"
        const val Container = 1
        const val ManifestEntry = "manifest.json"
        const val NotesEntry = "notes.json"
        const val SettingsEntry = "settings.json"
        const val ImagesDir = "images/"
        const val ContentNotes = "notes"
        const val ContentSettings = "settings"

        /** Limits for an untrusted zip: JSON entries, each image, and the number of entries. */
        const val MaxJsonBytes = 64 * 1024 * 1024
        const val MaxImageBytes = 64L * 1024 * 1024
        const val MaxEntries = 20_000

        /** 1 = notes only (0.7); 2 = + labels and archive (0.8); 3 = + label colours (0.9); 4 = + note colour and creation date (0.12); 5 = + reminders (0.13); 6 = + protected notes (0.14); 7 = + order by hand (0.16); 8 = + reminders at a place (0.17); 9 = + uuid and modifiedAt per note and notes deleted for good (next version). Only 9 is imported. The Trash and the history of each note are not exported. */
        const val Format = 9
        val SafeName = Regex("""^[\w.-]+$""")
    }
}
