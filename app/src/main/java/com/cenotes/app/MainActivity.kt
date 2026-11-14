package com.cenotes.app

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import com.cenotes.app.ui.LocalNoteTransition
import com.cenotes.app.ui.NoteTransition
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
import com.cenotes.app.ui.theme.isDarkNow

/** Swallows every pointer event before the children see it. */
private suspend fun PointerInputScope.blockAllTouches() {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
        }
    }
}

// A FragmentActivity (still a ComponentActivity) so the phone's fingerprint/PIN prompt can be shown for protected notes.
class MainActivity : FragmentActivity() {
    /** Request from a launcher shortcut or widget, consumed by the UI. */
    private var pendingAction by mutableStateOf<LaunchAction?>(null)

    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // After a rotation the intent was already handled; only a fresh start acts on it.
        if (savedInstanceState == null) pendingAction = LaunchAction.from(intent)
        val app = application as CenotesApp
        val repo = app.settings
        setContent {
            val settings by repo.state.collectAsStateWithLifecycle()
            // Light or dark chosen in Settings (not the phone's): the clock and icons of the system bars follow it.
            val darkBars = settings.skin.darkBars(settings.isDarkNow())
            LaunchedEffect(darkBars) {
                val transparent = android.graphics.Color.TRANSPARENT
                val style = if (darkBars) {
                    androidx.activity.SystemBarStyle.dark(transparent)
                } else {
                    androidx.activity.SystemBarStyle.light(transparent, transparent)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            // Once or twice a day (Updates skips earlier asks by itself): is there a newer Cenotes on GitHub?
            LaunchedEffect(settings.checkUpdates) {
                if (settings.checkUpdates) runCatching { app.updates.check() }
            }
            CenotesTheme(settings) {
                val vm: NotesViewModel = viewModel(factory = NotesViewModel.Factory)
                // null = list; 0 = new note; otherwise the id of the note being edited.
                var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
                var showSettings by rememberSaveable { mutableStateOf(false) }
                // Each new note gets its own session, so "new note" always opens a blank editor.
                var newNoteSession by rememberSaveable { mutableIntStateOf(0) }
                var newNoteTemplate by rememberSaveable { mutableStateOf("") }
                var newNoteTitle by rememberSaveable { mutableStateOf("") }
                var searchRequest by rememberSaveable { mutableIntStateOf(0) }

                // Every opening gets its own key: reopening a note while its previous editor is
                // still animating out must not reuse that (already "closed") editor, whose back
                // handler would then ignore every back press.
                var openCount by rememberSaveable { mutableIntStateOf(0) }
                fun openNote(id: Long) {
                    openCount++
                    showSettings = false
                    editingId = id
                }

                fun startNewNote(template: String, title: String = "") {
                    newNoteTemplate = template
                    newNoteTitle = title
                    newNoteSession++
                    showSettings = false
                    editingId = 0L
                }

                LaunchedEffect(pendingAction) {
                    // Notes started from outside (shortcuts, the tile, a widget, "Share") never go to "Protected" by themselves.
                    if ((pendingAction is LaunchAction.NewNote || pendingAction is LaunchAction.Shared) && vm.section.value == com.cenotes.app.ui.Section.Locked) {
                        vm.showSection(com.cenotes.app.ui.Section.All)
                    }
                    when (val action = pendingAction) {
                        is LaunchAction.NewNote -> startNewNote(action.template, action.title)
                        // A protected note (from a reminder, say) opens only once unlocked: until then, its section,
                        // which asks for the phone's lock.
                        is LaunchAction.OpenNote -> if (vm.load(action.id)?.locked == true && !vm.unlocked.value) {
                            showSettings = false
                            editingId = null
                            vm.showSection(com.cenotes.app.ui.Section.Locked)
                        } else {
                            openNote(action.id)
                        }
                        // Shared from another app: the pictures are copied into Cenotes first, then the new note
                        // opens with the text (or link) and the pictures, ready to edit or simply leave.
                        is LaunchAction.Shared -> {
                            val names = vm.importImages(action.images)
                            val small = names.size > 1
                            val images = names.joinToString("\n\n") { "![${if (small) "small" else ""}]($it)" }
                            startNewNote(listOf(action.text, images).filter { it.isNotEmpty() }.joinToString("\n\n"), action.title)
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

                // Tablets and open foldables (wide windows): the list and the open note side by side, as in Cemail.
                val twoPane = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= com.cenotes.app.ui.TWO_PANE_MIN_WIDTH
                val noteKey = when {
                    editingId == 0L -> "note:0:$newNoteSession"
                    editingId != null -> "note:$editingId:$openCount"
                    else -> null
                }
                val screen = when {
                    showSettings -> "settings"
                    noteKey != null && !twoPane -> noteKey
                    else -> "list"
                }
                val editorFor: @Composable (String) -> Unit = { key ->
                    NoteEditorScreen(
                        noteId = key.split(':')[1].toLong(),
                        template = newNoteTemplate,
                        initialTitle = newNoteTitle,
                        viewModel = vm,
                        onClose = { editingId = null },
                    )
                }
                val listScreen: @Composable () -> Unit = {
                    NotesListScreen(
                        viewModel = vm,
                        layout = settings.layout,
                        searchRequest = searchRequest,
                        onLayoutChange = repo::setLayout,
                        onOpenNote = ::openNote,
                        onNewNote = { startNewNote("") },
                        onOpenSettings = { showSettings = true },
                    )
                }

                CompositionLocalProvider(com.cenotes.app.ui.glass.LocalListOnRight provides (settings.listOnRight && twoPane)) {
                // One background behind everything: on wide screens it shows beside a centred screen.
                com.cenotes.app.ui.glass.GlassBackground {
                SharedTransitionLayout {
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        if (initialState.startsWith("note:") || targetState.startsWith("note:")) {
                            // The note card itself grows into / shrinks out of the editor
                            // (see noteSharedBounds); the rest just cross-fades.
                            fadeIn(glassSpring()) togetherWith fadeOut(glassSpring())
                        } else {
                            (fadeIn(glassSpring()) + scaleIn(glassSpring(), initialScale = 0.96f)) togetherWith
                                fadeOut(glassSpring())
                        }
                    },
                    label = "screen",
                ) { target ->
                  CompositionLocalProvider(
                    LocalNoteTransition provides NoteTransition(this@SharedTransitionLayout, this),
                  ) {
                    // The screen that is fading out must not react to taps meant for the new one
                    // (e.g. the editor's star sits exactly where the list's search button is).
                    val leaving = transition.targetState == EnterExitState.PostExit
                    Box(
                        Modifier
                            .fillMaxSize()
                            .then(if (leaving) Modifier.pointerInput(Unit) { blockAllTouches() } else Modifier),
                    ) {
                        when {
                            target.startsWith("note:") -> com.cenotes.app.ui.Centered { editorFor(target) }
                            target == "settings" -> com.cenotes.app.ui.Dockable(settings.settingsDock, repo::setSettingsDock) {
                                SettingsScreen(
                                    settings = settings,
                                    repo = repo,
                                    backup = app.backup,
                                    onAccentChanged = vm::refreshExternalViews, // widgets use the accent colour too
                                    onAddColor = vm::addCustomColor,
                                    onEditColor = vm::editCustomColor,
                                    onDeleteColor = vm::deleteCustomColor,
                                    onNotesImported = vm::refreshExternalViews,
                                    onBack = { showSettings = false },
                                )
                            }
                            twoPane -> com.cenotes.app.ui.TwoPane(list = listScreen, openKey = noteKey, note = editorFor)
                            else -> com.cenotes.app.ui.Centered(listScreen)
                        }
                    }
                  }
                }
                }
                }
                }
            }
        }
    }

    // Leaving Cenotes (home, another app, the screen going off) locks the protected notes again.
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            androidx.lifecycle.ViewModelProvider(this, NotesViewModel.Factory)[NotesViewModel::class.java].lock()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingAction = LaunchAction.from(intent)
    }
}
