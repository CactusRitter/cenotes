package com.cenotes.app.ui

import android.text.format.DateUtils
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cenotes.app.R
import com.cenotes.app.data.Reminder
import com.cenotes.app.data.Repeat
import com.cenotes.app.ui.glass.GlassKind
import com.cenotes.app.ui.glass.glass
import com.cenotes.app.ui.glass.popIn
import com.cenotes.app.ui.theme.LocalSkin
import java.util.Calendar

/** The quick times of a reminder: the same ones as Cemail's "Snooze" and "Send later". */
private enum class ReminderTime(@StringRes val label: Int) {
    LATER(R.string.reminder_later), TOMORROW(R.string.reminder_tomorrow),
    WEEKEND(R.string.reminder_weekend), NEXT_WEEK(R.string.reminder_next_week);

    fun at(now: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = now }
        fun morning() { c.set(Calendar.HOUR_OF_DAY, 8); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0) }
        when (this) {
            LATER -> c.add(Calendar.HOUR_OF_DAY, 3)
            TOMORROW -> { c.add(Calendar.DAY_OF_YEAR, 1); morning() }
            WEEKEND -> { do { c.add(Calendar.DAY_OF_YEAR, 1) } while (c.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY); morning() }
            NEXT_WEEK -> { do { c.add(Calendar.DAY_OF_YEAR, 1) } while (c.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY); morning() }
        }
        return c.timeInMillis
    }
}

/** Repeat choices with their names, in the order they are offered. */
private val RepeatLabels = listOf(
    Repeat.NONE to R.string.repeat_none, Repeat.DAILY to R.string.repeat_daily,
    Repeat.WEEKLY to R.string.repeat_weekly, Repeat.MONTHLY to R.string.repeat_monthly,
)

/**
 * "Remind me": how often first (once, every day, week or month), then when — the quick times or a date and time of
 * one's own — which sets it at once. With a reminder already set it can also be removed. Same look as Cemail's
 * "Send later" (glass menu card with rows that pop in).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderDialog(
    current: Reminder,
    places: List<com.cenotes.app.data.Place>,
    onSavePlace: suspend (String, Double, Double) -> Long,
    onDeletePlace: (Long) -> Unit,
    onSet: (Reminder) -> Unit,
    onDismiss: () -> Unit,
) {
    var repeat by remember { mutableStateOf(current.repeat) }
    var pickingOwn by remember { mutableStateOf(false) }
    var pickingPlace by remember { mutableStateOf(false) }
    if (pickingPlace) {
        PlaceDialog(
            placeId = current.placeId,
            leave = current.placeLeave,
            places = places,
            onSave = onSavePlace,
            onDelete = onDeletePlace,
            onSet = { id, leave -> onSet(current.copy(placeId = id, placeLeave = leave)) },
            onDismiss = { pickingPlace = false },
        )
        return
    }
    if (pickingOwn) {
        DateTimeDialog(
            initial = current.at,
            // A time that has already gone by would remind at once: it becomes a minute from now instead.
            onPick = { at -> onSet(current.copy(at = maxOf(at, System.currentTimeMillis() + 60_000), repeat = repeat)) },
            onDismiss = { pickingOwn = false },
        )
        return
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .popIn(initialScale = 0.75f)
                .fillMaxWidth()
                .glass(RoundedCornerShape(28.dp), GlassKind.Menu, elevation = 12.dp)
                .padding(vertical = 16.dp),
        ) {
            Text(stringResource(R.string.reminder_title), Modifier.padding(horizontal = 24.dp, vertical = 8.dp), style = MaterialTheme.typography.titleMedium)
            current.at?.let { at ->
                Text(
                    stringResource(R.string.reminder_current, reminderText(at, current.repeat)),
                    Modifier.padding(horizontal = 24.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FlowRow(
                Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RepeatLabels.forEach { (r, label) -> ChoiceChip(stringResource(label), repeat == r) { repeat = r } }
            }
            ReminderTime.entries.forEach { option ->
                ReminderRow(stringResource(option.label)) { onSet(current.copy(at = option.at(System.currentTimeMillis()), repeat = repeat)) }
            }
            ReminderRow(stringResource(R.string.reminder_pick)) { pickingOwn = true }
            if (current.at != null) ReminderRow(stringResource(R.string.reminder_remove), danger = true) { onSet(current.copy(at = null, repeat = Repeat.NONE)) }
            // At a place instead of (or as well as) a time: on arriving at the supermarket, on leaving home…
            ReminderRow(stringResource(R.string.place_row), icon = com.cenotes.app.ui.theme.PlaceIcon) { pickingPlace = true }
        }
    }
}

@Composable
private fun ReminderRow(
    label: String,
    danger: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Notifications,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, null, tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyLarge, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    }
}

/** "Tomorrow, 8:00 · Every week" in the phone's language and time format. */
@Composable
fun reminderText(at: Long, repeat: Repeat, compact: Boolean = false): String {
    val context = LocalContext.current
    val weekday = if (compact) 0 else DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY
    val time = DateUtils.formatDateTime(
        context, at,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH or weekday,
    )
    // On a card there is little room: a repeating reminder just gets the "repeat" sign.
    if (compact) return if (repeat == Repeat.NONE) time else "$time  ↻"
    val every = RepeatLabels.firstOrNull { it.first == repeat && repeat != Repeat.NONE }?.let { stringResource(it.second) }
    return if (every == null) time else "$time · $every"
}

/** The small chip of a note's reminder (on its card and in the editor); crossed out once the time has passed. */
@Composable
fun ReminderChip(at: Long, repeat: Repeat, modifier: Modifier = Modifier, iconSize: Dp = 14.dp, compact: Boolean = false) {
    val c = MaterialTheme.colorScheme
    val past = at < System.currentTimeMillis()
    Row(
        modifier
            .clip(LocalSkin.current.shape(CircleShape))
            .background(c.primaryContainer)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Notifications, null, Modifier.size(iconSize), tint = c.primary)
        androidx.compose.foundation.layout.Spacer(Modifier.width(4.dp))
        Text(
            reminderText(at, repeat, compact),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = c.primary,
            textDecoration = if (past) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}
