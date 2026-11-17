package com.cenotes.app.sync

import android.content.SharedPreferences
import com.cenotes.app.data.DeletedNote
import com.cenotes.app.data.ImageStore
import com.cenotes.app.data.Note
import com.cenotes.app.data.NoteDao
import com.cenotes.app.data.NoteSerializer
import com.cenotes.app.data.Place
import com.cenotes.app.data.newUuid
import com.cenotes.app.ui.markdown.imageNames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * EXPERIMENT (branch experimento-drive): the note sync. No manifest file: the listing of the app folder, with each file's
 * `appProperties.m` (modifiedAt or deletedAt), is the manifest, so one request tells the state of everything.
 *
 * Files in `appDataFolder` (names are public, contents are AES-GCM with the data key, AAD = the file name):
 * - `n-<uuid>`: a note (NoteSerializer JSON + trashedAt), `m` = its modifiedAt.
 * - `t-<uuid>`: a note deleted for good (empty content), `m` = deletedAt. Beats an older edit of the note.
 * - `i-<image file name>`: an image.
 * - `keyring.json`: see [DriveKeyring].
 *
 * Conflicts: [base] remembers, per note, the modifiedAt both sides had at the last sync. If both sides changed, the newest
 * wins and the loser is kept as a new note "… (conflicto)": no text is ever lost.
 */
