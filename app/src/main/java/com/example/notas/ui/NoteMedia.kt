package com.example.notas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.example.notas.R
import com.example.notas.ui.glass.GlassIconButton
import com.example.notas.ui.glass.GlassKind
import com.example.notas.ui.glass.glass
import com.example.notas.ui.theme.LocalGlass
import java.io.File

/**
 * An image inside a note. Large images use the full width but never get taller than
 * [MaxImageHeight] (they are cropped, and open in full in the viewer); small ones are square
 * thumbnails that line up three per row.
 */
@Composable
fun NoteImage(
    file: File,
    small: Boolean,
    onOpen: () -> Unit,
    onToggleSize: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(if (small) 16.dp else 20.dp)
    val glass = LocalGlass.current
    Box(modifier.clip(shape).border(1.dp, glass.borderTop, shape)) {
        AsyncImage(
            model = file,
            contentDescription = stringResource(R.string.image),
            contentScale = ContentScale.Crop,
            modifier = (if (small) Modifier.fillMaxWidth().aspectRatio(1f) else Modifier.fillMaxWidth().heightIn(max = MaxImageHeight))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                .clickable(onClick = onOpen),
        )
        Box(Modifier.align(Alignment.TopEnd).padding(6.dp)) {
            Box(
                Modifier
                    .size(if (small) 28.dp else 34.dp)
                    .glass(CircleShape, GlassKind.Chrome)
                    .clickable { menuOpen = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.MoreVert, stringResource(R.string.image_options), Modifier.size(18.dp))
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                shape = RoundedCornerShape(20.dp),
                containerColor = glass.menu,
            ) {
                DropdownMenuItem(text = { Text(stringResource(R.string.image_view)) }, onClick = { menuOpen = false; onOpen() })
                DropdownMenuItem(
                    text = { Text(stringResource(if (small) R.string.image_make_large else R.string.image_make_small)) },
                    onClick = { menuOpen = false; onToggleSize() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                    onClick = { menuOpen = false; onDelete() },
                )
            }
        }
    }
}

val MaxImageHeight = 340.dp

/** Full-screen image with pinch-to-zoom and double-tap to zoom. */
@Composable
fun ImageViewer(file: File, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    })
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        offset = if (scale == 1f) Offset.Zero else offset + pan
                    }
                },
        ) {
            AsyncImage(
                model = file,
                contentDescription = stringResource(R.string.image),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
            )
            GlassIconButton(
                Icons.Filled.Close,
                stringResource(R.string.close),
                onDismiss,
                Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp),
            )
        }
    }
}

/** Asks for the text and address of a link. */
@Composable
fun LinkDialog(initialLabel: String, onDismiss: () -> Unit, onConfirm: (label: String, url: String) -> Unit) {
    var label by remember { mutableStateOf(initialLabel) }
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = LocalGlass.current.menu,
        title = { Text(stringResource(R.string.link_add), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.link_text)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it.trim() },
                    label = { Text(stringResource(R.string.link_url)) },
                    placeholder = { Text("https://…") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    shape = RoundedCornerShape(16.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = url.isNotBlank(),
                onClick = { onConfirm(label.trim(), normalizeUrl(url)) },
            ) { Text(stringResource(R.string.link_insert), fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

fun normalizeUrl(url: String): String =
    if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(url)) url else "https://$url"

/** "https://www.example.com/a/b" → "example.com" for the link chip. */
fun displayHost(url: String): String =
    url.substringAfter("://").substringBefore('/').removePrefix("www.").ifBlank { url }
