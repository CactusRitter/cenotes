package com.cenotes.app.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.MoreVert
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cenotes.app.ui.theme.ArchiveIcon
import com.cenotes.app.ui.theme.LabelIcon
import com.cenotes.app.ui.theme.LabelOutlineIcon
import com.cenotes.app.ui.theme.UnarchiveIcon
import androidx.compose.runtime.DisposableEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cenotes.app.R
import com.cenotes.app.data.Note
import com.cenotes.app.ui.glass.GlassBackground
import com.cenotes.app.ui.glass.GlassIconButton
import com.cenotes.app.ui.glass.GlassKind
import com.cenotes.app.ui.glass.GlassPill
import com.cenotes.app.ui.glass.PillIcon
import com.cenotes.app.ui.glass.glass
import com.cenotes.app.ui.glass.glassSpring
import com.cenotes.app.ui.markdown.BlockEditorState
import com.cenotes.app.ui.markdown.BlockType
import com.cenotes.app.ui.markdown.EditorBlock
import com.cenotes.app.ui.markdown.LiveInlineMarkdown
import com.cenotes.app.ui.markdown.NestableTypes
import com.cenotes.app.ui.markdown.linkAt
import com.cenotes.app.ui.markdown.markdownToShareText
import com.cenotes.app.ui.markdown.toggleEmphasis
import com.cenotes.app.ui.markdown.toggleWrap
import com.cenotes.app.ui.theme.BulletListIcon
import com.cenotes.app.ui.theme.CameraIcon
import com.cenotes.app.ui.theme.CheckBoxIcon
import com.cenotes.app.ui.theme.ImageIcon
import com.cenotes.app.ui.theme.IndentIcon
import com.cenotes.app.ui.theme.LinkIcon
import com.cenotes.app.ui.theme.LocalGlass
import com.cenotes.app.ui.theme.NumberedListIcon
import com.cenotes.app.ui.theme.OpenInNewIcon
import com.cenotes.app.ui.theme.OutdentIcon
import com.cenotes.app.ui.theme.QuoteIcon
import com.cenotes.app.ui.theme.StarBorderIcon
import kotlinx.coroutines.launch
import java.io.File

