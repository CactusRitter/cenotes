package com.cenotes.app.ui

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cenotes.app.R
import com.cenotes.app.data.MaxVersions
import com.cenotes.app.data.NoteVersion
import com.cenotes.app.ui.glass.GlassKind
import com.cenotes.app.ui.glass.glass
import com.cenotes.app.ui.glass.popIn
import com.cenotes.app.ui.markdown.MarkdownPreview
import com.cenotes.app.ui.markdown.markdownToPlainText
import com.cenotes.app.ui.theme.HistoryIcon
import com.cenotes.app.ui.theme.LocalSkin

/**
 * ⋮ → History: the earlier versions of a note (the latest first), each with when it was written and how it began.
 * Tapping one shows it; "Restore" puts it back in the editor (what was there goes to the history, so nothing is lost).
 */
@Composable
fun HistoryDialog(versions: List<NoteVersion>?, onRestore: (NoteVersion) -> Unit, onDismiss: () -> Unit) {
    var open by remember { mutableStateOf<NoteVersion?>(null) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .popIn(initialScale = 0.75f)
                .fillMaxWidth()
                .glass(RoundedCornerShape(LocalSkin.current.radius(28.dp)), GlassKind.Menu, elevation = 12.dp)
                .padding(vertical = 16.dp),
        ) {
            Text(stringResource(R.string.history), Modifier.padding(horizontal = 24.dp, vertical = 8.dp), style = MaterialTheme.typography.titleMedium)
            val shown = open
            when {
                versions == null -> Unit // still loading
                versions.isEmpty() -> Text(
                    stringResource(R.string.history_empty, MaxVersions),
                    Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                shown != null -> {
                    // One version, as its note card would show it.
                    Text(
                        versionTime(shown.savedAt),
                        Modifier.padding(horizontal = 24.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Column(
                        Modifier
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (shown.title.isNotBlank()) Text(shown.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        MarkdownPreview(shown.content, maxBlocks = 60)
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { open = null }) { Text(stringResource(R.string.back)) }
                        TextButton(onClick = { onRestore(shown) }) { Text(stringResource(R.string.history_restore)) }
                    }
                }
                else -> {
                    Text(
                        stringResource(R.string.history_body, MaxVersions),
                        Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                        versions.forEach { v ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(role = Role.Button) { open = v }
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(HistoryIcon, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(versionTime(v.savedAt), style = MaterialTheme.typography.bodyLarge)
                                    val start = markdownToPlainText(v.content).trim().lineSequence().firstOrNull().orEmpty().ifBlank { v.title }
                                    Text(
                                        start.ifBlank { stringResource(R.string.untitled) },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** "Today, 14:32" style: the date and time a version was written, in the phone's language and format. */
@Composable
private fun versionTime(at: Long): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    return DateUtils.formatDateTime(
        context, at,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY,
    )
}
