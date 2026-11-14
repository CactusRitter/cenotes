package com.cenotes.app.ui

import com.cenotes.app.ui.theme.LockOpenIcon
import androidx.compose.material.icons.filled.Lock
import com.cenotes.app.data.Repeat
import androidx.compose.material.icons.filled.Notifications
import com.cenotes.app.data.TrashDays
import com.cenotes.app.ui.theme.DeleteForeverIcon
import com.cenotes.app.ui.theme.RestoreFromTrashIcon
import com.cenotes.app.ui.theme.PaletteIcon
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.material3.AlertDialog
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.cenotes.app.CenotesApp
import com.cenotes.app.R
import com.cenotes.app.data.Label
import com.cenotes.app.data.NoteLayout
import com.cenotes.app.ui.glass.GlassBackground
import com.cenotes.app.ui.glass.GlassIconButton
import com.cenotes.app.ui.glass.GlassKind
import com.cenotes.app.ui.glass.GlassPill
import com.cenotes.app.ui.glass.PillIcon
import com.cenotes.app.ui.glass.glass
import com.cenotes.app.ui.glass.glassSpring
import com.cenotes.app.ui.glass.onBackdrop
import com.cenotes.app.ui.glass.popIn
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import com.cenotes.app.ui.glass.pressScale
import com.cenotes.app.ui.glass.softBounceSpring
import com.cenotes.app.ui.markdown.MarkdownPreview
import com.cenotes.app.ui.markdown.firstImage
import com.cenotes.app.ui.markdown.taskStats
import com.cenotes.app.ui.theme.ArchiveIcon
import com.cenotes.app.ui.theme.CheckBoxIcon
import com.cenotes.app.ui.theme.GridViewIcon
import com.cenotes.app.ui.theme.LabelIcon
import com.cenotes.app.ui.theme.LocalGlass
import com.cenotes.app.ui.theme.LocalSkin
import com.cenotes.app.ui.theme.NotesIcon
import com.cenotes.app.ui.theme.UnarchiveIcon
import com.cenotes.app.ui.theme.ViewAgendaIcon
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch

private val HeaderHeight = 72.dp
private val SearchHeight = 68.dp