/**
 * Edits one note in a visual editor: checkboxes (with sub-tasks) are real checkboxes, bold is
 * bold, links open from a chip, and images sit between the text. Markdown typed by hand still
 * works as a shortcut, and the note is stored as Markdown. [noteId] 0 means a new note.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    noteId: Long,
    viewModel: NotesViewModel,
    onClose: () -> Unit,
    /** Initial Markdown for a new note (e.g. an empty checkbox for "Nueva lista"). */
    template: String = "",
) {
    // All state is keyed by the note id so nothing can leak from one note into another.
    var original by remember(noteId) { mutableStateOf<Note?>(null) }
    var loaded by rememberSaveable(noteId) { mutableStateOf(noteId == 0L) }
    var title by rememberSaveable(noteId) { mutableStateOf("") }
    var savedContent by rememberSaveable(noteId) { mutableStateOf(if (noteId == 0L) template else "") }
    var pinned by rememberSaveable(noteId) { mutableStateOf(false) }
    var archived by rememberSaveable(noteId) { mutableStateOf(false) }
    // A new note created while looking at a label starts with that label.
    var labelIds by rememberSaveable(noteId) {
        mutableStateOf(if (noteId == 0L) listOfNotNull(viewModel.labelForNewNote()) else emptyList())
    }
    var closed by remember(noteId) { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var imageMenuOpen by remember { mutableStateOf(false) }
    var moreMenuOpen by remember { mutableStateOf(false) }
    var pickingLabels by remember { mutableStateOf(false) }
    val allLabels by viewModel.labels.collectAsStateWithLifecycle()
    var viewing by remember { mutableStateOf<File?>(null) }
    var linkTarget by remember { mutableStateOf<Pair<Long, TextRange>?>(null) }
    val editor = remember(noteId) { BlockEditorState(savedContent) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val images = viewModel.images

    LaunchedEffect(noteId) {
        if (noteId == 0L) {
            editor.focusFirst()
            return@LaunchedEffect
        }
        val note = viewModel.load(noteId)
        original = note
        if (!loaded && note != null) {
            title = note.title
            editor.load(note.content)
            pinned = note.pinned
            archived = note.archived
            labelIds = viewModel.labelIdsOf(noteId)
        }
        loaded = true
    }

    // Keep a Markdown copy in saved state so text survives rotation / process death.
    LaunchedEffect(editor) {
        snapshotFlow { editor.toMarkdown() }.collect { savedContent = it }
    }

    // ---- Images: gallery (several at once) and camera ----
    val imageError = stringResource(R.string.image_error)
    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        scope.launch {
            val names = viewModel.importImages(uris)
            if (names.size < uris.size) Toast.makeText(context, imageError, Toast.LENGTH_SHORT).show()
            editor.insertImages(names)
        }
    }
    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) {
        addImages(it)
    }
    var cameraTarget by remember { mutableStateOf<Pair<File, Uri>?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val target = cameraTarget
        if (ok && target != null) {
            scope.launch {
                addImages(listOf(target.second))
            }
        }
    }
    fun openGallery() = pickImages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    fun openCamera() {
        val target = images.newCameraTarget()
        cameraTarget = target
        runCatching { takePicture.launch(target.second) }
    }

    // Guarded so a double back press (or a tap during the exit animation) can't save twice.
    fun saveNow() {
        if (loaded) viewModel.save(original, title, editor.toMarkdown(), pinned, labelIds, archived)
    }

    fun close() {
        if (closed) return
        closed = true
        saveNow()
        onClose()
    }

    /** Archiving (or un-archiving) leaves the note; the list then offers "Deshacer". */
    fun toggleArchived() {
        archived = !archived
        close()
        viewModel.noteArchived(noteId, archived)
    }

    fun share() {
        val text = listOf(title.trim(), markdownToShareText(editor.toMarkdown()).trim())
            .filter { it.isNotEmpty() }
            .joinToString("\n\n")
        val uris = ArrayList(images.shareUris(editor.imageNames()))
        val send = if (uris.isEmpty()) {
            Intent(Intent.ACTION_SEND).apply { type = "text/plain" }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }.apply {
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, null))
    }

    BackHandler { close() }

    // Leaving the editor any other way (a widget or shortcut opens another note, the app is
    // closed) still saves. Not on rotation: the editor comes straight back with its state.
    val activity = LocalActivity.current
    DisposableEffect(noteId) {
        onDispose {
            if (!closed && activity?.isChangingConfigurations != true) {
                closed = true
                saveNow()
            }
        }
    }

    GlassBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().height(72.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), ::close)
                Spacer(Modifier.weight(1f))
                GlassPill {
                    Box {
                        PillIcon(ImageIcon, stringResource(R.string.add_image), { imageMenuOpen = true })
                        ImageSourceMenu(
                            expanded = imageMenuOpen,
                            onDismiss = { imageMenuOpen = false },
                            onGallery = { imageMenuOpen = false; openGallery() },
                            onCamera = { imageMenuOpen = false; openCamera() },
                        )
                    }
                    PillIcon(
                        if (pinned) Icons.Filled.Star else StarBorderIcon,
                        stringResource(if (pinned) R.string.unpin else R.string.pin),
                        onClick = { pinned = !pinned },
                        selected = pinned,
                    )
                    // No highlighted background (it looked like a stray tap). Without labels the icon is
                    // hollow and plain, like the star; with labels it is filled in the first label's
                    // colour. The shape alone tells them apart, even if a label's colour matched the
                    // plain icon colour.
                    val firstLabel = allLabels.firstOrNull { it.id in labelIds }
                    PillIcon(
                        if (firstLabel == null) LabelOutlineIcon else LabelIcon,
                        stringResource(R.string.labels),
                        { pickingLabels = true },
                        tintOverride = firstLabel?.let { labelColor(it) },
                    )
                    Box {
                        PillIcon(Icons.Filled.MoreVert, stringResource(R.string.more_options), { moreMenuOpen = true })
                        DropdownMenu(
                            expanded = moreMenuOpen,
                            onDismissRequest = { moreMenuOpen = false },
                            shape = RoundedCornerShape(20.dp),
                            containerColor = LocalGlass.current.menu,
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(if (archived) R.string.unarchive else R.string.archive)) },
                                leadingIcon = { Icon(if (archived) UnarchiveIcon else ArchiveIcon, null) },
                                onClick = { moreMenuOpen = false; toggleArchived() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.share)) },
                                leadingIcon = { Icon(Icons.Filled.Share, null) },
                                onClick = { moreMenuOpen = false; share() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete)) },
                                leadingIcon = { Icon(Icons.Filled.Delete, null) },
                                onClick = { moreMenuOpen = false; confirmDelete = true },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .glass(RoundedCornerShape(28.dp), if (pinned) GlassKind.Pinned else GlassKind.Card),
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                ) {
                    TitleField(title, { title = it }, onNext = editor::focusFirst)
                    val noteLabels = allLabels.filter { it.id in labelIds }
                    if (noteLabels.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { pickingLabels = true },
                        ) {
                            noteLabels.forEach { LabelChip(it) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    BlockList(editor, images::file, onOpenImage = { viewing = it })
                    // Tapping the empty space under the text continues writing at the end.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 240.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = editor::focusEnd,
                            ),
                    )
                }
            }

            // Cursor on a link → a chip to open it (tapping the text itself keeps editing it).
            val focusedBlock = editor.focused
            val link = focusedBlock?.value?.let { v -> if (v.selection.collapsed) linkAt(v.text, v.selection.start) else null }
            AnimatedVisibility(
                visible = link != null,
                enter = fadeIn(glassSpring()) + expandVertically(glassSpring()),
                exit = fadeOut(glassSpring()) + shrinkVertically(glassSpring()),
            ) {
                var lastLink by remember { mutableStateOf("") }
                if (link != null) lastLink = link
                LinkChip(lastLink)
            }

            AnimatedVisibility(
                visible = focusedBlock != null,
                enter = fadeIn(glassSpring()) + slideInVertically(glassSpring()) { it / 2 },
                exit = fadeOut(glassSpring()) + slideOutVertically(glassSpring()) { it / 2 },
            ) {
                FormatToolbar(
                    editor,
                    onLink = {
                        editor.focused?.let { linkTarget = it.id to it.value.selection }
                    },
                    onImage = ::openGallery,
                )
            }
        }
    }

    viewing?.let { ImageViewer(it) { viewing = null } }

    if (pickingLabels) {
        LabelPickerDialog(
            labels = allLabels,
            selected = labelIds.toSet(),
            onToggle = { id -> labelIds = if (id in labelIds) labelIds - id else labelIds + id },
            onCreate = { name -> viewModel.createLabel(name)?.let { id -> if (id !in labelIds) labelIds = labelIds + id } },
            onDismiss = { pickingLabels = false },
        )
    }

    linkTarget?.let { (id, selection) ->
        val block = editor.blocks.firstOrNull { it.id == id }
        val selected = block?.value?.text?.let {
            val s = minOf(selection.start, selection.end).coerceIn(0, it.length)
            val e = maxOf(selection.start, selection.end).coerceIn(0, it.length)
            it.substring(s, e)
        } ?: ""
        LinkDialog(
            initialLabel = selected,
            onDismiss = { linkTarget = null },
            onConfirm = { label, url ->
                editor.insertLink(id, selection, label, url)
                linkTarget = null
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = LocalGlass.current.menu,
            title = { Text(stringResource(R.string.delete_confirm_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.delete_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    if (!closed) {
                        closed = true
                        original?.let(viewModel::delete)
                        onClose()
                    }
                }) { Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun ImageSourceMenu(expanded: Boolean, onDismiss: () -> Unit, onGallery: () -> Unit, onCamera: () -> Unit) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = LocalGlass.current.menu,
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.from_gallery)) },
            leadingIcon = { Icon(ImageIcon, null) },
            onClick = onGallery,
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.from_camera)) },
            leadingIcon = { Icon(CameraIcon, null) },
            onClick = onCamera,
        )
    }
}

