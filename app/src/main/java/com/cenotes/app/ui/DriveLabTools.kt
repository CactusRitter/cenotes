package com.cenotes.app.ui

import com.cenotes.app.CenotesApp
import com.cenotes.app.data.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// EXPERIMENT (branch experimento-drive): test notes made on this device, to try the sync with real devices.

private fun device() = android.os.Build.MODEL

internal suspend fun labSeed(app: CenotesApp): String = withContext(Dispatchers.IO) {
    val dao = app.database.noteDao()
    val img = "img_lab_${System.currentTimeMillis()}.jpg"
    val bmp = android.graphics.Bitmap.createBitmap(1000, 750, android.graphics.Bitmap.Config.ARGB_8888)
    val rnd = java.util.Random(7)
    val px = IntArray(1000 * 750) { android.graphics.Color.rgb(rnd.nextInt(256), (it % 1000) / 4, (it / 1000) / 3) }
    bmp.setPixels(px, 0, 1000, 0, 0, 1000, 750)
    app.images.file(img).outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, it) }
    val size = app.images.file(img).length()
    val now = System.currentTimeMillis()
    val notes = listOf(
        "Lab A" to "Plain note A, made on ${device()}",
        "Lab B" to "- [ ] milk\n- [x] bread\n- [ ] eggs",
        "Lab C" to "![]($img)\nA note with a picture",
        "Lab D" to "Note D, second line\nthird line",
        "Lab E" to "Note E",
    )
    notes.forEachIndexed { i, (t, c) ->
        dao.insert(Note(title = t, content = c, updatedAt = now + i, createdAt = now + i, modifiedAt = now + i, position = now + i))
    }
    "Seeded 5 notes (C has a ${size / 1024} KB image)."
}

internal suspend fun labNew(app: CenotesApp): String = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    app.database.noteDao().insert(
        Note(title = "Lab new ${device()} ${now % 100000}", content = "created on ${device()}", updatedAt = now, createdAt = now, modifiedAt = now, position = now),
    )
    "Created a new note on ${device()}."
}

private suspend fun find(app: CenotesApp, letter: String) =
    app.database.noteDao().allForSync().firstOrNull { it.title.startsWith("Lab $letter") }

internal suspend fun labEdit(app: CenotesApp, letter: String): String = withContext(Dispatchers.IO) {
    val n = find(app, letter) ?: return@withContext "No note 'Lab $letter' here."
    val now = System.currentTimeMillis()
    app.database.noteDao().update(n.copy(content = n.content + "\nedit by ${device()} @${now % 100000}", updatedAt = now))
    "Edited '${n.title}' on ${device()}."
}

internal suspend fun labTrash(app: CenotesApp, letter: String): String = withContext(Dispatchers.IO) {
    val n = find(app, letter) ?: return@withContext "No note 'Lab $letter' here."
    app.database.noteDao().setTrashed(listOf(n.id), System.currentTimeMillis())
    "Moved '${n.title}' to the Trash."
}

internal suspend fun labDelete(app: CenotesApp, letter: String): String = withContext(Dispatchers.IO) {
    val n = find(app, letter) ?: return@withContext "No note 'Lab $letter' here."
    app.database.noteDao().deleteNotes(listOf(n.id))
    "Deleted '${n.title}' for good."
}

internal suspend fun labShow(app: CenotesApp): String = withContext(Dispatchers.IO) {
    val l = app.database.noteDao().allForSync().filter { it.title.startsWith("Lab") }.sortedBy { it.title }
    "${l.size} lab notes:\n" + l.joinToString("\n") {
        "· ${it.title}${if (it.trashedAt != null) " [trash]" else ""} (m=${it.modifiedAt % 100000}): ${it.content.replace("\n", " / ").takeLast(70)}"
    }
}
