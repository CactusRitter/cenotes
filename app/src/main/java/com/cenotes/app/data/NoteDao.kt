package com.cenotes.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class NoteDao {
    // ---- Notes ----

    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC")
    abstract fun observeAll(): Flow<List<Note>>

    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC")
    abstract suspend fun all(): List<Note>

    @Query("SELECT content FROM notes")
    abstract suspend fun allContents(): List<String>

    @Query("SELECT * FROM notes WHERE id = :id")
    abstract suspend fun get(id: Long): Note?

    @Query("SELECT * FROM notes WHERE id IN (:ids)")
    abstract suspend fun getMany(ids: Collection<Long>): List<Note>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(note: Note): Long

    @Update
    abstract suspend fun update(note: Note)

    @Query("UPDATE notes SET archived = :archived WHERE id IN (:ids)")
    abstract suspend fun setArchived(ids: Collection<Long>, archived: Boolean)

    @Query("UPDATE notes SET pinned = :pinned WHERE id IN (:ids)")
    abstract suspend fun setPinned(ids: Collection<Long>, pinned: Boolean)

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    protected abstract suspend fun deleteNotesOnly(ids: Collection<Long>)

    /** Deletes notes together with their label links. */
    @Transaction
    open suspend fun deleteNotes(ids: Collection<Long>) {
        deleteLinksOfNotes(ids)
        deleteNotesOnly(ids)
    }

    // ---- Labels ----

    @Query("SELECT * FROM labels ORDER BY name COLLATE NOCASE")
    abstract fun observeLabels(): Flow<List<Label>>

    @Query("SELECT * FROM labels ORDER BY name COLLATE NOCASE")
    abstract suspend fun labels(): List<Label>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertLabel(label: Label): Long

    @Query("SELECT * FROM labels WHERE name = :name COLLATE NOCASE LIMIT 1")
    abstract suspend fun labelByName(name: String): Label?

    @Query("UPDATE labels SET name = :name WHERE id = :id")
    abstract suspend fun renameLabel(id: Long, name: String)

    @Query("DELETE FROM labels WHERE id = :id")
    protected abstract suspend fun deleteLabelOnly(id: Long)

    @Query("DELETE FROM note_labels WHERE labelId = :labelId")
    protected abstract suspend fun deleteLinksOfLabel(labelId: Long)

    /** Removes a label everywhere; the notes themselves stay. */
    @Transaction
    open suspend fun deleteLabel(id: Long) {
        deleteLinksOfLabel(id)
        deleteLabelOnly(id)
    }

    /** Returns the id of the label with this name, creating it if needed. */
    @Transaction
    open suspend fun labelIdFor(name: String): Long {
        labelByName(name)?.let { return it.id }
        val id = insertLabel(Label(name = name))
        return if (id > 0) id else labelByName(name)!!.id
    }

    // ---- Note ↔ label links ----

    @Query("SELECT * FROM note_labels")
    abstract fun observeLinks(): Flow<List<NoteLabel>>

    @Query("SELECT * FROM note_labels")
    abstract suspend fun links(): List<NoteLabel>

    @Query("SELECT labelId FROM note_labels WHERE noteId = :noteId")
    abstract suspend fun labelIdsOf(noteId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertLinks(links: List<NoteLabel>)

    @Query("DELETE FROM note_labels WHERE noteId IN (:noteIds)")
    protected abstract suspend fun deleteLinksOfNotes(noteIds: Collection<Long>)

    @Query("DELETE FROM note_labels WHERE labelId = :labelId AND noteId IN (:noteIds)")
    abstract suspend fun removeLabel(noteIds: Collection<Long>, labelId: Long)

    @Transaction
    open suspend fun setLabels(noteId: Long, labelIds: Collection<Long>) {
        deleteLinksOfNotes(listOf(noteId))
        insertLinks(labelIds.map { NoteLabel(noteId, it) })
    }

    suspend fun addLabel(noteIds: Collection<Long>, labelId: Long) =
        insertLinks(noteIds.map { NoteLabel(it, labelId) })
}