@Composable
private fun LinkChip(url: String) {
    val uriHandler = LocalUriHandler.current
    Row(
        Modifier
            .padding(bottom = 8.dp)
            .fillMaxWidth()
            .height(48.dp)
            .glass(CircleShape, GlassKind.Chrome, elevation = 8.dp)
            .padding(start = 18.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(LinkIcon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            displayHost(url),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { runCatching { uriHandler.openUri(normalizeUrl(url)) } }) {
            Icon(OpenInNewIcon, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.link_open), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TitleField(value: String, onChange: (String) -> Unit, onNext: () -> Unit) {
    val style = MaterialTheme.typography.headlineSmall.copy(
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    BasicTextField(
        value = value,
        onValueChange = { onChange(it.replace("\n", "")) },
        textStyle = style,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(stringResource(R.string.title_hint), style = style.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
                inner()
            }
        },
    )
}

@Composable
private fun BlockList(editor: BlockEditorState, fileFor: (String) -> File, onOpenImage: (File) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val transformation = remember(colors) {
        LiveInlineMarkdown(linkColor = colors.primary, codeBackground = colors.onSurface.copy(alpha = 0.08f))
    }
    val requesters = remember { mutableMapOf<Long, FocusRequester>() }

    LaunchedEffect(editor.pendingFocus) {
        val id = editor.pendingFocus ?: return@LaunchedEffect
        runCatching { requesters[id]?.requestFocus() }
        editor.pendingFocus = null
    }

    val numbers = editor.numberLabels()
    val blocks = editor.blocks.toList()
    Column {
        var i = 0
        while (i < blocks.size) {
            val block = blocks[i]
            when {
                block.type == BlockType.IMAGE && block.small -> {
                    // Consecutive small images form a gallery, three per row.
                    var end = i
                    while (end + 1 < blocks.size && blocks[end + 1].type == BlockType.IMAGE && blocks[end + 1].small) end++
                    val run = blocks.subList(i, end + 1)
                    key(block.id) {
                        Column(Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            run.chunked(3).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { img -> ImageBlock(img, editor, fileFor, onOpenImage, Modifier.weight(1f)) }
                                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                    i = end + 1
                    continue
                }
                block.type == BlockType.IMAGE -> key(block.id) {
                    ImageBlock(block, editor, fileFor, onOpenImage, Modifier.fillMaxWidth().padding(vertical = 6.dp))
                }
                block.type == BlockType.DIVIDER -> key(block.id) {
                    HorizontalDivider(Modifier.padding(vertical = 14.dp))
                }
                // key(): each block keeps its own text field (and keyboard focus) when blocks are
                // inserted or removed around it.
                else -> key(block.id) {
                    BlockRow(
                        block, numbers[block.id] ?: 0, editor,
                        requesters.getOrPut(block.id) { FocusRequester() }, transformation,
                    )
                }
            }
            i++
        }
    }
}

@Composable
private fun ImageBlock(
    block: EditorBlock,
    editor: BlockEditorState,
    fileFor: (String) -> File,
    onOpenImage: (File) -> Unit,
    modifier: Modifier,
) {
    val file = fileFor(block.imageName)
    NoteImage(
        file = file,
        small = block.small,
        onOpen = { onOpenImage(file) },
        onToggleSize = { block.small = !block.small },
        onDelete = { editor.removeImage(block) },
        modifier = modifier,
    )
}

/**
 * One text block. The layout is the same for every type (leading marker + text field) so that
 * turning a line into a checkbox or a heading keeps the very same text field and the keyboard.
 */
@Composable
private fun BlockRow(
    block: EditorBlock,
    number: Int,
    editor: BlockEditorState,
    requester: FocusRequester,
    transformation: LiveInlineMarkdown,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val body = typography.bodyLarge.copy(color = colors.onSurface)
    val type = block.type

    val style = when (type) {
        BlockType.H1 -> typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = colors.onSurface)
        BlockType.H2 -> typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = colors.onSurface)
        BlockType.H3 -> typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = colors.onSurface)
        BlockType.TASK -> if (block.checked) {
            body.copy(color = colors.onSurfaceVariant, textDecoration = TextDecoration.LineThrough)
        } else body
        BlockType.QUOTE -> body.copy(fontStyle = FontStyle.Italic, color = colors.onSurfaceVariant)
        BlockType.CODE -> body.copy(fontFamily = FontFamily.Monospace, fontSize = 15.sp)
        else -> body
    }
    val rowModifier = when (type) {
        BlockType.H1, BlockType.H2, BlockType.H3 -> Modifier.padding(top = 10.dp, bottom = 4.dp)
        BlockType.CODE -> Modifier
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.onSurface.copy(alpha = 0.07f))
            .padding(12.dp)
        BlockType.TASK -> Modifier
        else -> Modifier.padding(vertical = 3.dp)
    }

    Row(
        rowModifier.fillMaxWidth().height(IntrinsicSize.Min),
        verticalAlignment = if (type == BlockType.TASK) Alignment.CenterVertically else Alignment.Top,
    ) {
        if (type in NestableTypes) Spacer(Modifier.width((block.indent * 28).dp))
        when (type) {
            BlockType.TASK -> CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                Checkbox(
                    checked = block.checked,
                    onCheckedChange = { editor.toggleChecked(block) },
                    colors = CheckboxDefaults.colors(checkedColor = colors.primary),
                    modifier = Modifier.padding(end = 12.dp, top = 6.dp, bottom = 6.dp),
                )
            }
            BlockType.BULLET, BlockType.NUMBERED -> Text(
                if (type == BlockType.BULLET) listOf("•", "◦", "▪")[block.indent % 3] else "$number.",
                style = body.copy(color = colors.primary, fontWeight = FontWeight.Bold),
                modifier = Modifier.width(28.dp),
            )
            BlockType.QUOTE -> {
                Box(
                    Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.primary.copy(alpha = 0.6f)),
                )
                Spacer(Modifier.width(12.dp))
            }
            else -> Unit
        }
        BlockField(
            block, editor, requester, transformation, style, Modifier.weight(1f),
            placeholder = if (editor.blocks.size == 1 && type == BlockType.PARAGRAPH) {
                stringResource(R.string.content_hint)
            } else null,
        )
    }
}

