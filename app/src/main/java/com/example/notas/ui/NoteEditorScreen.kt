package com.example.notas.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notas.R
import com.example.notas.data.Note
import com.example.notas.ui.glass.GlassBackground
import com.example.notas.ui.glass.GlassIconButton
import com.example.notas.ui.glass.GlassKind
import com.example.notas.ui.glass.GlassPill
import com.example.notas.ui.glass.PillIcon
import com.example.notas.ui.glass.glass
import com.example.notas.ui.glass.glassSpring
import com.example.notas.ui.markdown.BlockEditorState
import com.example.notas.ui.markdown.BlockType
import com.example.notas.ui.markdown.EditorBlock
import com.example.notas.ui.markdown.LiveInlineMarkdown
import com.example.notas.ui.markdown.markdownToShareText
import com.example.notas.ui.markdown.toggleEmphasis
import com.example.notas.ui.markdown.toggleWrap
import com.example.notas.ui.theme.BulletListIcon
import com.example.notas.ui.theme.CheckBoxIcon
import com.example.notas.ui.theme.LocalGlass
import com.example.notas.ui.theme.NumberedListIcon
import com.example.notas.ui.theme.QuoteIcon
import com.example.notas.ui.theme.StarBorderIcon

/**
 * Edits one note in a visual editor: checkboxes are real checkboxes, bold is bold, and so on.
 * Markdown typed by hand ("- [ ] ", "# ", "**x**") turns into formatting as you type, and the
 * note is still stored as Markdown. [noteId] 0 means a new note. It is saved once, on leaving.
 */
@Composable
fun NoteEditorScreen(
    noteId: Long,
    viewModel: NotesViewModel,
    onClose: () -> Unit,
) {
    // All state is keyed by the note id so nothing can leak from one note into another.
    var original by remember(noteId) { mutableStateOf<Note?>(null) }
    var loaded by rememberSaveable(noteId) { mutableStateOf(noteId == 0L) }
    var title by rememberSaveable(noteId) { mutableStateOf("") }
    var savedContent by rememberSaveable(noteId) { mutableStateOf("") }
    var pinned by rememberSaveable(noteId) { mutableStateOf(false) }
    var closed by remember(noteId) { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val editor = remember(noteId) { BlockEditorState(savedContent) }
    val context = LocalContext.current

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
        }
        loaded = true
    }

    // Keep a Markdown copy in saved state so text survives rotation / process death.
    LaunchedEffect(editor) {
        snapshotFlow { editor.toMarkdown() }.collect { savedContent = it }
    }

    // Guarded so a double back press (or a tap during the exit animation) can't save twice.
    fun close() {
        if (closed) return
        closed = true
        if (loaded) viewModel.save(original, title, editor.toMarkdown(), pinned)
        onClose()
    }

    fun share() {
        val text = listOf(title.trim(), markdownToShareText(editor.toMarkdown()).trim())
            .filter { it.isNotEmpty() }
            .joinToString("\n\n")
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, null))
    }

    BackHandler { close() }

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
                    PillIcon(
                        if (pinned) Icons.Filled.Star else StarBorderIcon,
                        stringResource(if (pinned) R.string.unpin else R.string.pin),
                        onClick = { pinned = !pinned },
                        selected = pinned,
                    )
                    PillIcon(Icons.Filled.Share, stringResource(R.string.share), ::share)
                    PillIcon(Icons.Filled.Delete, stringResource(R.string.delete), { confirmDelete = true })
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
                    Spacer(Modifier.height(12.dp))
                    BlockList(editor)
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

            AnimatedVisibility(
                visible = editor.focusedId != null,
                enter = fadeIn(glassSpring()) + slideInVertically(glassSpring()) { it / 2 },
                exit = fadeOut(glassSpring()) + slideOutVertically(glassSpring()) { it / 2 },
            ) {
                FormatToolbar(editor)
            }
        }
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
private fun BlockList(editor: BlockEditorState) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val transformation = remember(colors) {
        LiveInlineMarkdown(linkColor = colors.primary, codeBackground = colors.onSurface.copy(alpha = 0.08f))
    }
    val requesters = remember { mutableMapOf<Long, FocusRequester>() }

    LaunchedEffect(editor.pendingFocus) {
        val id = editor.pendingFocus ?: return@LaunchedEffect
        runCatching { requesters[id]?.requestFocus() }
        editor.pendingFocus = null
    }

    var number = 0
    Column {
        editor.blocks.forEach { block ->
            number = if (block.type == BlockType.NUMBERED) number + 1 else 0
            val label = number
            // key(): each block keeps its own text field (and keyboard focus) when blocks are
            // inserted or removed around it.
            key(block.id) {
                if (block.type == BlockType.DIVIDER) {
                    HorizontalDivider(Modifier.padding(vertical = 14.dp))
                } else {
                    BlockRow(block, label, editor, requesters.getOrPut(block.id) { FocusRequester() }, transformation)
                }
            }
        }
    }
}

/**
 * One block. The layout is the same for every type (leading marker + text field) so that turning
 * a line into a checkbox or a heading keeps the very same text field and the keyboard stays open.
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
        if (type == BlockType.TASK || type == BlockType.BULLET || type == BlockType.NUMBERED) {
            Spacer(Modifier.width((block.indent * 24).dp))
        }
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
                if (type == BlockType.BULLET) "•" else "$number.",
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
            .onFocusChanged { state ->
                if (state.isFocused) editor.focusedId = block.id
                else if (editor.focusedId == block.id) editor.focusedId = null
            }
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
private fun FormatToolbar(editor: BlockEditorState) {
    val type = editor.focused?.type
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
        ToolText("</>", stringResource(R.string.fmt_code), monospace = true) {
            editor.editFocused { toggleWrap(it, "`") }
        }
        ToolIcon(QuoteIcon, stringResource(R.string.fmt_quote), type == BlockType.QUOTE) {
            editor.toggleType(BlockType.QUOTE)
        }
    }
}

@Composable
private fun ToolIcon(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    PillIcon(icon, label, onClick, selected = selected, width = 48.dp)
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
