package com.cenotes.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val pinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    /** Archived notes leave the main list (and widgets) but keep their labels. */
    @ColumnInfo(defaultValue = "0") val archived: Boolean = false,
    /** When the note went to the Trash (null = not in it). Notes there for over [TrashDays] days are deleted for good. */
    val trashedAt: Long? = null,
    /** Background colour of the note (ARGB, one of the label presets); 0 = none. */
    @ColumnInfo(defaultValue = "0") val color: Int = 0,
    /** When the note was first saved (for "sort by date created"). */
    @ColumnInfo(defaultValue = "0") val createdAt: Long = System.currentTimeMillis(),
    /** When the note reminds you (a notification), or null. A repeating reminder moves on to its next time once shown. */
    val reminderAt: Long? = null,
    /** How the reminder repeats: [Repeat] ordinal (0 = once). */
    @ColumnInfo(defaultValue = "0") val reminderRepeat: Int = 0,
)

/** Days a note stays in the Trash before it is deleted for good (like Google Keep). */
const val TrashDays = 7

@Entity(tableName = "labels", indices = [Index(value = ["name"], unique = true)])
data class Label(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** ARGB colour chosen by the user; 0 = automatic (taken from the accent palette). */
    @ColumnInfo(defaultValue = "0") val color: Int = 0,
)

/** A note can have any number of labels. */
@Entity(tableName = "note_labels", primaryKeys = ["noteId", "labelId"], indices = [Index("labelId")])
data class NoteLabel(val noteId: Long, val labelId: Long)

/** How a reminder repeats (stored as its ordinal in [Note.reminderRepeat]; the order must not change). */
enum class Repeat {
    NONE, DAILY, WEEKLY, MONTHLY;

    /** The next time after [at] (and after now), or null for a reminder that does not repeat. */
    fun next(at: Long, now: Long = System.currentTimeMillis()): Long? {
        if (this == NONE) return null
        val c = java.util.Calendar.getInstance().apply { timeInMillis = at }
        do {
            when (this) {
                DAILY -> c.add(java.util.Calendar.DAY_OF_YEAR, 1)
                WEEKLY -> c.add(java.util.Calendar.WEEK_OF_YEAR, 1)
                MONTHLY -> c.add(java.util.Calendar.MONTH, 1)
                NONE -> Unit
            }
        } while (c.timeInMillis <= now)
        return c.timeInMillis
    }

    companion object {
        fun of(value: Int): Repeat = entries.getOrElse(value) { NONE }
    }
}

/** A note's reminder as the editor holds it: when ([at], null = none) and how it repeats. */
data class Reminder(val at: Long? = null, val repeat: Repeat = Repeat.NONE) {
    companion object {
        fun of(note: Note) = Reminder(note.reminderAt, Repeat.of(note.reminderRepeat))
    }
}
