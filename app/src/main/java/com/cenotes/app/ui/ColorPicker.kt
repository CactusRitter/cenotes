package com.cenotes.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.cenotes.app.R
import com.cenotes.app.data.Label
import com.cenotes.app.data.MaxCustomColors
import com.cenotes.app.ui.theme.LocalGlass

/** Ready-made label colours (ARGB). */
val PresetColors = listOf(
    0xFFE5484D, 0xFFF5873B, 0xFFF2B632, 0xFF3FAE5A, 0xFF2BB5A0,
    0xFF29A8D8, 0xFF4C7DF0, 0xFF8B5CF6, 0xFFE8559E, 0xFF8A94A6,
).map { it.toInt() }

/** One round colour swatch; a check mark shows when it is the current choice. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Swatch(color: Int, selected: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(color))
            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(Icons.Filled.Check, null, tint = Color.White)
    }
}

/**
 * The user's own colours (up to [MaxCustomColors]): only the ones that exist are shown, followed
 * by a "+" button while there is room. With [onPick] a tap uses the colour and a long press edits it;
 * without it (settings) a tap edits it. Editing a colour also recolours the labels that use it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomColorsRow(
    colors: List<Int>,
    selected: Int,
    onPick: ((Int) -> Unit)?,
    onAdd: (Int) -> Unit,
    onEdit: (old: Int, new: Int) -> Unit,
    onDelete: (Int) -> Unit,
) {
    var editing by remember { mutableStateOf<Int?>(null) }
    var adding by remember { mutableStateOf(false) }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 5,
    ) {
        colors.forEach { c ->
            if (onPick != null) Swatch(c, selected == c, onClick = { onPick(c) }, onLongClick = { editing = c })
            else Swatch(c, false, onClick = { editing = c })
        }
        if (colors.size < MaxCustomColors) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f), CircleShape)
                    .clickable { adding = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, stringResource(R.string.color_add), tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
    if (adding) {
        ColorEditorDialog(
            initial = null,
            onSave = { onAdd(it) },
            onDelete = null,
            onDismiss = { adding = false },
        )
    }
    editing?.let { old ->
        ColorEditorDialog(
            initial = old,
            onSave = { onEdit(old, it) },
            onDelete = { onDelete(old) },
            onDismiss = { editing = null },
        )
    }
}

/** Build any colour with hue / saturation / brightness bars. [initial] null = a new colour. */
@Composable
private fun ColorEditorDialog(initial: Int?, onSave: (Int) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    val start = remember(initial) {
        FloatArray(3).also {
            if (initial != null) android.graphics.Color.colorToHSV(initial, it) else {
                it[0] = 210f; it[1] = 0.7f; it[2] = 0.9f
            }
        }
    }
    var hue by remember { mutableStateOf(start[0]) }
    var sat by remember { mutableStateOf(start[1]) }
    var bri by remember { mutableStateOf(start[2]) }
    val picked = Color.hsv(hue, sat, bri)
    val hues = List(7) { Color.hsv(it * 60f, 1f, 1f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = LocalGlass.current.menu,
        title = {
            Text(
                stringResource(if (initial == null) R.string.color_new else R.string.color_edit),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp)).background(picked))
                GradientBar(hue / 360f, hues, picked) { hue = it * 360f }
                GradientBar(sat, listOf(Color.hsv(hue, 0f, bri), Color.hsv(hue, 1f, bri)), picked) { sat = it }
                GradientBar(bri, listOf(Color.hsv(hue, sat, 0f), Color.hsv(hue, sat, 1f)), picked) { bri = it }
                if (onDelete != null) {
                    TextButton(onClick = { onDelete(); onDismiss() }) {
                        Text(stringResource(R.string.color_remove), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(picked.toArgb()); onDismiss() }) {
                Text(stringResource(R.string.color_save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Colour of one label: ten ready-made colours plus the user's own ones. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabelColorDialog(
    label: Label,
    customColors: List<Int>,
    onPick: (Int) -> Unit,
    onAddColor: (Int) -> Unit,
    onEditColor: (Int, Int) -> Unit,
    onDeleteColor: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
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
                    PresetColors.forEach { c -> Swatch(c, label.color == c, onClick = { onPick(c); onDismiss() }) }
                }
                Column {
                    Text(stringResource(R.string.colors_mine), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.colors_mine_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                CustomColorsRow(
                    colors = customColors,
                    selected = label.color,
                    onPick = { onPick(it); onDismiss() },
                    onAdd = { onAddColor(it); onPick(it); onDismiss() },
                    onEdit = onEditColor,
                    onDelete = onDeleteColor,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done), fontWeight = FontWeight.Bold) } },
    )
}

/** Horizontal gradient slider: drag or tap anywhere on the bar. [fraction] is 0..1. */
@Composable
private fun GradientBar(fraction: Float, colors: List<Color>, thumbColor: Color, onChange: (Float) -> Unit) {
    val thumb = 26.dp
    val thumbPx = with(LocalDensity.current) { thumb.toPx() }
    var width by remember { mutableStateOf(1) }
    val latest by rememberUpdatedState(onChange)
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
