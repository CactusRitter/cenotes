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
 * Backup file: a .zip with `notes.json` (every note, Markdown content) and, optionally,
 * `images/<file>` for the pictures. Importing merges: notes that already exist are skipped.
 */
class Backup(private val context: Context, private val dao: NoteDao, private val images: ImageStore) {

    data class ExportResult(val notes: Int, val images: Int)
    data class ImportResult(val added: Int, val skipped: Int, val images: Int)

    class NotABackupException : Exception()

    suspend fun export(uri: Uri, includeImages: Boolean): ExportResult = withContext(Dispatchers.IO) {
        val notes = dao.all()
        var imageCount = 0
        val out = context.contentResolver.openOutputStream(uri, "wt") ?: error("No se pudo escribir el archivo")
        ZipOutputStream(out.buffered()).use { zip ->
            val array = JSONArray()
            notes.forEach { n ->
                array.put(
                    JSONObject()
                        .put("title", n.title)
                        .put("content", if (includeImages) n.content else withoutImages(n.content))
                        .put("pinned", n.pinned)
                        .put("updatedAt", n.updatedAt),
                )
            }
            val root = JSONObject()
                .put("app", AppId)
                .put("format", Format)
                .put("exportedAt", System.currentTimeMillis())
                .put("includesImages", includeImages)
                .put("notes", array)
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

    suspend fun import(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
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

        val existing = dao.all().map { it.title to it.content }.toHashSet()
        val list = root.getJSONArray("notes")
        var added = 0
        var skipped = 0
        for (i in 0 until list.length()) {
            val o = list.getJSONObject(i)
            val title = o.optString("title")
            val content = o.optString("content")
            if ((title to content) in existing || (title.isBlank() && content.isBlank())) {
                skipped++
                continue
            }
            dao.insert(
                Note(
                    title = title,
                    content = content,
                    pinned = o.optBoolean("pinned"),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                ),
            )
            // Only notes already on the phone are skipped; the backup itself is restored as it is.
            added++
        }
        ImportResult(added, skipped, imageCount)
    }

    private companion object {
        const val AppId = "Cenotes"
        const val Format = 1
        val SafeName = Regex("""^[\w.-]+$""")
    }
}
