package com.cenotes.app.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cenotes.app.CenotesApp
import com.cenotes.app.ExternalViews
import com.cenotes.app.data.ImageStore
import com.cenotes.app.data.Note
import com.cenotes.app.data.NoteDao
import com.cenotes.app.ui.markdown.imageNames
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _pinnedOnly = MutableStateFlow(false)
    val pinnedOnly: StateFlow<Boolean> = _pinnedOnly.asStateFlow()

    /** Notes filtered by the search query and the "Fijadas" tab; pinned first, then most recently edited. */
    val notes: StateFlow<List<Note>?> = combine(dao.observeAll(), _query, _pinnedOnly) { all, q, pinned ->
        val term = q.trim()
        all.filter { note ->
            (!pinned || note.pinned) &&
                (term.isEmpty() || note.title.contains(term, ignoreCase = true) ||
                    note.content.contains(term, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setPinnedOnly(value: Boolean) {
        _pinnedOnly.value = value
    }

    /** The most recently deleted note, kept so the list can offer "Deshacer". */
    private val _lastDeleted = MutableStateFlow<Note?>(null)
    val lastDeleted: StateFlow<Note?> = _lastDeleted.asStateFlow()

    fun setQuery(value: String) {
        _query.value = value
    }

    suspend fun load(id: Long): Note? = dao.get(id)

    /**
     * Persists the editor's content. An empty note is not stored (and an existing one that was
     * emptied is deleted). The edit time only moves when the text actually changed.
     */
    fun save(original: Note?, title: String, content: String, pinned: Boolean) = write {
        if (title.isBlank() && content.isBlank()) {
            original?.let { dao.delete(it) }
            return@write
        }
        if (original == null) {
            dao.insert(Note(title = title, content = content, pinned = pinned))
            return@write
        }
        val textChanged = original.title != title || original.content != content
        if (!textChanged && original.pinned == pinned) return@write
        dao.update(
            original.copy(
                title = title,
                content = content,
                pinned = pinned,
                updatedAt = if (textChanged) System.currentTimeMillis() else original.updatedAt,
            )
        )
    }

    fun togglePin(note: Note) = write { dao.update(note.copy(pinned = !note.pinned)) }

    fun delete(note: Note) = write {
        dao.delete(note)
        _lastDeleted.value = note
    }

    fun undoDelete() {
        val note = _lastDeleted.value ?: return
        _lastDeleted.value = null
        write { dao.insert(note) }
    }

    fun clearLastDeleted() {
        _lastDeleted.value = null
    }

    /** After an import: widgets and shortcuts need to show the new notes. */
    fun refreshExternalViews() = write { }

    companion object {
        val Factory = viewModelFactory {
            initializer { NotesViewModel(this[APPLICATION_KEY] as CenotesApp) }
        }
    }
}
