package com.cenotes.app.ui

import com.ce.kit.ui.settings.ChoiceChip
import com.ce.kit.ui.settings.SettingsCard
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ce.kit.log.AppLog
import com.ce.kit.log.CrashLog
import com.cenotes.app.R

/** Settings → Diagnostics (as in Cemail): the activity log can always be shared; the last crash shows only if there was one. */
@Composable
internal fun DiagnosticsPage() {
    val context = LocalContext.current
    var crash by remember { mutableStateOf(CrashLog.read(context)) }
    val copiedLabel = stringResource(R.string.copied)
    fun copy(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(copiedLabel, text))
    }
    SettingsCard {
        Text(stringResource(R.string.settings_log_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.settings_log_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val shareTitle = stringResource(R.string.settings_log_share_title)
            ChoiceChip(stringResource(R.string.settings_log_share), false) { AppLog.share(context, shareTitle) }
            ChoiceChip(stringResource(R.string.copy), false) { copy(AppLog.text(context)) }
            ChoiceChip(stringResource(R.string.clear), false) {
                AppLog.clear(context)
                crash = null
            }
        }
    }
    crash?.let { text ->
        SettingsCard {
            Text(stringResource(R.string.settings_crash_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.settings_crash_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 6, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip(stringResource(R.string.copy), false) { copy(text) }
                ChoiceChip(stringResource(R.string.clear), false) {
                    CrashLog.clear(context)
                    crash = null
                }
            }
        }
    }
}
