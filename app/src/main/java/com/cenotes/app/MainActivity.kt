package com.cenotes.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as CenotesApp).settings
        setContent {
            val settings by repo.state.collectAsStateWithLifecycle()
            CenotesTheme(settings) {
                val vm: NotesViewModel = viewModel(factory = NotesViewModel.Factory)
                // null = list; 0 = new note; otherwise the id of the note being edited.
                var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
                var showSettings by rememberSaveable { mutableStateOf(false) }
                val screen = when {
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
                            noteId = target.removePrefix("note:").toLong(),
                            viewModel = vm,
                            onClose = { editingId = null },
                        )
                        target == "settings" -> SettingsScreen(
                            settings = settings,
                            onAccentChange = repo::setAccent,
                            onTransparencyChange = repo::setTransparency,
                            onBack = { showSettings = false },
                        )
                        else -> NotesListScreen(
                            viewModel = vm,
                            layout = settings.layout,
                            onLayoutChange = repo::setLayout,
                            onOpenNote = { editingId = it },
                            onNewNote = { editingId = 0L },
                            onOpenSettings = { showSettings = true },
                        )
                    }
                    }
                }
            }
        }
    }
}