class DriveSync(
    private val api: DriveApi,
    private val dek: ByteArray,
    private val dao: NoteDao,
    private val images: ImageStore,
    private val prefs: SharedPreferences,
) {
    class Report(
        val up: Int, val down: Int, val deletedLocal: Int, val deletedRemote: Int, val conflicts: Int,
        val imagesUp: Int, val imagesDown: Int, val requests: Int, val ms: Long, val details: List<String>,
    ) {
        override fun toString() =
            "Sync done in ${ms} ms, $requests requests: up $up, down $down, deleted here $deletedLocal, deleted in Drive $deletedRemote, " +
                "conflicts $conflicts, images up $imagesUp / down $imagesDown." + details.joinToString("") { "\n· $it" }
    }

    private val gate = Semaphore(6)
    /** Label and place creation and note writes one at a time: two notes may name the same new label. */
    private val dbLock = Mutex()
    private val up = AtomicInteger()
    private val down = AtomicInteger()
    private val delLocal = AtomicInteger()
    private val delRemote = AtomicInteger()
    private val conflicts = AtomicInteger()
    private val imgUp = AtomicInteger()
    private val imgDown = AtomicInteger()
    private val details = java.util.Collections.synchronizedList(ArrayList<String>())
    private val base = ConcurrentHashMap<String, Long>()

    private fun loadBase() {
        runCatching {
            val o = JSONObject(prefs.getString(BaseKey, "{}") ?: "{}")
            o.keys().forEach { base[it] = o.getLong(it) }
        }
    }

    private fun saveBase() {
        val o = JSONObject()
        base.forEach { (k, v) -> o.put(k, v) }
        prefs.edit().putString(BaseKey, o.toString()).apply()
    }

    private fun aad(name: String) = name.toByteArray()
    private fun DriveFile.m() = props["m"]?.toLongOrNull() ?: 0L

    suspend fun sync(): Report = withContext(Dispatchers.Default) {
        val started = System.currentTimeMillis()
        val before = api.requests.get()
        loadBase()
        val remote = api.list()
        val rn = remote.filter { it.name.startsWith("n-") }.associateBy { it.name.removePrefix("n-") }
        val rt = remote.filter { it.name.startsWith("t-") }.associateBy { it.name.removePrefix("t-") }
        val ri = remote.filter { it.name.startsWith("i-") }.associateBy { it.name.removePrefix("i-") }.toMutableMap()
        val locals = dao.allForSync().associateBy { it.uuid }
        val ltomb = dao.deletedNotes().associateBy { it.uuid }
        val labelName = dao.labels().associate { it.id to it.name }
        val labelsOf = dao.links().groupBy({ it.noteId }, { labelName[it.labelId] })
        val placeById = dao.places().associateBy { it.id }
        val ctx = Ctx(labelsOf, placeById, ri)

        val uuids = (rn.keys + rt.keys + locals.keys + ltomb.keys)
        coroutineScope {
            uuids.map { u ->
                async { gate.withPermit { one(u, locals[u], ltomb[u], rn[u], rt[u], ctx) } }
            }.awaitAll()
        }
        saveBase()
        Report(up.get(), down.get(), delLocal.get(), delRemote.get(), conflicts.get(), imgUp.get(), imgDown.get(),
            api.requests.get() - before, System.currentTimeMillis() - started, details.toList())
    }

    private class Ctx(val labelsOf: Map<Long, List<String?>>, val placeById: Map<Long, Place>, val remoteImages: MutableMap<String, DriveFile>)

    private suspend fun one(u: String, l: Note?, tl: DeletedNote?, r: DriveFile?, tr: DriveFile?, c: Ctx) {
        if (tr != null) {
            val d = tr.m()
            if (r != null && d >= r.m()) { api.delete(r.id); delRemote.incrementAndGet() }
            if (l != null) {
                if (d >= l.modifiedAt) {
                    dbLock.withLock { dao.deleteNotes(listOf(l.id)) }; base.remove(u); delLocal.incrementAndGet()
                    details += "deleted here (deleted elsewhere): ${l.title.take(30)}"
                } else {
                    // Edited here after it was deleted elsewhere: the edit wins and the note comes back.
                    uploadNote(l, null, c); api.delete(tr.id); base[u] = l.modifiedAt
                    details += "kept (edited after deletion elsewhere): ${l.title.take(30)}"
                }
            }
            return
        }
        if (l == null) {
            if (tl != null) {
                if (r != null && tl.deletedAt >= r.m()) { api.delete(r.id); delRemote.incrementAndGet() }
                if (r != null && tl.deletedAt < r.m()) { applyRemote(u, r, null, c); base[u] = r.m(); details += "restored from Drive (edited after deletion here)"; return }
                api.put("t-$u", ByteArray(0), null, mapOf("m" to tl.deletedAt.toString()))
                base.remove(u)
                return
            }
            if (r != null) { applyRemote(u, r, null, c); base[u] = r.m() }
            return
        }
        // The note exists here and no tombstone anywhere.
        if (r == null) { uploadNote(l, null, c); base[u] = l.modifiedAt; return }
        val mL = l.modifiedAt
        val mR = r.m()
        if (mL == mR) { base[u] = mL; return }
        val b = base[u]
        val localChanged = b == null || mL != b
        val remoteChanged = b == null || mR != b
        when {
            localChanged && !remoteChanged -> { uploadNote(l, r.id, c); base[u] = mL }
            remoteChanged && !localChanged -> { applyRemote(u, r, l, c); base[u] = mR }
            else -> conflict(u, l, r, c)
        }
    }

    /** Both sides changed: the newest keeps the note; the other copy becomes "… (conflicto)", uploaded too. */
    private suspend fun conflict(u: String, l: Note, r: DriveFile, c: Ctx) {
        conflicts.incrementAndGet()
        val remoteNote = download(u, r, c)
        val loserIsLocal = l.modifiedAt < r.m() || (l.modifiedAt == r.m())
        val loser = if (loserIsLocal) l else remoteNote.note
        val copy = loser.copy(id = 0, uuid = newUuid(), title = (loser.title.ifBlank { "" } + " (conflicto)").trim(), modifiedAt = System.currentTimeMillis(), trashedAt = loser.trashedAt)
        val saved = dbLock.withLock {
            val copyId = dao.insert(copy)
            dao.setLabels(copyId, (if (loserIsLocal) c.labelsOf[l.id].orEmpty().filterNotNull() else remoteNote.labels).map { dao.labelIdFor(it) })
            dao.setModified(copyId, copy.modifiedAt)
            dao.byUuid(copy.uuid)!!
        }
        uploadNote(saved, null, c)
        base[saved.uuid] = saved.modifiedAt
        if (loserIsLocal) {
            applyParsed(remoteNote, l.id, c); base[u] = r.m()
        } else {
            uploadNote(l, r.id, c); base[u] = l.modifiedAt
        }
        details += "conflict on \"${l.title.take(30)}\": ${if (loserIsLocal) "Drive" else "this device"} copy wins, the other kept as (conflicto)"
    }

    // ---- Files ----

    private suspend fun uploadNote(n: Note, existingId: String?, c: Ctx) {
        val labels = c.labelsOf[n.id].orEmpty().filterNotNull()
        val place = n.placeId?.let(c.placeById::get)
        val json = NoteSerializer.toJson(n, n.content, labels, place).put("trashedAt", n.trashedAt ?: 0L)
        val name = "n-${n.uuid}"
        val blob = DriveCrypto.encrypt(dek, json.toString().toByteArray(), aad(name))
        // Images first, so another device never sees a note whose pictures are missing.
        for (img in imageNames(n.content).distinct()) {
            if (c.remoteImages.containsKey(img)) continue
            val f = images.file(img)
            if (!f.exists()) continue
            val iname = "i-$img"
            val id = api.put(iname, DriveCrypto.encrypt(dek, f.readBytes(), aad(iname)), null, emptyMap())
            c.remoteImages[img] = DriveFile(id, iname, "", f.length())
            imgUp.incrementAndGet()
        }
        api.put(name, blob, existingId, mapOf("m" to n.modifiedAt.toString()))
        up.incrementAndGet()
    }

    private suspend fun download(u: String, r: DriveFile, c: Ctx): NoteSerializer.Parsed {
        val name = "n-$u"
        val plain = DriveCrypto.decrypt(dek, api.get(r.id), aad(name))
        val o = JSONObject(String(plain))
        val parsed = NoteSerializer.fromJson(o)
        val trashed = o.optLong("trashedAt", 0L).takeIf { it > 0 }
        for (img in imageNames(parsed.note.content).distinct()) {
            if (images.file(img).exists()) continue
            val f = c.remoteImages[img] ?: continue
            val iname = "i-$img"
            images.file(img).writeBytes(DriveCrypto.decrypt(dek, api.get(f.id), aad(iname)))
            imgDown.incrementAndGet()
        }
        return NoteSerializer.Parsed(parsed.note.copy(trashedAt = trashed), parsed.labels, parsed.place)
    }

    private suspend fun applyRemote(u: String, r: DriveFile, local: Note?, c: Ctx) {
        applyParsed(download(u, r, c), local?.id, c)
        down.incrementAndGet()
    }

    private suspend fun applyParsed(p: NoteSerializer.Parsed, localId: Long?, c: Ctx) = dbLock.withLock {
        val note = p.note
        val id = dao.insert(if (localId != null) note.copy(id = localId) else note)
        dao.setLabels(id, p.labels.map { dao.labelIdFor(it) })
        p.place?.let { pl ->
            val placeId = dao.placeByName(pl.name)?.id ?: dao.insertPlace(Place(name = pl.name, latitude = pl.latitude, longitude = pl.longitude))
            dao.setPlace(id, placeId, pl.leave)
        }
        // The label and place calls stamp "now"; the note keeps the time it has everywhere else.
        dao.setModified(id, note.modifiedAt)
    }

    private companion object {
        const val BaseKey = "drive_base"
    }
}
