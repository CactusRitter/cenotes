package com.cenotes.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cenotes.app.R
import com.cenotes.app.data.Label
import com.cenotes.app.ui.theme.Accent
import com.cenotes.app.ui.theme.LocalGlass
import kotlinx.coroutines.launch

/** The colour the user picked, or (automatic) one from the accent palette by id, tuned for light and dark. */
@Composable
fun labelColor(label: Label): Color {
    if (label.color != 0) return Color(label.color)
    val accent = Accent.entries[(label.id % Accent.entries.size).toInt()]
    return if (LocalGlass.current.dark) accent.dark else accent.light
}

/** Small coloured chip with the label's name. */
@Composable
fun LabelChip(label: Label, modifier: Modifier = Modifier) {
    val color = labelColor(label)
    Row(
        modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(
            label.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Text field + button to create a label; [onCreate] returns once it exists. */
@Composable
private fun NewLabelField(onCreate: suspend (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    fun submit() {
        val n = name.trim()
        if (n.isEmpty()) return
        name = ""
        scope.launch { onCreate(n) }
    }
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        placeholder = { Text(stringResource(R.string.label_new_hint)) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        trailingIcon = {
            IconButton(onClick = ::submit, enabled = name.isNotBlank()) {
                Icon(Icons.Filled.Add, stringResource(R.string.label_create))
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun LabelRow(label: Label, onClick: () -> Unit, trailing: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(labelColor(label)))
        Spacer(Modifier.width(12.dp))
        Text(label.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        trailing()
    }
}

/** Labels of one note (editor): tick the ones it should have, or create a new one. */
@Composable
fun LabelPickerDialog(
    labels: List<Label>,
    selected: Set<Long>,
    onToggle: (Long) -> Unit,
    onCreate: suspend (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = LocalGlass.current.menu,
        title = { Text(stringResource(R.string.labels), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                NewLabelField(onCreate)
                Spacer(Modifier.padding(4.dp))
                Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState())) {
                    if (labels.isEmpty()) {
                        Text(
                            stringResource(R.string.labels_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    labels.forEach { label ->
                        LabelRow(label, onClick = { onToggle(label.id) }) {
                            Checkbox(checked = label.id in selected, onCheckedChange = { onToggle(label.id) })
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done), fontWeight = FontWeight.Bold) } },
    )
}

/**
 * Labels for several notes at once. Each label shows whether all, some or none of the
 * selected notes have it; tapping applies it to all (or removes it from all).
 */
@Composable
fun BulkLabelDialog(
    labels: List<Label>,
    counts: Map<Long, Int>,
    total: Int,
    onSet: (labelId: Long, add: Boolean) -> Unit,
    onCreate: suspend (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = LocalGlass.current.menu,
        title = { Text(stringResource(R.string.labels_for_n, total), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                NewLabelField(onCreate)
                Spacer(Modifier.padding(4.dp))
                Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState())) {
                    labels.forEach { label ->
                        val n = counts[label.id] ?: 0
                        val state = when (n) {
                            0 -> ToggleableState.Off
                            total -> ToggleableState.On
                            else -> ToggleableState.Indeterminate
                        }
                        val apply = { onSet(label.id, state != ToggleableState.On) }
                        LabelRow(label, onClick = apply) {
                            TriStateCheckbox(state = state, onClick = apply)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done), fontWeight = FontWeight.Bold) } },
    )
}

/** Create, rename and delete labels. Deleting a label never deletes notes. */
@Composable
fun ManageLabelsDialog(
    labels: List<Label>,
    onCreate: suspend (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onColor: (Long, Int) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf<Label?>(null) }
    var colorFor by remember { mutableStateOf<Label?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = LocalGlass.current.menu,
        title = { Text(stringResource(R.string.labels_edit), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                NewLabelField(onCreate)
                Spacer(Modifier.padding(4.dp))
                Column(
                    Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    labels.forEach { label ->
                        var name by remember(label.id, label.name) { mutableStateOf(label.name) }
                        val changed = name.trim().isNotEmpty() && name.trim() != label.name
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = {
                                Box(
                                    Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(labelColor(label))
                                        .clickable { colorFor = label },
                                )
                            },
                            trailingIcon = {
                                if (changed) {
                                    IconButton(onClick = { onRename(label.id, name) }) {
                                        Icon(Icons.Filled.Check, stringResource(R.string.label_rename))
                                    }
                                } else {
                                    IconButton(onClick = { confirmDelete = label }) {
                                        Icon(Icons.Filled.Delete, stringResource(R.string.delete))
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { if (changed) onRename(label.id, name) }),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done), fontWeight = FontWeight.Bold) } },
    )
    // Look the label up again so the dialog shows the colour as soon as it changes.
    labels.firstOrNull { it.id == colorFor?.id }?.let { label ->
        LabelColorDialog(
            label = label,
            onPick = { onColor(label.id, it) },
            onDismiss = { colorFor = null },
        )
    }
    confirmDelete?.let { label ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = LocalGlass.current.menu,
            title = { Text(stringResource(R.string.label_delete_title, label.name), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.label_delete_text)) },
            confirmButton = {
                TextButton(onClick = { onDelete(label.id); confirmDelete = null }) {
                    Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** Ready-made label colours (ARGB). */
private val PresetColors = listOf(
    0xFFE5484D, 0xFFF5873B, 0xFFF2B632, 0xFF3FAE5A, 0xFF2BB5A0,
    0xFF29A8D8, 0xFF4C7DF0, 0xFF8B5CF6, 0xFFE8559E, 0xFF8A94A6,
).map { it.toInt() }

/** Pick one of ten ready-made colours, or build any colour with hue / saturation / brightness bars. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabelColorDialog(label: Label, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val current = labelColor(label)
    val hsv = remember(label.id) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(current.toArgb(), it) }
    }
    var custom by remember { mutableStateOf(label.color != 0 && label.color !in PresetColors) }
    var hue by remember { mutableStateOf(hsv[0]) }
    var sat by remember { mutableStateOf(hsv[1]) }
    var bri by remember { mutableStateOf(hsv[2]) }
    val picked = Color.hsv(hue, sat, bri)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = LocalGlass.current.menu,
        title = { Text(label.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = 5,
                ) {
                    PresetColors.forEach { argb ->
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .clickable { onPick(argb); onDismiss() },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (label.color == argb) Icon(Icons.Filled.Check, null, tint = Color.White)
                        }
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { custom = !custom }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Brush.sweepGradient(List(7) { Color.hsv(it * 60f, 1f, 1f) })),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.color_custom), Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    if (label.color != 0 && label.color !in PresetColors) {
                        Box(Modifier.size(20.dp).clip(CircleShape).background(Color(label.color)))
                    }
                }
                if (custom) {
                    GradientBar(hue / 360f, List(7) { Color.hsv(it * 60f, 1f, 1f) }, picked) { hue = it * 360f }
                    GradientBar(sat, listOf(Color.hsv(hue, 0f, bri), Color.hsv(hue, 1f, bri)), picked) { sat = it }
                    GradientBar(bri, listOf(Color.hsv(hue, sat, 0f), Color.hsv(hue, sat, 1f)), picked) { bri = it }
                    Box(Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(12.dp)).background(picked))
                }
            }
        },
        confirmButton = {
            if (custom) {
                TextButton(onClick = { onPick(picked.toArgb()); onDismiss() }) {
                    Text(stringResource(R.string.color_use), fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.done), fontWeight = FontWeight.Bold) }
            }
        },
        dismissButton = if (custom) {
            { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
        } else null,
    )
}

/** Horizontal gradient slider: drag or tap anywhere on the bar. [fraction] is 0..1. */
@Composable
private fun GradientBar(fraction: Float, colors: List<Color>, thumbColor: Color, onChange: (Float) -> Unit) {
    val thumb = 26.dp
    val thumbPx = with(LocalDensity.current) { thumb.toPx() }
    var width by remember { mutableStateOf(1) }
    val latest by androidx.compose.runtime.rememberUpdatedState(onChange)
    Box(
        Modifier
            .fillMaxWidth()
            .height(thumb)
            .onSizeChanged { width = it.width.coerceAtLeast(1) }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    fun at(x: Float) = ((x - thumbPx / 2) / (size.width - thumbPx)).coerceIn(0f, 1f)
                    latest(at(down.position.x))
                    drag(down.id) {
                        latest(at(it.position.x))
                        it.consume()
                    }
                }
            },
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(14.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(colors)),
        )
        Box(
            Modifier
                .offset { IntOffset(((width - thumbPx) * fraction.coerceIn(0f, 1f)).toInt(), 0) }
                .size(thumb)
                .clip(CircleShape)
                .background(thumbColor)
                .border(3.dp, Color.White, CircleShape),
        )
    }
}
