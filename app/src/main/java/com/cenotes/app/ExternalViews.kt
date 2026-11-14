package com.cenotes.app

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.glance.appwidget.updateAll
import com.cenotes.app.ui.markdown.markdownToPlainText
import com.cenotes.app.widget.NotesWidget
import com.cenotes.app.widget.TitlesWidget

/**
 * Everything that shows notes outside the app: the two home-screen widgets and the pinned notes
 * in the launcher's long-press menu. Called after any change to the notes.
 */
object ExternalViews {
    private const val MaxPinnedShortcuts = 2

    suspend fun refresh(context: Context) {
        runCatching { NotesWidget().updateAll(context) }
        runCatching { TitlesWidget().updateAll(context) }
        runCatching { updatePinnedShortcuts(context) }
    }

    private suspend fun updatePinnedShortcuts(context: Context) {
        val app = context.applicationContext as CenotesApp
        val pinned = app.database.noteDao().all().filter { it.pinned && !it.archived && !it.locked }.take(MaxPinnedShortcuts)
        val shortcuts = pinned.mapIndexed { i, note ->
            val label = note.title.ifBlank {
                markdownToPlainText(note.content).lineSequence().firstOrNull { it.isNotBlank() }?.trim()
                    ?: context.getString(R.string.untitled)
            }
            ShortcutInfoCompat.Builder(context, "note_${note.id}")
                .setShortLabel(label.take(24))
                .setLongLabel(label.take(48))
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_pinned))
                .setIntent(
                    Intent(context, MainActivity::class.java)
                        .setAction(LaunchAction.ACTION_OPEN_NOTE)
                        .putExtra(LaunchAction.EXTRA_NOTE_ID, note.id),
                )
                .setRank(i)
                .build()
        }
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }
}
