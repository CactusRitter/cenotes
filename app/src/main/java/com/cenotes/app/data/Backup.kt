package com.cenotes.app.data

import android.content.Context
import android.net.Uri
import com.cenotes.app.ui.markdown.imageNames
import com.cenotes.app.ui.markdown.withoutImages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backup file: a .zip with `notes.json` (every note, Markdown content; the app's settings too if asked) and, optionally,
 * `images/<file>` for the pictures. Importing merges: a note that is already here (same uuid, or same title and text in
 * old backups) is kept or updated, never duplicated.
 */
class Backup(
    private val context: Context,
    private val dao: NoteDao,
    private val images: ImageStore,
    private val settings: SettingsRepository,
) {

    data class ExportResult(val notes: Int, val images: Int)

    /**
     * [added] new notes; [updated] notes here replaced by a newer copy from the backup; [skipped] notes already here
     * (or empty); [kept] notes the backup has but that were deleted for good here, so they stay deleted.
     */
    data class ImportResult(val added: Int, val skipped: Int, val images: Int, val updated: Int = 0, val kept: Int = 0, val settingsRestored: Boolean = false)

    class NotABackupException : Exception()

    suspend fun export(uri: Uri, includeImages: Boolean, includeSettings: Boolean = false): ExportResult = withContext(Dispatchers.IO) {
        val notes = dao.all()
        val placeById = dao.places().associateBy { it.id }
        val labels = dao.labels()
        val labelName = labels.associate { it.id to it.name }
        val labelsOf = dao.links().groupBy({ it.noteId }, { labelName[it.labelId] })
        var imageCount = 0
        val out = context.contentResolver.openOutputStream(uri, "wt") ?: error("No se pudo escribir el archivo")
        ZipOutputStream(out.buffered()).use { zip ->
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
                .put("labelColors", JSONObject().also { o -> labels.filter { it.color != 0 }.forEach { o.put(it.name, it.color) } })
                .put("notes", array)
                // Notes deleted for good, so an old backup or another phone does not bring them back.
                .put("deleted", JSONArray(dao.deletedNotes().map { JSONObject().put("uuid", it.uuid).put("deletedAt", it.deletedAt) }))
            // The settings exactly as stored (typed), never anything that looks like a secret: this file is plain text.
            if (includeSettings) root.put("settings", settings.exportValues())
            zip.putNextEntry(ZipEntry("notes.json"))
            zip.write(root.toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            if (includeImages) {
                notes.flatMap { imageNames(it.content) }.distinct().forEach { name ->
                    val f = images.file(name)
                    if (f.exists()) {
                        zip.putNextEntry(ZipEntry("images/$name"))
                        f.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                        imageCount++
                    }
                }
            }
        }
        ExportResult(notes.size, imageCount)
    }

    /** Says whether the backup carries the app's settings (so the import can ask whether to restore them). Reads only `notes.json`. */
    suspend fun hasSettings(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: error("No se pudo leer el archivo")
        var json: String? = null
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name == "notes.json") {
                    json = zip.readBytes().toString(Charsets.UTF_8)
                    break
                }
            }
        }
        val root = json?.let { runCatching { JSONObject(it) }.getOrNull() }
        if (root == null || root.optString("app") != AppId) throw NotABackupException()
        root.optJSONObject("settings") != null
    }

    suspend fun import(uri: Uri, restoreSettings: Boolean = false): ImportResult = withContext(Dispatchers.IO) {
        var json: String? = null
        var imageCount = 0
        val input = context.contentResolver.openInputStream(uri) ?: error("No se pudo leer el archivo")
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                when {
                    entry.isDirectory -> Unit
                    entry.name == "notes.json" -> json = zip.readBytes().toString(Charsets.UTF_8)
                    entry.name.startsWith("images/") -> {
                        // Only plain file names: nothing in the archive can write outside our folder.
                        val name = entry.name.removePrefix("images/")
                        if (SafeName.matches(name)) {
                            val f = images.file(name)
                            if (!f.exists()) {
                                f.outputStream().use { zip.copyTo(it) }
                                imageCount++
                            }
                        }
                    }
                }
                zip.closeEntry()
            }
        }
        val root = json?.let { runCatching { JSONObject(it) }.getOrNull() }
        if (root == null || root.optString("app") != AppId) throw NotABackupException()

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

        val existing = dao.all().map { it.title to it.content }.toHashSet()
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
            if (p.hasUuid) {
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
            } else {
                // Old backups (and keep2cenotes) have no uuid: a note already here with the same title and text is skipped.
                if ((note.title to note.content) in existing) {
                    skipped++
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

        var settingsRestored = false
        if (restoreSettings) root.optJSONObject("settings")?.let {
            settings.importValues(it)
            settingsRestored = true
        }
        ImportResult(added, skipped, imageCount, updated, kept, settingsRestored)
    }

    private companion object {
        const val AppId = "Cenotes"
        /** 1 = notes only (0.7); 2 = + labels and archive (0.8); 3 = + label colours (0.9); 4 = + note colour and creation date (0.12); 5 = + reminders (0.13); 6 = + protected notes (0.14); 7 = + order by hand (0.16); 8 = + reminders at a place (0.17); 9 = + uuid and modifiedAt per note, notes deleted for good, optional settings (1.6). All can be imported. The Trash and the history of each note are not exported. */
        const val Format = 9
        val SafeName = Regex("""^[\w.-]+$""")
    }
}