@Composable
fun NotesListScreen(
    viewModel: NotesViewModel,
    layout: NoteLayout,
    /** Bumped by the "Buscar" launcher shortcut to open the search field. */
    searchRequest: Int,
    onLayoutChange: (NoteLayout) -> Unit,
    onOpenNote: (Long) -> Unit,
    onNewNote: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val labels by viewModel.labels.collectAsStateWithLifecycle()
    val customColors by viewModel.customColors.collectAsStateWithLifecycle()
    val labelCounts by viewModel.labelCounts.collectAsStateWithLifecycle()
    val section by viewModel.section.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val pinnedOnly by viewModel.pinnedOnly.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val unlocked by viewModel.unlocked.collectAsStateWithLifecycle()
    val undo by viewModel.undo.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val hazeState = rememberHazeState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var searchOpen by rememberSaveable { mutableStateOf(query.isNotEmpty()) }
    var bulkLabels by remember { mutableStateOf(false) }
    var bulkColor by remember { mutableStateOf(false) }
    // What "Delete for good" is about to delete (it asks first: there is no way back).
    var confirmForever by remember { mutableStateOf<ForeverTarget?>(null) }
    var manageLabels by remember { mutableStateOf(false) }
    LaunchedEffect(searchRequest) { if (searchRequest > 0) searchOpen = true }

    // Opening "Protected" asks for the phone's lock straight away (and again from its button, if cancelled).
    val activity = androidx.activity.compose.LocalActivity.current as? androidx.fragment.app.FragmentActivity
    val unlockTitle = stringResource(R.string.unlock_title)
    val unlockBody = stringResource(R.string.unlock_body)
    val askUnlock: () -> Unit = {
        val a = activity
        if (a != null && Unlock.available(a)) Unlock.ask(a, unlockTitle, unlockBody) { viewModel.unlock() }
    }
    LaunchedEffect(section) { if (section == Section.Locked && !unlocked) askUnlock() }

    val selecting = selected.isNotEmpty()
    BackHandler(enabled = selecting) { viewModel.clearSelection() }
    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    val undoLabel = stringResource(R.string.undo)
    LaunchedEffect(undo) {
        val event = undo ?: return@LaunchedEffect
        snackbarHostState.currentSnackbarData?.dismiss()
        val result = snackbarHostState.showSnackbar(event.message, undoLabel, duration = SnackbarDuration.Short)
        if (result == SnackbarResult.ActionPerformed) viewModel.performUndo(event) else viewModel.dismissUndo(event)
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val topPadding = topInset + HeaderHeight + (if (searchOpen) SearchHeight else 0.dp)

    val sectionTitle = when (val s = section) {
        Section.All -> stringResource(R.string.app_name)
        Section.Archive -> stringResource(R.string.section_archive)
        Section.Trash -> stringResource(R.string.section_trash)
        Section.Reminders -> stringResource(R.string.section_reminders)
        Section.Locked -> stringResource(R.string.section_locked)
        is Section.ByLabel -> labels.firstOrNull { it.id == s.labelId }?.name ?: stringResource(R.string.app_name)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !selecting,
        drawerContent = {
            SideMenu(
                hazeState = hazeState,
                section = section,
                labels = labels,
                counts = labelCounts,
                onSection = {
                    viewModel.showSection(it)
                    scope.launch { drawerState.close() }
                },
                onEditLabels = {
                    manageLabels = true
                    scope.launch { drawerState.close() }
                },
                onSettings = {
                    scope.launch { drawerState.close() }
                    onOpenSettings()
                },
            )
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            // Everything in here is what the floating glass blurs.
            GlassBackground(Modifier.hazeSource(hazeState)) {
                val list = notes
                when {
                    list == null -> Unit // Still loading; avoid flashing an empty state.
                    // Protected notes stay out of sight until the phone's lock is given.
                    section == Section.Locked && !unlocked -> LockedScreen(onUnlock = askUnlock)
                    list.isEmpty() && query.isBlank() && !pinnedOnly && section == Section.All -> WelcomeCard(
                        onCreate = onNewNote,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 16.dp, end = 16.dp, bottom = bottomInset + 104.dp),
                    )
                    list.isEmpty() -> EmptyText(
                        stringResource(
                            when {
                                query.isNotBlank() -> R.string.empty_search
                                pinnedOnly -> R.string.empty_pinned
                                section == Section.Archive -> R.string.empty_archive
                                section == Section.Trash -> R.string.empty_trash_list
                                section == Section.Reminders -> R.string.empty_reminders
                                section == Section.Locked -> R.string.empty_locked
                                else -> R.string.empty_label
                            },
                        ),
                    )
                    else -> LazyVerticalStaggeredGrid(
                        columns = if (layout == NoteLayout.GRID) StaggeredGridCells.Adaptive(160.dp)
                        else StaggeredGridCells.Fixed(1),
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp, top = topPadding + 4.dp, bottom = bottomInset + 112.dp,
                        ),
                        verticalItemSpacing = 10.dp,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        // A newer Cenotes on GitHub: the notice goes first, across the whole width (like Cemail's Inbox).
                        if (section == Section.All && query.isBlank()) {
                            item(key = "update", span = StaggeredGridItemSpan.FullLine) { UpdateBanner() }
                        }
                        // Protected notes, open: a reminder of it and a button to lock them again at once.
                        if (section == Section.Locked) {
                            item(key = "locked", span = StaggeredGridItemSpan.FullLine) { LockedHeader(onLock = viewModel::lock) }
                        }
                        // The Trash says how long notes stay in it, and can be emptied at once.
                        if (section == Section.Trash) {
                            item(key = "trash", span = StaggeredGridItemSpan.FullLine) {
                                TrashHeader(onEmpty = { confirmForever = ForeverTarget.All })
                            }
                        }
                        items(list, key = { it.note.id }) { item ->
                            val id = item.note.id
                            NoteCard(
                                item = item,
                                maxLines = if (layout == NoteLayout.GRID) 8 else 4,
                                selected = id in selected,
                                // A protected note in the Trash shows nothing of itself until the notes are unlocked.
                                hidden = item.note.locked && !unlocked,
                                // While selecting, a tap adds/removes; otherwise it opens the note.
                                // In the Trash a note is not opened: a tap picks it, to restore it or delete it for good.
                                onClick = { if (selecting || section == Section.Trash) viewModel.toggleSelected(id) else onOpenNote(id) },
                                onLongClick = { viewModel.toggleSelected(id) },
                                modifier = Modifier
                                    .animateItem(
                                        fadeInSpec = glassSpring(), placementSpec = softBounceSpring(), fadeOutSpec = glassSpring(),
                                    )
                                    .noteSharedBounds(id),
                            )
                        }
                    }
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp),
            ) {
                if (selecting) {
                    SelectionBar(
                        count = selected.size,
                        archiveSection = section == Section.Archive,
                        trash = section == Section.Trash,
                        lockedSection = section == Section.Locked,
                        hazeState = hazeState,
                        onClose = viewModel::clearSelection,
                        onPin = viewModel::pinSelected,
                        onColor = { bulkColor = true },
                        onLabel = { bulkLabels = true },
                        onArchive = { viewModel.archiveSelected(section != Section.Archive) },
                        onDelete = viewModel::deleteSelected,
                        onRestore = viewModel::restoreSelected,
                        onUnprotect = { viewModel.setLockedOnSelected(false) },
                        onDeleteForever = { confirmForever = ForeverTarget.Selection },
                    )
                } else {
                    // Menu on the left, section title centred, search + new note on the right.
                    Box(Modifier.fillMaxWidth().height(HeaderHeight), contentAlignment = Alignment.Center) {
                        GlassIconButton(
                            Icons.Filled.Menu,
                            stringResource(R.string.menu),
                            { scope.launch { drawerState.open() } },
                            Modifier.align(Alignment.CenterStart),
                            hazeState = hazeState,
                        )
                        Text(
                            sectionTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = onBackdrop(MaterialTheme.colorScheme.onSurface),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 76.dp),
                        )
                        // Like Cemail: a round search button opposite the menu one (new note is the button at the bottom).
                        GlassIconButton(
                            if (searchOpen) Icons.Filled.Close else Icons.Filled.Search,
                            stringResource(if (searchOpen) R.string.clear_search else R.string.search),
                            {
                                if (searchOpen) viewModel.setQuery("")
                                searchOpen = !searchOpen
                            },
                            Modifier.align(Alignment.CenterEnd),
                            hazeState = hazeState,
                        )
                    }
                }
                AnimatedVisibility(
                    visible = searchOpen,
                    enter = fadeIn(glassSpring()) + expandVertically(glassSpring()),
                    exit = fadeOut(glassSpring()) + shrinkVertically(glassSpring()),
                ) {
                    SearchField(query, viewModel::setQuery, hazeState)
                }
            }

            // Bottom glass pill: layout and the "Fijadas" filter. The highlight slides under the
            // finger when dragging across the pill (like Celandar's view pill).
            LayoutPill(
                layout = layout,
                onLayoutChange = onLayoutChange,
                pinnedOnly = pinnedOnly,
                onTogglePinned = { viewModel.setPinnedOnly(!pinnedOnly) },
                hazeState = hazeState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 18.dp),
            )

            // New note: the same floating button as Cemail's "write" (60 dp, accent glass, bottom corner).
            if (!selecting && section != Section.Trash) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .popIn(delayMillis = 220, initialScale = 0.3f)
                        .pressScale(0.88f)
                        .size(60.dp)
                        .glass(
                            LocalSkin.current.fabCorner?.let { RoundedCornerShape(it) } ?: CircleShape,
                            GlassKind.Pinned, elevation = 10.dp, hazeState = hazeState,
                        )
                        .clickable(role = Role.Button, onClickLabel = stringResource(R.string.new_note), onClick = onNewNote),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Add, stringResource(R.string.new_note), tint = MaterialTheme.colorScheme.primary)
                }
            }

            SnackbarHost(
                snackbarHostState,
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 96.dp),
            ) { data ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .glass(RoundedCornerShape(28.dp), GlassKind.Menu, elevation = 10.dp)
                        .padding(start = 22.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(data.visuals.message, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    data.visuals.actionLabel?.let { label ->
                        TextButton(onClick = { data.performAction() }) { Text(label, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }

    if (bulkColor) {
        // The colour the selected notes share, if they all have the same one (otherwise nothing is ticked).
        val shared = notes.orEmpty().filter { it.note.id in selected }.map { it.note.color }.distinct().singleOrNull() ?: -1
        NoteColorDialog(shared, onPick = viewModel::setColorOnSelected, onDismiss = { bulkColor = false })
    }
    confirmForever?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmForever = null },
            shape = RoundedCornerShape(LocalSkin.current.radius(28.dp)),
            containerColor = LocalGlass.current.menu,
            title = {
                Text(
                    if (target == ForeverTarget.All) stringResource(R.string.empty_trash_title)
                    else pluralStringResource(R.plurals.delete_forever_title, selected.size, selected.size),
                    fontWeight = FontWeight.Bold,
                )
            },
            text = { Text(stringResource(R.string.delete_forever_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmForever = null
                    if (target == ForeverTarget.All) viewModel.emptyTrash() else viewModel.deleteSelectedForever()
                }) { Text(stringResource(R.string.delete_forever), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmForever = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (bulkLabels) {
        val items = notes.orEmpty()
        BulkLabelDialog(
            labels = labels,
            counts = viewModel.selectedLabelCounts(items),
            total = selected.size,
            onSet = viewModel::setLabelOnSelected,
            onCreate = { name -> viewModel.createLabel(name)?.let { viewModel.setLabelOnSelected(it, add = true) } },
            onDismiss = { bulkLabels = false },
        )
    }
    if (manageLabels) {
        ManageLabelsDialog(
            labels = labels,
            onCreate = { viewModel.createLabel(it) },
            onRename = viewModel::renameLabel,
            onColor = viewModel::setLabelColor,
            customColors = customColors,
            onAddColor = viewModel::addCustomColor,
            onEditColor = viewModel::editCustomColor,
            onDeleteColor = viewModel::deleteCustomColor,
            onDelete = viewModel::deleteLabel,
            onDismiss = { manageLabels = false },
        )
    }
}

/**
 * Replaces the header while notes are selected: count, then pin / colour / label / archive / delete. In the Trash:
 * restore and delete for good.
 */
@Composable
private fun SelectionBar(
    count: Int,
    archiveSection: Boolean,
    trash: Boolean,
    lockedSection: Boolean,
    hazeState: HazeState,
    onClose: () -> Unit,
    onPin: () -> Unit,
    onColor: () -> Unit,
    onLabel: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
    onUnprotect: () -> Unit,
) {
    if (trash) {
        Row(Modifier.fillMaxWidth().height(HeaderHeight), verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(Icons.Filled.Close, stringResource(R.string.cancel_selection), onClose, hazeState = hazeState)
            Spacer(Modifier.width(14.dp))
            Text(
                stringResource(R.string.selected_count, count),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = onBackdrop(MaterialTheme.colorScheme.onSurface),
            )
            Spacer(Modifier.weight(1f))
            GlassPill(hazeState = hazeState) {
                PillIcon(RestoreFromTrashIcon, stringResource(R.string.restore), onRestore)
                PillIcon(DeleteForeverIcon, stringResource(R.string.delete_forever), onDeleteForever)
            }
        }
        return
    }
    Row(Modifier.fillMaxWidth().height(HeaderHeight), verticalAlignment = Alignment.CenterVertically) {
        GlassIconButton(Icons.Filled.Close, stringResource(R.string.cancel_selection), onClose, hazeState = hazeState)
        Spacer(Modifier.width(14.dp))
        Text(
            stringResource(R.string.selected_count, count),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = onBackdrop(MaterialTheme.colorScheme.onSurface),
        )
        Spacer(Modifier.weight(1f))
        // Five actions: a little narrower than usual (like Cemail's selection pill) so they fit next to the count.
        GlassPill(hazeState = hazeState) {
            PillIcon(Icons.Filled.Star, stringResource(R.string.pin_selected), onPin, width = 46.dp)
            PillIcon(PaletteIcon, stringResource(R.string.note_color), onColor, width = 46.dp)
            PillIcon(LabelIcon, stringResource(R.string.labels), onLabel, width = 46.dp)
            // In "Protected", the archive place is taken by "take out of Protected".
            if (lockedSection) {
                PillIcon(LockOpenIcon, stringResource(R.string.unlock_note), onUnprotect, width = 46.dp)
            } else {
                PillIcon(
                    if (archiveSection) UnarchiveIcon else ArchiveIcon,
                    stringResource(if (archiveSection) R.string.unarchive else R.string.archive),
                    onArchive,
                    width = 46.dp,
                )
            }
            PillIcon(Icons.Filled.Delete, stringResource(R.string.delete), onDelete, width = 46.dp)
        }
    }
}

/** "Protected" while it is locked: a padlock, a line saying how to open it, and the button that asks for the lock. */
@Composable
private fun LockedScreen(onUnlock: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val available = remember { Unlock.available(context) }
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .popIn(initialScale = 0.9f)
                .fillMaxWidth()
                .glass(RoundedCornerShape(32.dp), GlassKind.Chrome, elevation = 10.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBubble(56.dp) { Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.primary) }
            Text(stringResource(R.string.section_locked), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                stringResource(if (available) R.string.locked_info else R.string.locked_no_lock),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (available) {
                Button(onClick = onUnlock, shape = LocalSkin.current.shape(CircleShape)) {
                    Icon(LockOpenIcon, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.unlock), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** First row of "Protected" while it is open: they lock again when Cenotes is left, or now with the button. */
@Composable
private fun LockedHeader(onLock: () -> Unit) {
    Row(
        Modifier
            .popIn(initialScale = 0.9f)
            .fillMaxWidth()
            .glass(RoundedCornerShape(24.dp), GlassKind.Card)
            .padding(start = 18.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.locked_open_info),
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onLock) { Text(stringResource(R.string.lock), fontWeight = FontWeight.Bold) }
    }
}

/** Side menu: all notes, each label with its count, the archive, then label editing and settings. */
@Composable
private fun SideMenu(
    hazeState: HazeState,
    section: Section,
    labels: List<Label>,
    counts: Map<Long, Int>,
    onSection: (Section) -> Unit,
    onEditLabels: () -> Unit,
    onSettings: () -> Unit,
) {
    val colors = NavigationDrawerItemDefaults.colors(
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedContainerColor = Color.Transparent,
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.onSurface,
    )
    // Like Google Keep: a narrow panel (not the full width), square on the right, with the
    // selected item as a pill that runs from the left edge.
    ModalDrawerSheet(
        modifier = Modifier.width(DrawerWidth),
        // The sheet itself is see-through; the glass (blur + tint) is drawn by the column inside.
        drawerContainerColor = Color.Transparent,
        drawerTonalElevation = 0.dp,
        drawerShape = RectangleShape,
        windowInsets = WindowInsets(0),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .glass(RectangleShape, GlassKind.Card, hazeState = hazeState)
                // Extra veil so a photo behind the panel can't make the text hard to read.
                .background(LocalGlass.current.base.copy(alpha = 0.62f))
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(end = 12.dp),
        ) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 28.dp, top = 28.dp, bottom = 16.dp),
            )
            MenuItem(NotesIcon, stringResource(R.string.section_notes), section == Section.All, colors) {
                onSection(Section.All)
            }
            // Like Keep: the notes that will remind you, the next one first.
            MenuItem(Icons.Filled.Notifications, stringResource(R.string.section_reminders), section == Section.Reminders, colors) {
                onSection(Section.Reminders)
            }
            if (labels.isNotEmpty()) {
                Text(
                    stringResource(R.string.labels),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 28.dp, top = 16.dp, bottom = 6.dp),
                )
            }
            labels.forEach { label ->
                NavigationDrawerItem(
                    label = { Text(label.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    icon = { Icon(LabelIcon, null, tint = labelColor(label)) },
                    badge = { counts[label.id]?.let { Text(it.toString()) } },
                    selected = section == Section.ByLabel(label.id),
                    onClick = { onSection(Section.ByLabel(label.id)) },
                    colors = colors,
                    shape = KeepItemShape,
                )
            }
            MenuItem(Icons.Filled.Edit, stringResource(R.string.labels_edit), false, colors, onEditLabels)
            HorizontalDivider(Modifier.padding(vertical = 10.dp, horizontal = 16.dp))
            // Archive, then Trash, as in Google Keep.
            MenuItem(ArchiveIcon, stringResource(R.string.section_archive), section == Section.Archive, colors) {
                onSection(Section.Archive)
            }
            MenuItem(Icons.Filled.Lock, stringResource(R.string.section_locked), section == Section.Locked, colors) {
                onSection(Section.Locked)
            }
            MenuItem(Icons.Filled.Delete, stringResource(R.string.section_trash), section == Section.Trash, colors) {
                onSection(Section.Trash)
            }
            MenuItem(Icons.Filled.Settings, stringResource(R.string.settings), false, colors, onSettings)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    text: String,
    selected: Boolean,
    colors: androidx.compose.material3.NavigationDrawerItemColors,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        label = { Text(text) },
        icon = { Icon(icon, null) },
        selected = selected,
        onClick = onClick,
        colors = colors,
        shape = KeepItemShape,
    )
}

private val DrawerWidth = 280.dp
private val KeepItemShape = RoundedCornerShape(topStartPercent = 0, topEndPercent = 50, bottomEndPercent = 50, bottomStartPercent = 0)

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, hazeState: HazeState) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .padding(top = 4.dp, bottom = 8.dp)
            .fillMaxWidth()
            .glass(CircleShape, GlassKind.Chrome, elevation = 10.dp, hazeState = hazeState)
            .focusRequester(focus),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.clear_search))
                }
            }
        },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun NoteCard(
    item: NoteItem,
    maxLines: Int,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** A protected note while locked: only a padlock and "Protected note", nothing of what it says. */
    hidden: Boolean = false,
) {
    val note = item.note
    val shape = LocalSkin.current.shape(RoundedCornerShape(24.dp))
    val context = LocalContext.current
    val cover = remember(note.content) { firstImage(note.content) }
    val primary = MaterialTheme.colorScheme.primary
    Box(modifier) {
        Column(
            Modifier
                .pressScale(0.97f)
                .fillMaxWidth()
                .glass(shape, if (note.pinned) GlassKind.Pinned else GlassKind.Card)
                .noteTint(note.color)
                .then(if (selected) Modifier.border(2.5.dp, primary, shape) else Modifier)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        ) {
            if (hidden) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, null, Modifier.size(18.dp), tint = primary)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.locked_note), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                return@Column
            }
            // The note's first image works as its cover, like in Keep.
            if (cover != null) {
                AsyncImage(
                    model = (context.applicationContext as CenotesApp).images.file(cover),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(if (maxLines > 4) 130.dp else 150.dp),
                )
            }
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = note.title.ifBlank { stringResource(R.string.untitled) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (note.title.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (note.pinned) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = stringResource(R.string.pinned),
                            tint = primary,
                            modifier = Modifier.padding(start = 4.dp).size(18.dp),
                        )
                    }
                }
                if (note.content.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    MarkdownPreview(note.content, maxBlocks = maxLines)
                }
                note.reminderAt?.let { at ->
                    Spacer(Modifier.height(10.dp))
                    ReminderChip(at, Repeat.of(note.reminderRepeat), compact = true)
                }
                if (item.labels.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        item.labels.take(3).forEach { LabelChip(it) }
                        if (item.labels.size > 3) {
                            Text(
                                "+${item.labels.size - 3}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val tasks = remember(note.content) { taskStats(note.content) }
                    if (tasks.total > 0) {
                        val allDone = tasks.done == tasks.total
                        Row(
                            Modifier
                                .clip(LocalSkin.current.shape(CircleShape))
                                .background(if (allDone) primary else MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val tint = if (allDone) MaterialTheme.colorScheme.onPrimary else primary
                            Icon(CheckBoxIcon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.tasks_progress, tasks.done, tasks.total),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = tint,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    val justNow = System.currentTimeMillis() - note.updatedAt < DateUtils.MINUTE_IN_MILLIS
                    val whenText = if (justNow) stringResource(R.string.just_now)
                    else DateUtils.getRelativeTimeSpanString(note.updatedAt).toString().lowercase()
                    Text(
                        // Next to the checklist chip there's only room for the time itself.
                        text = if (tasks.total > 0) whenText.replaceFirstChar { it.uppercase() }
                        else stringResource(R.string.edited, whenText),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Same shape as Celander's "Welcome" card: icon, title, one line, and a filled accent button. */
@Composable
private fun WelcomeCard(onCreate: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(32.dp), GlassKind.Chrome, elevation = 10.dp)
            .padding(22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).glass(CircleShape, GlassKind.Pinned),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.welcome_text),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))
        Button(onClick = onCreate, modifier = Modifier.height(52.dp), shape = CircleShape) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.create_first_note), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyText(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = onBackdrop(MaterialTheme.colorScheme.onSurfaceVariant),
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Bottom pill with the grid / list switch and the "Fijadas" filter. The highlight behind the grid
 * and list icons is one capsule that springs between them, and when a finger slides across the pill
 * the capsule follows the finger (clamped to the two icons) and settles on release, like Celandar's
 * view pill. The view changes as soon as the finger crosses the middle.
 */
@Composable
private fun LayoutPill(
    layout: NoteLayout,
    onLayoutChange: (NoteLayout) -> Unit,
    pinnedOnly: Boolean,
    onTogglePinned: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val slot = with(density) { 52.dp.toPx() } // width of one icon
    val edge = with(density) { 8.dp.toPx() } // pill padding + spacer before the first icon
    val currentLayout by rememberUpdatedState(layout)
    val changeLayout by rememberUpdatedState(onLayoutChange)
    fun slotX(l: NoteLayout) = if (l == NoteLayout.LIST) slot else 0f

    val indicator = remember { androidx.compose.animation.core.Animatable(slotX(layout)) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(layout, dragging) {
        if (!dragging) indicator.animateTo(slotX(layout), glassSpring())
    }

    GlassPill(
        modifier.pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = { dragging = true },
                onDragEnd = { dragging = false },
                onDragCancel = { dragging = false },
            ) { change, _ ->
                val local = change.position.x - edge // finger position measured from the first icon
                scope.launch { indicator.snapTo((local - slot / 2).coerceIn(0f, slot)) }
                val target = when {
                    local < slot -> NoteLayout.GRID
                    local < slot * 2.25f -> NoteLayout.LIST
                    else -> null // over the star: it is a filter, not a view
                }
                if (target != null && target != currentLayout) {
                    changeLayout(target)
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                }
            }
        },
        hazeState = hazeState,
    ) {
        Spacer(Modifier.width(4.dp))
        Box(Modifier.height(48.dp)) {
            Box(
                Modifier
                    .offset { androidx.compose.ui.unit.IntOffset(indicator.value.toInt(), 0) }
                    .size(width = 52.dp, height = 48.dp)
                    .clip(LocalSkin.current.shape(CircleShape))
                    .background(MaterialTheme.colorScheme.primaryContainer),
            )
            Row {
                PillIcon(
                    GridViewIcon, stringResource(R.string.layout_grid),
                    onClick = { onLayoutChange(NoteLayout.GRID) },
                    selected = layout == NoteLayout.GRID, showBackground = false,
                )
                PillIcon(
                    ViewAgendaIcon, stringResource(R.string.layout_list),
                    onClick = { onLayoutChange(NoteLayout.LIST) },
                    selected = layout == NoteLayout.LIST, showBackground = false,
                )
            }
        }
        Box(
            Modifier
                .padding(horizontal = 6.dp)
                .size(width = 1.dp, height = 32.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
        )
        PillIcon(
            Icons.Filled.Star, stringResource(R.string.pinned_tab),
            onClick = onTogglePinned,
            selected = pinnedOnly,
        )
        Spacer(Modifier.width(4.dp))
    }
}

/** What "Delete for good" deletes: the selected notes of the Trash, or all of it. */
private enum class ForeverTarget { Selection, All }

/** First row of the Trash: notes go after a week, and a button to empty it now. */
@Composable
private fun TrashHeader(onEmpty: () -> Unit) {
    Row(
        Modifier
            .popIn(initialScale = 0.9f)
            .fillMaxWidth()
            .glass(RoundedCornerShape(24.dp), GlassKind.Card)
            .padding(start = 18.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            pluralStringResource(R.plurals.trash_info, TrashDays, TrashDays),
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onEmpty) { Text(stringResource(R.string.empty_trash), fontWeight = FontWeight.Bold) }
    }
}