@Composable
private fun BlockField(
    block: EditorBlock,
    editor: BlockEditorState,
    requester: FocusRequester,
    transformation: LiveInlineMarkdown,
    style: TextStyle,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
) {
    BasicTextField(
        value = block.value,
        onValueChange = { editor.onValueChange(block, it) },
        textStyle = style,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        visualTransformation = transformation,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = modifier
            .focusRequester(requester)
            .onFocusChanged { editor.onFocus(block, it.isFocused) }
            .onPreviewKeyEvent { event ->
                event.type == KeyEventType.KeyDown && event.key == Key.Backspace && editor.onBackspaceAtStart(block)
            },
        decorationBox = { inner ->
            Box {
                if (placeholder != null && block.value.text.isEmpty()) {
                    Text(placeholder, style = style.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
                inner()
            }
        },
    )
}

/** Glass formatting bar that sits above the keyboard while a line is being edited. */
@Composable
private fun FormatToolbar(editor: BlockEditorState, onLink: () -> Unit, onImage: () -> Unit) {
    val focused = editor.focused
    val type = focused?.type
    Row(
        Modifier
            .padding(bottom = 12.dp)
            .fillMaxWidth()
            .height(56.dp)
            .glass(CircleShape, GlassKind.Chrome, elevation = 10.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolIcon(CheckBoxIcon, stringResource(R.string.fmt_checklist), type == BlockType.TASK) {
            editor.toggleType(BlockType.TASK)
        }
        // Sub-tasks: only offered on list lines.
        if (type in NestableTypes) {
            ToolIcon(IndentIcon, stringResource(R.string.fmt_indent), false, enabled = editor.canIndent(focused)) {
                editor.indentFocused()
            }
            ToolIcon(OutdentIcon, stringResource(R.string.fmt_outdent), false, enabled = (focused?.indent ?: 0) > 0) {
                editor.outdentFocused()
            }
        }
        ToolIcon(BulletListIcon, stringResource(R.string.fmt_bullets), type == BlockType.BULLET) {
            editor.toggleType(BlockType.BULLET)
        }
        ToolIcon(NumberedListIcon, stringResource(R.string.fmt_numbered), type == BlockType.NUMBERED) {
            editor.toggleType(BlockType.NUMBERED)
        }
        ToolText(
            when (type) { BlockType.H1 -> "H1"; BlockType.H2 -> "H2"; BlockType.H3 -> "H3"; else -> "H" },
            stringResource(R.string.fmt_heading),
            selected = type == BlockType.H1 || type == BlockType.H2 || type == BlockType.H3,
            fontWeight = FontWeight.Black,
        ) { editor.cycleHeading() }
        ToolText("B", stringResource(R.string.fmt_bold), fontWeight = FontWeight.Black) {
            editor.editFocused { toggleEmphasis(it, bold = true) }
        }
        ToolText("I", stringResource(R.string.fmt_italic), fontStyle = FontStyle.Italic) {
            editor.editFocused { toggleEmphasis(it, bold = false) }
        }
        ToolText("S", stringResource(R.string.fmt_strike), decoration = TextDecoration.LineThrough) {
            editor.editFocused { toggleWrap(it, "~~") }
        }
        ToolIcon(LinkIcon, stringResource(R.string.fmt_link), false, onClick = onLink)
        ToolIcon(ImageIcon, stringResource(R.string.add_image), false, onClick = onImage)
        ToolText("</>", stringResource(R.string.fmt_code), monospace = true) {
            editor.editFocused { toggleWrap(it, "`") }
        }
        ToolIcon(QuoteIcon, stringResource(R.string.fmt_quote), type == BlockType.QUOTE) {
            editor.toggleType(BlockType.QUOTE)
        }
    }
}

@Composable
private fun ToolIcon(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(Modifier.then(if (enabled) Modifier else Modifier.semantics { }), contentAlignment = Alignment.Center) {
        PillIcon(icon, label, { if (enabled) onClick() }, selected = selected, width = 48.dp)
        if (!enabled) {
            // Dim disabled tools without changing the bar's layout.
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(LocalGlass.current.chrome.copy(alpha = 0.6f)),
            )
        }
    }
}

@Composable
private fun ToolText(
    text: String,
    label: String,
    selected: Boolean = false,
    fontWeight: FontWeight = FontWeight.Medium,
    fontStyle: FontStyle = FontStyle.Normal,
    decoration: TextDecoration = TextDecoration.None,
    monospace: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(width = 48.dp, height = 48.dp)
            .clip(CircleShape)
            .background(if (selected) colors.primaryContainer else colors.primaryContainer.copy(alpha = 0f))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            textDecoration = decoration,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            fontSize = if (monospace) 15.sp else 19.sp,
            color = if (selected) colors.primary else colors.onSurface,
        )
    }
}
