package com.cenotes.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cenotes.app.R
import com.cenotes.app.ui.glass.pressScale
import com.cenotes.app.ui.theme.LocalGlass
import com.cenotes.app.ui.theme.LocalSkin

/**
 * The background colour of a note (like Google Keep's), drawn as a soft wash of the colour over the card, under its
 * content, so it works on the glass and on every other design, in light and dark. [color] 0 = none.
 * Put it right after the card's `glass(...)`, which has already clipped to the card's shape.
 */
@Composable
fun Modifier.noteTint(color: Int): Modifier {
    if (color == 0) return this
    val wash = Color(color).copy(alpha = if (LocalGlass.current.dark) 0.30f else 0.24f)
    return drawWithContent {
        drawRect(wash)
        drawContent()
    }
}

/** Picks a note's background colour: "none" and the ready-made colours of the labels. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteColorDialog(current: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(LocalSkin.current.radius(28.dp)),
        containerColor = LocalGlass.current.menu,
        title = { Text(stringResource(R.string.note_color), fontWeight = FontWeight.Bold) },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ColorChoice(0, current == 0, stringResource(R.string.note_color_none)) { onPick(0) }
                PresetColors.forEach { c -> ColorChoice(c, current == c, null) { onPick(c) } }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done), fontWeight = FontWeight.Bold) } },
    )
}

@Composable
private fun ColorChoice(color: Int, selected: Boolean, label: String?, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    // "None" is the card's own colour with a thin ring; the others are the colour as it looks on a card.
    val fill = if (color == 0) c.surface else Color(color).copy(alpha = 0.55f)
    Box(
        Modifier
            .pressScale(0.9f)
            .size(44.dp)
            .clip(CircleShape)
            .background(fill)
            .border(if (selected) 3.dp else 1.dp, if (selected) c.onSurface.copy(alpha = 0.6f) else c.outline.copy(alpha = 0.4f), CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(Icons.Filled.Check, label, Modifier.size(20.dp), tint = c.onSurface)
    }
}
