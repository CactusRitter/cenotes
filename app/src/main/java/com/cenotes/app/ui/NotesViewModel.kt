package com.cenotes.app.ui

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cenotes.app.CenotesApp
import com.cenotes.app.ExternalViews
import com.cenotes.app.R
import com.cenotes.app.data.ImageStore
import com.cenotes.app.data.Label
import com.cenotes.app.data.Note
import com.cenotes.app.data.NoteDao
import com.cenotes.app.ui.markdown.imageNames
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the main list shows: everything active, one label, or the archive. */
sealed interface Section {
    data object All : Section
    data object Archive : Section
    data class ByLabel(val labelId: Long) : Section
}

/** A note as shown in the list: with its labels. */
data class NoteItem(val note: Note, val labels: List<Label>)

/** A finished action the list offers to undo from a snackbar. */
data class UndoEvent(val id: Long, val message: String, val undo: suspend () -> Unit)

private data class Filter(
    val query: String = "",
    val pinnedOnly: Boolean = false,
    val section: Section = Section.All,
)

class NotesViewModel(private val app: CenotesApp) : ViewModel() {
    private val dao: NoteDao = app.database.noteDao()
    val images: ImageStore = app.images

    init {
        // Once per app start: drop image files that no note uses any more, and make sure the
        // widgets and launcher shortcuts show the current notes.
        viewModelScope.launch {
            val referenced = dao.allContents().flatMap { imageNames(it) }.toSet()
            images.cleanUp(referenced)
            ExternalViews.refresh(app)
        }
    }

    /**
     * Writes run in the app-wide scope (not the screen's), so a save started while leaving a
     * note always completes; afterwards widgets and shortcuts are refreshed.
     */
    private fun write(block: suspend () -> Unit) {
        app.appScope.launch {
            block()
            ExternalViews.refresh(app)
        }
    }

    /** Imports picked/captured pictures into the app; returns their file names (failures skipped). */
    suspend fun importImages(uris: List<Uri>): List<String> = uris.mapNotNull { images.import(it) }

    // ---- What is shown ----

    private val filter = MutableStateFlow(Filter())

    val labels: StateFlow<List<Label>> =
        dao.observeLabels().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val links = dao.observeLinks()

