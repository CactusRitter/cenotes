package com.example.notas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
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
import com.example.notas.ui.NoteEditorScreen
import com.example.notas.ui.NotesListScreen
import com.example.notas.ui.NotesViewModel
import com.example.notas.ui.SettingsScreen
import com.example.notas.ui.glass.glassSpring
import com.example.notas.ui.theme.NotesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as NotesApp).settings
        setContent {
            val settings by repo.state.collectAsStateWithLifecycle()
            NotesTheme(settings) {
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
