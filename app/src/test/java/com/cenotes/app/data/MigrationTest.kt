package com.cenotes.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.zip.ZipFile

/**
 * Every old database (versions 1 to 8) opens in today's app with nothing lost, using César's real notes when his
 * backup is on this computer and a hand-made set otherwise.
 *
 * The real notes are read at test time and never copied into the repository (it is public). They come from
 * `CENOTES_REAL_BACKUP` (a backup .zip) or, if that is not set, the newest .zip in `G:\Mi unidad\Ce Apps\Cenotes\Copias`.
 *
 * An old database is built as version 1 (the schema Room made for the first `Note`) and then taken up with the app's own
 * migrations. That is the same schema a fresh install of that version made: every column added later came with its
 * `@ColumnInfo(defaultValue)` in the same commit as its migration (checked in the git history of `Note.kt`). Opening it
 * with Room checks the final schema against the entities, so a migration that leaves any difference fails here.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val opened = ArrayList<NotesDatabase>()

    @After
    fun close() = opened.forEach { it.close() }

    @Test
    fun everyOldVersionMigratesWithNothingLost() {
        val fixture = fixture()
        println("MigrationTest: ${fixture.source}, ${fixture.notes.size} notes, ${fixture.labels.size} labels")
        for (version in 1..8) {
            val name = "v$version.db"
            createAt(version, name, fixture)
            val db = open(name)
            check(version, fixture, db)
            // Opening it a second time (already at 9) must not touch anything.
            db.close()
            check(version, fixture, open(name))
        }
    }

    @Test
    fun realBackupRestoresAndRoundTrips() = runBlocking {
        val fixture = fixture()
        val zip = fixture.zip ?: return@runBlocking // only with a real backup
        val first = backup(open("restore1.db"))
        val r = first.import(Uri.fromFile(zip), notes = true, settings = false)
        val nonEmpty = fixture.notes.take(fixture.realCount).count { it.title.isNotBlank() || it.content.isNotBlank() }
        assertEquals("added", nonEmpty, r.added)
        // Restoring the same backup again changes nothing (same uuids, same modifiedAt).
        val again = first.import(Uri.fromFile(zip), notes = true, settings = false)
        assertEquals("added again", 0, again.added)
        assertEquals("updated again", 0, again.updated)

        // A backup made now restores into an empty database with the same notes.
        val out = File(context.cacheDir, "roundtrip.zip")
        first.export(Uri.fromFile(out), includeNotes = true, includeImages = true, includeSettings = false)
        val secondDb = open("restore2.db")
        val r2 = backup(secondDb).import(Uri.fromFile(out), notes = true, settings = false)
        assertEquals("round trip", nonEmpty, r2.added)
        val a = snapshot(opened[0])
        val b = snapshot(secondDb)
        val diff = a.zip(b).filter { (x, y) -> x != y }.map { (x, y) -> x.split("|").zip(y.split("|")).withIndex().filter { it.value.first != it.value.second }.map { "field ${it.index}: ${it.value.first.take(40)} -> ${it.value.second.take(40)}" } }
        assertEquals("round trip differences", emptyList<List<String>>(), diff.take(3))
        assertEquals(a.size, b.size)
    }

    // ---- Building an old database ----

    private fun createAt(version: Int, name: String, f: Fixture) {
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) = db.execSQL(V1Notes)
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build(),
        )
        val db = helper.writableDatabase
        NotesDatabase.AllMigrations.filter { it.endVersion <= version }.forEach { it.migrate(db) }
        db.version = version
        val noteCols = columns(db, "notes")
        f.notes.forEachIndexed { i, n ->
            val cv = ContentValues().apply {
                put("id", i + 1L); put("title", n.title); put("content", n.content); put("pinned", n.pinned); put("updatedAt", n.updatedAt)
                put("archived", n.archived); put("trashedAt", n.trashedAt); put("color", n.color); put("createdAt", n.createdAt)
                put("reminderAt", n.reminderAt); put("reminderRepeat", n.reminderRepeat); put("locked", n.locked); put("position", n.position)
                if (n.place != null && version >= 8) { put("placeId", f.places.indexOf(n.place) + 1L); put("placeLeave", n.placeLeave) }
            }
            cv.keySet().toList().forEach { if (it !in noteCols) cv.remove(it) }
            db.insert("notes", SQLiteDatabase.CONFLICT_ABORT, cv)
        }
        if (version >= 2) {
            val labelCols = columns(db, "labels")
            f.labels.forEachIndexed { i, (label, color) ->
                db.insert("labels", SQLiteDatabase.CONFLICT_ABORT, ContentValues().apply {
                    put("id", i + 1L); put("name", label); if ("color" in labelCols) put("color", color)
                })
            }
            f.notes.forEachIndexed { i, n ->
                n.labels.forEach { l ->
                    db.insert("note_labels", SQLiteDatabase.CONFLICT_ABORT, ContentValues().apply {
                        put("noteId", i + 1L); put("labelId", f.labels.indexOfFirst { it.first == l } + 1L)
                    })
                }
            }
        }
        if (version >= 7) f.notes.forEachIndexed { i, n ->
            n.versions.forEach { (text, at) ->
                db.insert("note_versions", SQLiteDatabase.CONFLICT_ABORT, ContentValues().apply {
                    put("noteId", i + 1L); put("title", n.title); put("content", text); put("savedAt", at)
                })
            }
        }
        if (version >= 8) f.places.forEachIndexed { i, p ->
            db.insert("places", SQLiteDatabase.CONFLICT_ABORT, ContentValues().apply {
                put("id", i + 1L); put("name", p); put("latitude", 40.4 + i); put("longitude", -3.7 - i)
            })
        }
        helper.close()
    }

    private fun columns(db: SupportSQLiteDatabase, table: String) =
        db.query("PRAGMA table_info(`$table`)").use { c -> buildSet { while (c.moveToNext()) add(c.getString(1)) } }

    private fun open(name: String) = Room.databaseBuilder(context, NotesDatabase::class.java, name)
        .addMigrations(*NotesDatabase.AllMigrations)
        .allowMainThreadQueries()
        .build().also { opened += it }

    private fun backup(db: NotesDatabase) = Backup(context, db.noteDao(), ImageStore(context), SettingsRepository(context))

    // ---- Checking the result ----

    private fun check(version: Int, f: Fixture, room: NotesDatabase) = runBlocking {
        val db = room.openHelper.writableDatabase
        assertEquals("v$version user_version", 9, db.version)
        val dao = room.noteDao()
        val notes = dao.getMany((1L..f.notes.size).toList()).associateBy { it.id }
        assertEquals("v$version notes", f.notes.size, notes.size)
        assertEquals("v$version uuids", f.notes.size, notes.values.map { it.uuid }.filter { it.isNotEmpty() }.toSet().size)
        f.notes.forEachIndexed { i, e ->
            val n = notes.getValue(i + 1L)
            val at = "v$version note ${i + 1}"
            assertEquals("$at title", e.title, n.title)
            assertEquals("$at content", e.content, n.content)
            assertEquals("$at pinned", e.pinned, n.pinned)
            assertEquals("$at updatedAt", e.updatedAt, n.updatedAt)
            assertEquals("$at modifiedAt", e.updatedAt, n.modifiedAt)
            assertEquals("$at archived", version >= 2 && e.archived, n.archived)
            assertEquals("$at trashedAt", if (version >= 4) e.trashedAt else null, n.trashedAt)
            assertEquals("$at color", if (version >= 4) e.color else 0, n.color)
            assertEquals("$at createdAt", if (version >= 4) e.createdAt else e.updatedAt, n.createdAt)
            assertEquals("$at reminderAt", if (version >= 5) e.reminderAt else null, n.reminderAt)
            assertEquals("$at reminderRepeat", if (version >= 5) e.reminderRepeat else 0, n.reminderRepeat)
            assertEquals("$at locked", version >= 6 && e.locked, n.locked)
            assertEquals("$at position", if (version >= 7) e.position else e.updatedAt, n.position)
            assertEquals("$at placeId", if (version >= 8 && e.place != null) f.places.indexOf(e.place) + 1L else null, n.placeId)
            assertEquals("$at labels", if (version >= 2) e.labels.toSet() else emptySet(), labelNames(room, n.id).toSet())
            assertEquals("$at versions", if (version >= 7) e.versions.map { it.first } else emptyList(),
                db.query("SELECT content FROM note_versions WHERE noteId = ${n.id} ORDER BY savedAt").use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } })
        }
        if (version >= 3) f.labels.forEach { (l, color) ->
            assertEquals("v$version label colour $l", color, dao.labels().single { it.name == l }.color)
        }
        assertEquals("v$version tombstones", 0, dao.deletedNotes().size)
    }

    /** What a database holds, without ids or uuids-independent clocks that a restore may renumber. */
    private suspend fun snapshot(db: NotesDatabase): List<String> {
        val dao = db.noteDao()
        val all = db.openHelper.readableDatabase.query("SELECT id FROM notes").use { c -> buildList { while (c.moveToNext()) add(c.getLong(0)) } }
        return dao.getMany(all).map { n ->
            listOf(n.uuid, n.title, n.content, n.pinned, n.archived, n.trashedAt, n.color, n.createdAt, n.updatedAt, n.modifiedAt,
                n.reminderAt, n.reminderRepeat, n.locked, n.position, labelNames(db, n.id).sorted()).joinToString("|")
        }.sorted()
    }

    // ---- Test data ----

    private data class FNote(
        val title: String, val content: String, val pinned: Boolean = false, val updatedAt: Long, val archived: Boolean = false,
        val trashedAt: Long? = null, val color: Int = 0, val createdAt: Long = updatedAt - 1000, val reminderAt: Long? = null,
        val reminderRepeat: Int = 0, val locked: Boolean = false, val position: Long = updatedAt + 7, val labels: List<String> = emptyList(),
        val versions: List<Pair<String, Long>> = emptyList(), val place: String? = null, val placeLeave: Boolean = false,
    )

    private class Fixture(val source: String, val notes: List<FNote>, val realCount: Int, val labels: List<Pair<String, Int>>, val places: List<String>, val zip: File?)

    private fun fixture(): Fixture {
        val zip = realBackup()
        val real = ArrayList<FNote>()
        val labels = LinkedHashMap<String, Int>()
        if (zip != null) {
            val root = ZipFile(zip).use { z -> JSONObject(z.getInputStream(z.getEntry("notes.json")).readBytes().toString(Charsets.UTF_8)) }
            val names = root.optJSONArray("labels")
            for (i in 0 until (names?.length() ?: 0)) labels[names!!.getString(i)] = root.optJSONObject("labelColors")?.optInt(names.getString(i), 0) ?: 0
            val list = root.getJSONArray("notes")
            for (i in 0 until list.length()) {
                val o = list.getJSONObject(i)
                val ls = o.optJSONArray("labels")?.let { a -> List(a.length()) { a.getString(it) } }.orEmpty()
                ls.forEach { labels.putIfAbsent(it, 0) }
                real += FNote(
                    title = o.getString("title"), content = o.getString("content"), pinned = o.optBoolean("pinned"),
                    updatedAt = o.getLong("updatedAt"), archived = o.optBoolean("archived"), color = o.optInt("color"),
                    createdAt = o.optLong("createdAt"), reminderAt = o.optLong("reminderAt").takeIf { it > 0 },
                    reminderRepeat = o.optInt("reminderRepeat"), locked = o.optBoolean("locked"), position = o.optLong("position"), labels = ls,
                )
            }
        }
        // Always added: what the real notes may not have (Trash, reminders, places, history, colours, odd text).
        val t = 1_790_000_000_000L
        labels.putIfAbsent("Prueba ✓", 0xFF2E7D32.toInt())
        labels["Prueba ✓"] = 0xFF2E7D32.toInt()
        labels.putIfAbsent("Ünïcödé", 0)
        val extra = listOf(
            FNote("", "Sin título\n- [ ] uno\n- [x] dos\n<!--fold-->\n  - [ ] dentro", updatedAt = t, labels = listOf("Prueba ✓")),
            FNote("Papelera", "en la papelera", updatedAt = t + 1, trashedAt = t + 50, color = 0xFFFFCC80.toInt()),
            FNote("Recordatorio 🔔", "cada semana", updatedAt = t + 2, reminderAt = t + 86_400_000, reminderRepeat = 2, pinned = true),
            FNote("Protegida", "secreto 'con' \"comillas\" y \\ barras", updatedAt = t + 3, locked = true, archived = true, labels = listOf("Ünïcödé", "Prueba ✓")),
            FNote("Con historial", "versión 3", updatedAt = t + 4, versions = listOf("versión 1" to t - 20, "versión 2" to t - 10)),
            FNote("En el súper", "leche", updatedAt = t + 5, place = "Supermercado", placeLeave = true),
            FNote("Larga", "x".repeat(200_000), updatedAt = t + 6),
        )
        return Fixture(
            source = zip?.let { "real backup ${it.name}" } ?: "no real backup found, hand-made notes only",
            notes = real + extra, realCount = real.size, labels = labels.toList(), places = listOf("Supermercado", "Casa"), zip = zip,
        )
    }

    private fun realBackup(): File? {
        System.getenv("CENOTES_REAL_BACKUP")?.let { return File(it).takeIf(File::isFile) }
        return File("G:/Mi unidad/Ce Apps/Cenotes/Copias").listFiles { f -> f.name.endsWith(".zip") }
            ?.filter { f -> runCatching { ZipFile(f).use { it.getEntry("notes.json") != null } }.getOrDefault(false) }
            ?.maxByOrNull { it.lastModified() }
    }

    private companion object {
        /** The notes table exactly as Room created it in version 1 (the first `Note`: id, title, content, pinned, updatedAt). */
        const val V1Notes = "CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, " +
            "`content` TEXT NOT NULL, `pinned` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
    }
}

private fun labelNames(db: NotesDatabase, noteId: Long): List<String> =
    db.openHelper.readableDatabase.query("SELECT l.name FROM labels l JOIN note_labels nl ON nl.labelId = l.id WHERE nl.noteId = $noteId")
        .use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }
