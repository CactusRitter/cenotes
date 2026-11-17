package com.cenotes.app.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * A note as JSON: the one place that knows the note's file format. The backup uses it now, and a future sync can use it
 * for the per-note files, so both always agree. Labels and the place go by name (their ids are local to each phone).
 */
object NoteSerializer {

    data class PlaceRef(val name: String, val latitude: Double, val longitude: Double, val leave: Boolean)

    /** A note read from JSON (id 0, to be inserted), with the label names and place it refers to. [hasUuid] is false for old backups. */
    data class Parsed(val note: Note, val hasUuid: Boolean, val labels: List<String>, val place: PlaceRef?)

    fun toJson(note: Note, content: String, labelNames: List<String>, place: Place?): JSONObject =
        JSONObject()
            .put("uuid", note.uuid)
            .put("title", note.title)
            .put("content", content)
            .put("pinned", note.pinned)
            .put("archived", note.archived)
            .put("updatedAt", note.updatedAt)
            .put("modifiedAt", note.modifiedAt)
            .put("createdAt", note.createdAt)
            .put("color", note.color)
            .put("reminderAt", note.reminderAt ?: 0L)
            .put("reminderRepeat", note.reminderRepeat)
            .put("locked", note.locked)
            .put("position", note.position)
            .also { o ->
                if (place != null) o.put("place", JSONObject().put("name", place.name).put("latitude", place.latitude).put("longitude", place.longitude).put("leave", note.placeLeave))
            }
            .put("labels", JSONArray(labelNames))

    fun fromJson(o: JSONObject): Parsed {
        val now = System.currentTimeMillis()
        val updatedAt = o.optLong("updatedAt", now)
        val uuid = o.optString("uuid").trim()
        val note = Note(
            title = o.optString("title"),
            content = o.optString("content"),
            pinned = o.optBoolean("pinned"),
            archived = o.optBoolean("archived"),
            updatedAt = updatedAt,
            createdAt = o.optLong("createdAt", updatedAt),
            color = o.optInt("color", 0),
            reminderAt = o.optLong("reminderAt", 0L).takeIf { it > 0L },
            reminderRepeat = o.optInt("reminderRepeat", 0),
            locked = o.optBoolean("locked"),
            position = o.optLong("position", updatedAt),
            uuid = uuid.ifEmpty { newUuid() },
            // Formats before 9 only know the last text edit.
            modifiedAt = o.optLong("modifiedAt", updatedAt),
        )
        val labels = o.optJSONArray("labels")?.let { names ->
            (0 until names.length()).mapNotNull { names.optString(it).trim().takeIf(String::isNotEmpty) }
        }.orEmpty()
        val place = o.optJSONObject("place")?.let { p ->
            val name = p.optString("name").trim()
            if (name.isNotEmpty() && p.has("latitude") && p.has("longitude")) PlaceRef(name, p.getDouble("latitude"), p.getDouble("longitude"), p.optBoolean("leave")) else null
        }
        return Parsed(note, uuid.isNotEmpty(), labels, place)
    }
}