    /** Notes for the current section, search and "Fijadas" tab, each with its labels. */
    val notes: StateFlow<List<NoteItem>?> = combine(dao.observeAll(), links, labels, filter) { all, links, labels, f ->
        val labelById = labels.associateBy { it.id }
        val labelsOf = links.groupBy({ it.noteId }, { labelById[it.labelId] }).mapValues { (_, l) ->
            l.filterNotNull().sortedBy { it.name.lowercase() }
        }
        val term = f.query.trim()
        all.asSequence()
            .filter { n ->
                when (val s = f.section) {
                    Section.All -> !n.archived
                    Section.Archive -> n.archived
                    is Section.ByLabel -> !n.archived && labelsOf[n.id].orEmpty().any { it.id == s.labelId }
                }
            }
            .filter { !f.pinnedOnly || it.pinned }
            .filter { term.isEmpty() || it.title.contains(term, true) || it.content.contains(term, true) }
            .map { NoteItem(it, labelsOf[it.id].orEmpty()) }
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Number of active (not archived) notes per label, for the side menu. */
    val labelCounts: StateFlow<Map<Long, Int>> = combine(dao.observeAll(), links) { all, links ->
        val active = all.filterNot { it.archived }.map { it.id }.toHashSet()
        links.filter { it.noteId in active }.groupingBy { it.labelId }.eachCount()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val query: StateFlow<String> = filter.map { it.query }.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val pinnedOnly: StateFlow<Boolean> = filter.map { it.pinnedOnly }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val section: StateFlow<Section> = filter.map { it.section }.stateIn(viewModelScope, SharingStarted.Eagerly, Section.All)

    fun setQuery(value: String) = filter.update { it.copy(query = value) }
    fun setPinnedOnly(value: Boolean) = filter.update { it.copy(pinnedOnly = value) }

    fun showSection(section: Section) {
        filter.update { it.copy(section = section, pinnedOnly = false) }
        clearSelection()
    }

    // ---- Selection (bulk actions) ----

    private val _selected = MutableStateFlow<Set<Long>>(emptySet())
    val selected: StateFlow<Set<Long>> = _selected.asStateFlow()

    fun toggleSelected(id: Long) = _selected.update { if (id in it) it - id else it + id }
    fun clearSelection() { _selected.value = emptySet() }

    private fun takeSelection(): Set<Long> = _selected.value.also { clearSelection() }

    // ---- Undo ----

    private val _undo = MutableStateFlow<UndoEvent?>(null)
    val undo: StateFlow<UndoEvent?> = _undo.asStateFlow()
    private var undoCounter = 0L

    private fun offerUndo(@StringRes one: Int, @StringRes many: Int, count: Int, undo: suspend () -> Unit) {
        val message = if (count == 1) app.getString(one) else app.getString(many, count)
        _undo.value = UndoEvent(++undoCounter, message, undo)
    }

    fun performUndo(event: UndoEvent) {
        if (_undo.value?.id == event.id) _undo.value = null
        write { event.undo() }
    }

    fun dismissUndo(event: UndoEvent) {
        if (_undo.value?.id == event.id) _undo.value = null
    }

    // ---- Single note (editor) ----

    suspend fun load(id: Long): Note? = dao.get(id)
    suspend fun labelIdsOf(noteId: Long): List<Long> = dao.labelIdsOf(noteId)

    /** The label a new note starts with: the one being viewed, if any. */
    fun labelForNewNote(): Long? = (filter.value.section as? Section.ByLabel)?.labelId

    /**
     * Persists the editor's content. An empty note is not stored (and an existing one that was
     * emptied is deleted). The edit time only moves when the text actually changed.
     */
    fun save(
        original: Note?,
        title: String,
        content: String,
        pinned: Boolean,
        labelIds: List<Long>,
        archived: Boolean,
    ) = write {
        if (title.isBlank() && content.isBlank()) {
            original?.let { dao.deleteNotes(listOf(it.id)) }
            return@write
        }
        if (original == null) {
            val id = dao.insert(Note(title = title, content = content, pinned = pinned, archived = archived))
            dao.setLabels(id, labelIds)
            return@write
        }
        val textChanged = original.title != title || original.content != content
        if (textChanged || original.pinned != pinned || original.archived != archived) {
            dao.update(
                original.copy(
                    title = title,
                    content = content,
                    pinned = pinned,
                    archived = archived,
                    updatedAt = if (textChanged) System.currentTimeMillis() else original.updatedAt,
                ),
            )
        }
        if (dao.labelIdsOf(original.id).toSet() != labelIds.toSet()) dao.setLabels(original.id, labelIds)
    }

    /** Archiving from the editor: the list then offers "Deshacer". */
    fun noteArchived(noteId: Long, archived: Boolean) {
        if (noteId == 0L) return
        val (one, many) = if (archived) R.string.archived_one to R.string.archived_many
        else R.string.unarchived_one to R.string.unarchived_many
        offerUndo(one, many, 1) { dao.setArchived(listOf(noteId), !archived) }
    }

    /** Deletes one note (from the editor), with undo. */
    fun delete(note: Note) = deleteIds(setOf(note.id))

    // ---- Bulk actions on the selection ----

    fun archiveSelected(archive: Boolean) {
        val ids = takeSelection()
        if (ids.isEmpty()) return
        write { dao.setArchived(ids, archive) }
        val (one, many) = if (archive) R.string.archived_one to R.string.archived_many
        else R.string.unarchived_one to R.string.unarchived_many
        offerUndo(one, many, ids.size) { dao.setArchived(ids, !archive) }
    }

    fun deleteSelected() = deleteIds(takeSelection())

    private fun deleteIds(ids: Set<Long>) {
        if (ids.isEmpty()) return
        write {
            val notes = dao.getMany(ids)
            val links = dao.links().filter { it.noteId in ids }
            dao.deleteNotes(ids)
            offerUndo(R.string.note_deleted, R.string.notes_deleted, notes.size) {
                notes.forEach { dao.insert(it) }
                dao.insertLinks(links)
            }
        }
    }

    /** Pins all selected notes, or unpins them if they all were pinned already. */
    fun pinSelected() {
        val ids = takeSelection()
        if (ids.isEmpty()) return
        write {
            val allPinned = dao.getMany(ids).all { it.pinned }
            dao.setPinned(ids, !allPinned)
        }
    }

    fun togglePin(note: Note) = write { dao.update(note.copy(pinned = !note.pinned)) }

    /** Adds [labelId] to all the selected notes, or removes it ([add] = false). Keeps the selection. */
    fun setLabelOnSelected(labelId: Long, add: Boolean) {
        val ids = _selected.value
        if (ids.isEmpty()) return
        write { if (add) dao.addLabel(ids, labelId) else dao.removeLabel(ids, labelId) }
    }

    /** For the bulk label dialog: how many of the selected notes have each label. */
    fun selectedLabelCounts(items: List<NoteItem>): Map<Long, Int> {
        val ids = _selected.value
        return items.filter { it.note.id in ids }.flatMap { it.labels }.groupingBy { it.id }.eachCount()
    }

    // ---- Labels ----

    suspend fun createLabel(name: String): Long? {
        val clean = name.trim()
        if (clean.isEmpty()) return null
        return dao.labelIdFor(clean)
    }

    fun renameLabel(id: Long, name: String) {
        val clean = name.trim()
        if (clean.isNotEmpty()) write { runCatching { dao.renameLabel(id, clean) } }
    }

    fun deleteLabel(id: Long) {
        write { dao.deleteLabel(id) }
        if ((filter.value.section as? Section.ByLabel)?.labelId == id) showSection(Section.All)
    }

    /** After an import: widgets and shortcuts need to show the new notes. */
    fun refreshExternalViews() = write { }

    companion object {
        val Factory = viewModelFactory {
            initializer { NotesViewModel(this[APPLICATION_KEY] as CenotesApp) }
        }
    }
}

