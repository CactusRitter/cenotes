package com.example.notas.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
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
import com.example.notas.ui.markdown.LineFormat
import com.example.notas.ui.markdown.MarkdownHighlighter
import com.example.notas.ui.markdown.MarkdownView
import com.example.notas.ui.markdown.continueList
import com.example.notas.ui.markdown.markdownToShareText
import com.example.notas.ui.markdown.toggleLineFormat
import com.example.notas.ui.markdown.toggleTask
import com.example.notas.ui.markdown.toggleWrap
import com.example.notas.ui.theme.BulletListIcon
import com.example.notas.ui.theme.CheckBoxIcon
import com.example.notas.ui.theme.LocalGlass
import com.example.notas.ui.theme.NumberedListIcon
import com.example.notas.ui.theme.QuoteIcon
import com.example.notas.ui.theme.StarBorderIcon
import com.example.notas.ui.theme.VisibilityIcon

/**
 * Edits one note. [noteId] 0 means a new note. Existing notes open in "view" mode (rendered
 * Markdown with tappable checkboxes); the pencil switches to "edit" mode with a formatting bar.
 * Changes are saved once, when leaving the screen; an empty note is simply discarded.
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
    var content by rememberSaveable(noteId, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var pinned by rememberSaveable(noteId) { mutableStateOf(false) }
    var editing by rememberSaveable(noteId) { mutableStateOf(noteId == 0L) }
    var closed by remember(noteId) { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val contentFocus = remember { FocusRequester() }
    val context = LocalContext.current

    LaunchedEffect(noteId) {
        if (noteId == 0L) return@LaunchedEffect
        val note = viewModel.load(noteId)
        original = note
        if (!loaded && note != null) {
            title = note.title
            content = TextFieldValue(note.content, TextRange(note.content.length))
            pinned = note.pinned
            editing = note.content.isBlank()
        }
        loaded = true
    }

    val focusManager = LocalFocusManager.current
    LaunchedEffect(editing) {
        if (editing) runCatching { contentFocus.requestFocus() } else focusManager.clearFocus()
    }

    // Guarded so a double back press (or a tap during the exit animation) can't save twice.
    fun close() {
        if (closed) return
        closed = true
        if (loaded) viewModel.save(original, title, content.text, pinned)
        onClose()
    }

    fun share() {
        val text = listOf(title.trim(), markdownToShareText(content.text).trim())
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
                        if (editing) VisibilityIcon else Icons.Filled.Edit,
                        stringResource(if (editing) R.string.view_mode else R.string.edit_mode),
                        onClick = { editing = !editing },
                    )
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

            val colors = MaterialTheme.colorScheme
            val fieldColors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            )
            val highlighter = remember(colors) {
                MarkdownHighlighter(
                    marker = colors.onSurfaceVariant.copy(alpha = 0.6f),
                    accent = colors.primary,
                    muted = colors.onSurfaceVariant,
                    codeBackground = colors.onSurface.copy(alpha = 0.08f),
                )
            }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .glass(RoundedCornerShape(28.dp), if (pinned) GlassKind.Pinned else GlassKind.Card),
            ) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)) {
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = {
                            Text(stringResource(R.string.title_hint), style = MaterialTheme.typography.headlineSmall)
                        },
                        textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (editing) {
                        TextField(
                            value = content,
                            onValueChange = { content = continueList(content, it) },
                            placeholder = { Text(stringResource(R.string.content_hint)) },
                            textStyle = MaterialTheme.typography.bodyLarge,
                            visualTransformation = highlighter,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            colors = fieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 320.dp)
                                .focusRequester(contentFocus),
                        )
                    } else {
                        // Tapping anywhere that isn't a checkbox or link switches to editing.
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 320.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { editing = true }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            if (content.text.isBlank()) {
                                Text(
                                    stringResource(R.string.tap_to_write),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.onSurfaceVariant,
                                )
                            } else {
                                MarkdownView(
                                    source = content.text,
                                    onToggleTask = { line -> content = content.copy(text = toggleTask(content.text, line)) },
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = editing,
                enter = fadeIn(glassSpring()) + slideInVertically(glassSpring()) { it / 2 },
                exit = fadeOut(glassSpring()) + slideOutVertically(glassSpring()) { it / 2 },
            ) {
                FormatToolbar(onEdit = { transform -> content = transform(content) })
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

/** Glass formatting bar that sits above the keyboard while editing. */
@Composable
private fun FormatToolbar(onEdit: ((TextFieldValue) -> TextFieldValue) -> Unit) {
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
        ToolIcon(CheckBoxIcon, stringResource(R.string.fmt_checklist)) { onEdit { toggleLineFormat(it, LineFormat.TASK) } }
        ToolIcon(BulletListIcon, stringResource(R.string.fmt_bullets)) { onEdit { toggleLineFormat(it, LineFormat.BULLET) } }
        ToolIcon(NumberedListIcon, stringResource(R.string.fmt_numbered)) { onEdit { toggleLineFormat(it, LineFormat.NUMBERED) } }
        ToolText("H", stringResource(R.string.fmt_heading), fontWeight = FontWeight.Black) {
            onEdit { toggleLineFormat(it, LineFormat.HEADING) }
        }
        ToolText("B", stringResource(R.string.fmt_bold), fontWeight = FontWeight.Black) { onEdit { toggleWrap(it, "**") } }
        ToolText("I", stringResource(R.string.fmt_italic), fontStyle = FontStyle.Italic) { onEdit { toggleWrap(it, "*") } }
        ToolText("S", stringResource(R.string.fmt_strike), decoration = TextDecoration.LineThrough) {
            onEdit { toggleWrap(it, "~~") }
        }
        ToolText("</>", stringResource(R.string.fmt_code), monospace = true) { onEdit { toggleWrap(it, "`") } }
        ToolIcon(QuoteIcon, stringResource(R.string.fmt_quote)) { onEdit { toggleLineFormat(it, LineFormat.QUOTE) } }
    }
}

@Composable
private fun ToolIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    PillIcon(icon, label, onClick, width = 48.dp)
}

@Composable
private fun ToolText(
    text: String,
    label: String,
    fontWeight: FontWeight = FontWeight.Medium,
    fontStyle: FontStyle = FontStyle.Normal,
    decoration: TextDecoration = TextDecoration.None,
    monospace: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(width = 48.dp, height = 48.dp)
            .clip(CircleShape)
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
        )
    }
}
