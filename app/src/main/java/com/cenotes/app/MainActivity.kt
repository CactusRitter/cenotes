package com.cenotes.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cenotes.app.ui.NoteEditorScreen
import com.cenotes.app.ui.NotesListScreen
import com.cenotes.app.ui.NotesViewModel
import com.cenotes.app.ui.SettingsScreen
import com.cenotes.app.ui.glass.glassSpring
import com.cenotes.app.ui.theme.CenotesTheme

/** Swallows every pointer event before the children see it. */
private suspend fun PointerInputScope.blockAllTouches() {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
        }
    }
}

class MainActivity : ComponentActivity() {
    /** Request from a launcher shortcut or widget, consumed by the UI. */
    private var pendingAction by mutableStateOf<LaunchAction?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // After a rotation the intent was already handled; only a fresh start acts on it.
        if (savedInstanceState == null) pendingAction = LaunchAction.from(intent)
        val app = application as CenotesApp
        val repo = app.settings
        setContent {
            val settings by repo.state.collectAsStateWithLifecycle()
            CenotesTheme(settings) {
                val vm: NotesViewModel = viewModel(factory = NotesViewModel.Factory)
                // null = list; 0 = new note; otherwise the id of the note being edited.
                var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
                var showSettings by rememberSaveable { mutableStateOf(false) }
                // Each new note gets its own session, so "new note" always opens a blank editor.
                var newNoteSession by rememberSaveable { mutableIntStateOf(0) }
                var newNoteTemplate by rememberSaveable { mutableStateOf("") }
                var searchRequest by rememberSaveable { mutableIntStateOf(0) }

                fun startNewNote(template: String) {
                    newNoteTemplate = template
                    newNoteSession++
                    showSettings = false
                    editingId = 0L
                }

                LaunchedEffect(pendingAction) {
                    when (val action = pendingAction) {
                        is LaunchAction.NewNote -> startNewNote(action.template)
                        is LaunchAction.OpenNote -> {
                            showSettings = false
                            editingId = action.id
                        }
                        LaunchAction.Search -> {
                            showSettings = false
                            editingId = null
                            searchRequest++
                        }
                        null -> Unit
                    }
                    pendingAction = null
                }

                val screen = when {
                    editingId == 0L -> "note:0:$newNoteSession"
                    editingId != null -> "note:$editingId"
                    showSettings -> "settings"
                    else -> "list"
                }

                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        (fadeIn(glassSpring()) + scaleIn(glassSpring(), initialScale = 0.96f)) togetherWith
                            fadeOut(glassSpring())
                    },
                    label = "screen",
                ) { target ->
                    // The screen that is fading out must not react to taps meant for the new one
                    // (e.g. the editor's star sits exactly where the list's search button is).
                    val leaving = transition.targetState == EnterExitState.PostExit
                    Box(
                        Modifier
                            .fillMaxSize()
                            .then(if (leaving) Modifier.pointerInput(Unit) { blockAllTouches() } else Modifier),
                    ) {
                        when {
                            target.startsWith("note:") -> NoteEditorScreen(
                                noteId = target.split(':')[1].toLong(),
                                template = newNoteTemplate,
                                viewModel = vm,
                                onClose = { editingId = null },
                            )
                            target == "settings" -> SettingsScreen(
                                settings = settings,
                                backup = app.backup,
                                onAccentChange = { accent ->
                                    repo.setAccent(accent)
                                    vm.refreshExternalViews() // widgets use the accent colour too
                                },
                                onTransparencyChange = repo::setTransparency,
                                onAddColor = vm::addCustomColor,
                                onEditColor = vm::editCustomColor,
                                onDeleteColor = vm::deleteCustomColor,
                                onNotesImported = vm::refreshExternalViews,
                                onBack = { showSettings = false },
                            )
                            else -> NotesListScreen(
                                viewModel = vm,
                                layout = settings.layout,
                                searchRequest = searchRequest,
                                onLayoutChange = repo::setLayout,
                                onOpenNote = { editingId = it },
                                onNewNote = { startNewNote("") },
                                onOpenSettings = { showSettings = true },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingAction = LaunchAction.from(intent)
    }
}
