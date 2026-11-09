package com.example.notas.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.notas.R
import com.example.notas.data.Note
import com.example.notas.ui.glass.GlassBackground
import com.example.notas.ui.glass.GlassIconButton
import com.example.notas.ui.glass.GlassKind
import com.example.notas.ui.glass.GlassPill
import com.example.notas.ui.glass.PillIcon
import com.example.notas.ui.glass.glass
import com.example.notas.ui.theme.LocalGlass
import com.example.notas.ui.theme.StarBorderIcon

/**
 * Edits one note. [noteId] 0 means a new note. Changes are saved when leaving the screen
 * (back button or system back); an empty note is simply discarded.
 */
@Composable
fun NoteEditorScreen(
    noteId: Long,
    viewModel: NotesViewModel,
    onClose: () -> Unit,
) {
    var original by remember { mutableStateOf<Note?>(null) }
    // Saveable so text survives rotation / process death; `loaded` stops a reload from overwriting it.
    var loaded by rememberSaveable { mutableStateOf(noteId == 0L) }
    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var pinned by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val contentFocus = remember { FocusRequester() }

    LaunchedEffect(noteId) {
        if (noteId == 0L) {
            contentFocus.requestFocus()
            return@LaunchedEffect
        }
        val note = viewModel.load(noteId)
        original = note
        if (!loaded && note != null) {
            title = note.title
            content = note.content
            pinned = note.pinned
        }
        loaded = true
    }

    fun close() {
        if (loaded) viewModel.save(original, title, content, pinned)
        onClose()
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
                    PillIcon(Icons.Filled.Delete, stringResource(R.string.delete), { confirmDelete = true })
                }
            }
            Spacer(Modifier.height(8.dp))
            val fieldColors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
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
                    TextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = { Text(stringResource(R.string.content_hint)) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        colors = fieldColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 320.dp)
                            .focusRequester(contentFocus),
                    )
                }
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
                    original?.let(viewModel::delete)
                    onClose()
                }) { Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
