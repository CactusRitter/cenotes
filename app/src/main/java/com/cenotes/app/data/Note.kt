package com.cenotes.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "notes", indices = [Index(value = ["uuid"], unique = true)])
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
    /** In "Protected": hidden everywhere else and opened only after the phone's fingerprint, face or PIN. */
    @ColumnInfo(defaultValue = "0") val locked: Boolean = false,
    /** Place in the list when notes are sorted by hand: higher first. New notes get the time, so they go on top. */
    @ColumnInfo(defaultValue = "0") val position: Long = System.currentTimeMillis(),
    /** Reminds on arriving at (or leaving, with [placeLeave]) this saved [Place]; null = no place reminder. */
    val placeId: Long? = null,
    @ColumnInfo(defaultValue = "0") val placeLeave: Boolean = false,
    /** Stable identity of the note across devices and backups (random UUID, never changes). */
    @ColumnInfo(defaultValue = "") val uuid: String = newUuid(),
    /** Last change of anything in the note (text, flags, colour, labels, Trash, order); the clock for sync and conflicts. [updatedAt] stays "last text edit" and only orders the list. */
    @ColumnInfo(defaultValue = "0") val modifiedAt: Long = System.currentTimeMillis(),
)

/**
 * A place a note can remind you at (the supermarket, home…), saved with a name to use again. It is found on the device
 * itself (an address typed in, or "I'm here"); Cenotes never sends it anywhere.
 */
@Entity(tableName = "places")
data class Place(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

/** How near a place counts as being there, in metres (Android's geofences work well from about 100 m). */
const val PlaceRadius = 150f

/** Days a note stays in the Trash before it is deleted for good (like Google Keep). */
const val TrashDays = 7

/** How many earlier versions of a note are kept. */
const val MaxVersions = 20

/**
 * An earlier text of a note (⋮ → History): saved each time the note is closed after its text changed, with the time
 * that text was written. Only the [MaxVersions] latest are kept; they go when the note is deleted for good.
 */
@Entity(tableName = "note_versions", indices = [Index("noteId")])
data class NoteVersion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val title: String,
    val content: String,
    val savedAt: Long,
)

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

/**
 * A note's reminders as the editor holds them: when ([at], null = none) and how it repeats, and the place ([placeId],
 * null = none) with whether it reminds on leaving it ([placeLeave]) rather than arriving.
 */
data class Reminder(
    val at: Long? = null,
    val repeat: Repeat = Repeat.NONE,
    val placeId: Long? = null,
    val placeLeave: Boolean = false,
) {
    companion object {
        fun of(note: Note) = Reminder(note.reminderAt, Repeat.of(note.reminderRepeat), note.placeId, note.placeLeave)
    }
}

fun newUuid(): String = java.util.UUID.randomUUID().toString()

/** Days a tombstone is kept. Decision to review if sync arrives: dropping it too soon could resurrect a note from a device that has not synced for months. */
const val TombstoneDays = 90

/** A note deleted for good (not just in the Trash): remembered by its [uuid] so it does not come back from a backup or another device. */
@Entity(tableName = "deleted_notes")
data class DeletedNote(
    @PrimaryKey val uuid: String,
    val deletedAt: Long,
)
