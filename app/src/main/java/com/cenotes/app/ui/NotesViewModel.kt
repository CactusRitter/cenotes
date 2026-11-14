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
import com.cenotes.app.data.NoteSort
import com.cenotes.app.data.NoteVersion
import com.cenotes.app.data.Reminder
import com.cenotes.app.data.TrashDays
import com.cenotes.app.ui.markdown.imageNames
import com.cenotes.app.ui.markdown.markdownToPlainText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the main list shows: everything active, one label, the archive or the Trash. */
sealed interface Section {
    data object All : Section
    data object Archive : Section
    data object Trash : Section
    data object Reminders : Section
    /** Protected notes: hidden everywhere else, shown only once unlocked with the phone's fingerprint, face or PIN. */
    data object Locked : Section
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
            // Notes that have been in the Trash for longer than a week go for good (like Google Keep).
            val expired = dao.trashedBefore(System.currentTimeMillis() - TrashDays * 24L * 60 * 60 * 1000)
            if (expired.isNotEmpty()) dao.deleteNotes(expired)
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
            // A reminder may have been set, changed, removed, trashed or restored: the alarm follows.
            com.cenotes.app.notify.Reminders.schedule(app)
        }
    }

    /** Imports picked/captured pictures into the app; returns their file names (failures skipped). */
    suspend fun importImages(uris: List<Uri>): List<String> = uris.mapNotNull { images.import(it) }

    // ---- What is shown ----

    private val filter = MutableStateFlow(Filter())

    val labels: StateFlow<List<Label>> =
        dao.observeLabels().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val links = dao.observeLinks()

    /** The notes out of the Trash and those in it, and the order chosen in Settings. */
    private val sources = combine(dao.observeAll(), dao.observeTrash(), app.settings.state.map { it.sort }) { all, trash, sort ->
        Triple(all, trash, sort)
    }

    /** Notes for the current section, search and "Fijadas" tab, each with its labels. */
    val notes: StateFlow<List<NoteItem>?> = combine(sources, links, labels, filter) { (all, trash, sort), links, labels, f ->
        val labelById = labels.associateBy { it.id }
        val labelsOf = links.groupBy({ it.noteId }, { labelById[it.labelId] }).mapValues { (_, l) ->
            l.filterNotNull().sortedBy { it.name.lowercase() }
        }
        val term = f.query.trim()
        // The Trash keeps its own order (last deleted first); elsewhere pinned notes go first, then the chosen order.
        val ordered = if (f.section == Section.Trash) trash else if (f.section == Section.Reminders) {
            all.sortedBy { it.reminderAt ?: Long.MAX_VALUE } // the next one first
        } else all.sortedWith(
            compareByDescending<Note> { it.pinned }.then(
                when (sort) {
                    NoteSort.EDITED -> compareByDescending { it.updatedAt }
                    NoteSort.CREATED -> compareByDescending { it.createdAt }
                    NoteSort.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { n: Note ->
                        n.title.ifBlank { markdownToPlainText(n.content).trim().lineSequence().firstOrNull().orEmpty() }
                    }
                    NoteSort.MANUAL -> compareByDescending<Note> { it.position }.thenByDescending { it.updatedAt }
                },
            ),
        )
        ordered.asSequence()
            .filter { n ->
                when (val s = f.section) {
                    Section.All -> !n.archived && !n.locked
                    Section.Archive -> n.archived && !n.locked
                    Section.Trash -> true
                    Section.Reminders -> n.reminderAt != null && !n.locked
                    Section.Locked -> n.locked
                    is Section.ByLabel -> !n.archived && !n.locked && labelsOf[n.id].orEmpty().any { it.id == s.labelId }
                }
            }
            .filter { !f.pinnedOnly || it.pinned }
            .filter { term.isEmpty() || it.title.contains(term, true) || it.content.contains(term, true) }
            .map { NoteItem(it, labelsOf[it.id].orEmpty()) }
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Number of active (not archived) notes per label, for the side menu. */
    val labelCounts: StateFlow<Map<Long, Int>> = combine(dao.observeAll(), links) { all, links ->
        val active = all.filterNot { it.archived || it.locked }.map { it.id }.toHashSet()
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
        color: Int,
        reminder: Reminder,
    ) {
        // A note written while looking at "Protected" is protected from the start.
        val lockNew = original == null && filter.value.section == Section.Locked
        write {
            val id = persist(original, title, content, pinned, labelIds, archived, color, reminder)
            if (lockNew && id != null) dao.setLocked(listOf(id), true)
        }
    }

    /**
     * "Delete" in the editor: what is on screen is saved first and then the note goes to the Trash, in one go (so
     * the copy in the Trash has the latest text), with undo.
     */
    fun saveAndTrash(
        original: Note?,
        title: String,
        content: String,
        pinned: Boolean,
        labelIds: List<Long>,
        archived: Boolean,
        color: Int,
        reminder: Reminder,
    ) {
        write {
            val id = persist(original, title, content, pinned, labelIds, archived, color, reminder) ?: return@write
            dao.setTrashed(listOf(id), System.currentTimeMillis())
            offerUndo(R.string.note_trashed, R.string.notes_trashed, 1) { dao.setTrashed(listOf(id), null) }
        }
    }

    /** Stores the editor's content; returns the note's id, or null when the note was empty (and is gone). */
    private suspend fun persist(
        original: Note?,
        title: String,
        content: String,
        pinned: Boolean,
        labelIds: List<Long>,
        archived: Boolean,
        color: Int,
        reminder: Reminder,
    ): Long? {
        if (title.isBlank() && content.isBlank()) {
            original?.let { dao.deleteNotes(listOf(it.id)) }
            return null
        }
        if (original == null) {
            val id = dao.insert(
                Note(
                    title = title, content = content, pinned = pinned, archived = archived, color = color,
                    reminderAt = reminder.at, reminderRepeat = reminder.repeat.ordinal,
                ),
            )
            dao.setLabels(id, labelIds)
            return id
        }
        val textChanged = original.title != title || original.content != content
        // History: the text as it was before this edit, with the time it was written.
        if (textChanged) dao.addVersion(NoteVersion(noteId = original.id, title = original.title, content = original.content, savedAt = original.updatedAt))
        if (textChanged ||original.pinned != pinned || original.archived != archived || original.color != color ||
            original.reminderAt != reminder.at || original.reminderRepeat != reminder.repeat.ordinal
        ) {
            dao.update(
                original.copy(
                    title = title,
                    content = content,
                    pinned = pinned,
                    archived = archived,
                    color = color,
                    reminderAt = reminder.at,
                    reminderRepeat = reminder.repeat.ordinal,
                    updatedAt = if (textChanged) System.currentTimeMillis() else original.updatedAt,
                ),
            )
        }
        if (dao.labelIdsOf(original.id).toSet() != labelIds.toSet()) dao.setLabels(original.id, labelIds)
        return original.id
    }

    /** Earlier versions of a note, the latest first (⋮ → History). */
    suspend fun versionsOf(noteId: Long): List<NoteVersion> = dao.versionsOf(noteId)

    /** Before an earlier version replaces what is on screen, what is on screen goes to the history too (so it can come back). */
    fun keepVersion(noteId: Long, title: String, content: String) {
        if (noteId == 0L) return
        write { dao.addVersion(NoteVersion(noteId = noteId, title = title, content = content, savedAt = System.currentTimeMillis())) }
    }

    /** Whether the list can be reordered by dragging (Settings → Notes → By hand, outside the Trash and Reminders). */
    val canReorder: StateFlow<Boolean> = combine(app.settings.state, filter) { s, f ->
        s.sort == NoteSort.MANUAL && f.section != Section.Trash && f.section != Section.Reminders && f.query.isBlank()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * The notes [shown] in the list were dragged into the order [ids]. They swap their positions among themselves, so
     * notes elsewhere (archive, other labels) keep their place around them.
     */
    fun applyOrder(ids: List<Long>, shown: List<Note>) {
        val byId = shown.associateBy { it.id }
        val order = ids.mapNotNull { byId[it] }
        if (order.size != shown.size || order.map { it.id } == shown.map { it.id }) return
        val positions = shown.map { it.position }.sortedDescending()
        // Equal positions (notes that never had one) would not keep the order: spread them out.
        val distinct = if (positions.toSet().size == positions.size) positions else positions.indices.map { positions.first() - it }
        val changes = order.mapIndexed { i, n -> n.id to distinct[i] }.filter { (id, p) -> byId.getValue(id).position != p }
        if (changes.isNotEmpty()) write { dao.setPositions(changes.toMap()) }
    }

    /** Archiving from the editor: the list then offers "Deshacer". */
    fun noteArchived(noteId: Long, archived: Boolean) {
        if (noteId == 0L) return
        val (one, many) = if (archived) R.string.archived_one to R.string.archived_many
        else R.string.unarchived_one to R.string.unarchived_many
        offerUndo(one, many, 1) { dao.setArchived(listOf(noteId), !archived) }
    }

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

    /** "Delete" sends notes to the Trash (with undo); they are deleted for good after [TrashDays] days. */
    private fun deleteIds(ids: Set<Long>) {
        if (ids.isEmpty()) return
        write { dao.setTrashed(ids, System.currentTimeMillis()) }
        offerUndo(R.string.note_trashed, R.string.notes_trashed, ids.size) { dao.setTrashed(ids, null) }
    }

    // ---- Protected notes ----

    private val _unlocked = MutableStateFlow(false)

    /**
     * Whether the protected notes are open: after the phone's fingerprint, face or PIN, all of them at once, until
     * Cenotes leaves the screen (or "Lock" is tapped).
     */
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    fun unlock() { _unlocked.value = true }

    fun lock() {
        _unlocked.value = false
        if (filter.value.section == Section.Locked) clearSelection()
    }

    /** Protects the selected notes, or takes them out of "Protected" ([locked] false), with undo. */
    fun setLockedOnSelected(locked: Boolean) {
        val ids = takeSelection()
        if (ids.isEmpty()) return
        write { dao.setLocked(ids, locked) }
        val (one, many) = if (locked) R.string.locked_one to R.string.locked_many else R.string.unlocked_one to R.string.unlocked_many
        offerUndo(one, many, ids.size) { dao.setLocked(ids, !locked) }
    }

    /** "Protect" in the editor: what is on screen is saved first and then the note is protected (or not), with undo. */
    fun saveAndLock(
        original: Note?,
        title: String,
        content: String,
        pinned: Boolean,
        labelIds: List<Long>,
        archived: Boolean,
        color: Int,
        reminder: Reminder,
        locked: Boolean,
    ) {
        write {
            val id = persist(original, title, content, pinned, labelIds, archived, color, reminder) ?: return@write
            dao.setLocked(listOf(id), locked)
            val (one, many) = if (locked) R.string.locked_one to R.string.locked_many else R.string.unlocked_one to R.string.unlocked_many
            offerUndo(one, many, 1) { dao.setLocked(listOf(id), !locked) }
        }
    }

    // ---- Trash ----

    /** Brings the selected notes back from the Trash (where they were: list or archive). */
    fun restoreSelected() {
        val ids = takeSelection()
        if (ids.isEmpty()) return
        write { dao.setTrashed(ids, null) }
        offerUndo(R.string.restored_one, R.string.restored_many, ids.size) { dao.setTrashed(ids, System.currentTimeMillis()) }
    }

    /** Deletes the selected notes of the Trash for good (the screen asks first; there is no undo). */
    fun deleteSelectedForever() {
        val ids = takeSelection()
        if (ids.isNotEmpty()) write { dao.deleteNotes(ids) }
    }

    /** Empties the whole Trash for good (the screen asks first). */
    fun emptyTrash() {
        clearSelection()
        write { dao.deleteNotes(dao.trashIds()) }
    }

    // ---- Colour ----

    /** Gives the selected notes a background colour (0 = none). Keeps the selection. */
    fun setColorOnSelected(color: Int) {
        val ids = _selected.value
        if (ids.isNotEmpty()) write { dao.setColor(ids, color) }
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

    fun setLabelColor(id: Long, color: Int) = write { dao.setLabelColor(id, color) }

    /** The app settings (the editor needs "checked items to the bottom"). */
    val settings = app.settings.state

    val customColors: StateFlow<List<Int>> =
        app.settings.state.map { it.customColors }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addCustomColor(color: Int) = app.settings.addCustomColor(color)

    fun editCustomColor(old: Int, new: Int) {
        app.settings.replaceCustomColor(old, new)
        write { dao.recolorLabels(old, new) }
    }

    fun deleteCustomColor(color: Int) = app.settings.removeCustomColor(color)

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

