package com.cenotes.app.ui

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import com.cenotes.app.ui.markdown.MarkdownPreview
import com.cenotes.app.ui.markdown.firstImage
import com.cenotes.app.ui.markdown.taskStats
import com.cenotes.app.ui.theme.ArchiveIcon
import com.cenotes.app.ui.theme.CheckBoxIcon
import com.cenotes.app.ui.theme.GridViewIcon
import com.cenotes.app.ui.theme.LabelIcon
import com.cenotes.app.ui.theme.LocalGlass
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
    val labelCounts by viewModel.labelCounts.collectAsStateWithLifecycle()
    val section by viewModel.section.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val pinnedOnly by viewModel.pinnedOnly.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val undo by viewModel.undo.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val hazeState = rememberHazeState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var searchOpen by rememberSaveable { mutableStateOf(query.isNotEmpty()) }
    var bulkLabels by remember { mutableStateOf(false) }
    var manageLabels by remember { mutableStateOf(false) }
    LaunchedEffect(searchRequest) { if (searchRequest > 0) searchOpen = true }

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
        is Section.ByLabel -> labels.firstOrNull { it.id == s.labelId }?.name ?: stringResource(R.string.app_name)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !selecting,
        drawerContent = {
            SideMenu(
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
                        items(list, key = { it.note.id }) { item ->
                            val id = item.note.id
                            NoteCard(
                                item = item,
                                maxLines = if (layout == NoteLayout.GRID) 8 else 4,
                                selected = id in selected,
                                // While selecting, a tap adds/removes; otherwise it opens the note.
                                onClick = { if (selecting) viewModel.toggleSelected(id) else onOpenNote(id) },
                                onLongClick = { viewModel.toggleSelected(id) },
                                modifier = Modifier.animateItem(
                                    fadeInSpec = glassSpring(), placementSpec = glassSpring(), fadeOutSpec = glassSpring(),
                                ),
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
                        hazeState = hazeState,
                        onClose = viewModel::clearSelection,
                        onPin = viewModel::pinSelected,
                        onLabel = { bulkLabels = true },
                        onArchive = { viewModel.archiveSelected(section != Section.Archive) },
                        onDelete = viewModel::deleteSelected,
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
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 76.dp),
                        )
                        GlassPill(Modifier.align(Alignment.CenterEnd), hazeState = hazeState) {
                            PillIcon(
                                Icons.Filled.Search,
                                stringResource(R.string.search),
                                onClick = {
                                    if (searchOpen) viewModel.setQuery("")
                                    searchOpen = !searchOpen
                                },
                                selected = searchOpen,
                            )
                            PillIcon(Icons.Filled.Add, stringResource(R.string.new_note), onNewNote)
                        }
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

            // Bottom glass pill: layout and the "Fijadas" filter.
            GlassPill(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                height = 64.dp,
                hazeState = hazeState,
            ) {
                Spacer(Modifier.width(4.dp))
                PillIcon(
                    GridViewIcon, stringResource(R.string.layout_grid),
                    onClick = { onLayoutChange(NoteLayout.GRID) },
                    selected = layout == NoteLayout.GRID, width = 64.dp,
                )
                PillIcon(
                    ViewAgendaIcon, stringResource(R.string.layout_list),
                    onClick = { onLayoutChange(NoteLayout.LIST) },
                    selected = layout == NoteLayout.LIST, width = 64.dp,
                )
                Box(
                    Modifier
                        .padding(horizontal = 10.dp)
                        .size(width = 1.dp, height = 32.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
                )
                PillIcon(
                    Icons.Filled.Star, stringResource(R.string.pinned_tab),
                    onClick = { viewModel.setPinnedOnly(!pinnedOnly) },
                    selected = pinnedOnly, width = 64.dp,
                )
                Spacer(Modifier.width(4.dp))
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
            onDelete = viewModel::deleteLabel,
            onDismiss = { manageLabels = false },
        )
    }
}

/** Replaces the header while notes are selected: count, then pin / label / archive / delete. */
@Composable
private fun SelectionBar(
    count: Int,
    archiveSection: Boolean,
    hazeState: HazeState,
    onClose: () -> Unit,
    onPin: () -> Unit,
    onLabel: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().height(HeaderHeight), verticalAlignment = Alignment.CenterVertically) {
        GlassIconButton(Icons.Filled.Close, stringResource(R.string.cancel_selection), onClose, hazeState = hazeState)
        Spacer(Modifier.width(14.dp))
        Text(
            stringResource(R.string.selected_count, count),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.weight(1f))
        GlassPill(hazeState = hazeState) {
            PillIcon(Icons.Filled.Star, stringResource(R.string.pin_selected), onPin)
            PillIcon(LabelIcon, stringResource(R.string.labels), onLabel)
            PillIcon(
                if (archiveSection) UnarchiveIcon else ArchiveIcon,
                stringResource(if (archiveSection) R.string.unarchive else R.string.archive),
                onArchive,
            )
            PillIcon(Icons.Filled.Delete, stringResource(R.string.delete), onDelete)
        }
    }
}

/** Side menu: all notes, each label with its count, the archive, then label editing and settings. */
@Composable
private fun SideMenu(
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
    ModalDrawerSheet(
        drawerContainerColor = LocalGlass.current.menu,
        drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
        ) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 28.dp, bottom = 16.dp),
            )
            MenuItem(NotesIcon, stringResource(R.string.section_notes), section == Section.All, colors) {
                onSection(Section.All)
            }
            if (labels.isNotEmpty()) {
                Text(
                    stringResource(R.string.labels),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp),
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
                    shape = CircleShape,
                )
            }
            MenuItem(Icons.Filled.Edit, stringResource(R.string.labels_edit), false, colors, onEditLabels)
            HorizontalDivider(Modifier.padding(vertical = 10.dp, horizontal = 16.dp))
            MenuItem(ArchiveIcon, stringResource(R.string.section_archive), section == Section.Archive, colors) {
                onSection(Section.Archive)
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
        shape = CircleShape,
    )
}

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
) {
    val note = item.note
    val shape = RoundedCornerShape(24.dp)
    val context = LocalContext.current
    val cover = remember(note.content) { firstImage(note.content) }
    val primary = MaterialTheme.colorScheme.primary
    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .glass(shape, if (note.pinned) GlassKind.Pinned else GlassKind.Card)
                .then(if (selected) Modifier.border(2.5.dp, primary, shape) else Modifier)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        ) {
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
                                .clip(CircleShape)
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
