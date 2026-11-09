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
)

@Entity(tableName = "labels", indices = [Index(value = ["name"], unique = true)])
data class Label(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

/** A note can have any number of labels. */
@Entity(tableName = "note_labels", primaryKeys = ["noteId", "labelId"], indices = [Index("labelId")])
data class NoteLabel(val noteId: Long, val labelId: Long)
