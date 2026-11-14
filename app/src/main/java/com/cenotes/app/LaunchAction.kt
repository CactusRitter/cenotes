package com.cenotes.app

import android.content.Intent
import android.net.Uri
import android.os.Build

/** What the app was opened for: from the launcher shortcuts, a widget, the Quick Settings tile, a reminder or "Share". */
sealed interface LaunchAction {
    /** A new note; [template] is its initial Markdown (e.g. an empty checkbox for a list) and [title] its title. */
    data class NewNote(val template: String = "", val title: String = "") : LaunchAction
    data class OpenNote(val id: Long) : LaunchAction
    data object Search : LaunchAction

    /** Something shared with Cenotes from another app: a subject, a text or link, and pictures. */
    data class Shared(val title: String, val text: String, val images: List<Uri>) : LaunchAction

    companion object {
        const val ACTION_NEW_NOTE = "com.cenotes.app.NEW_NOTE"
        const val ACTION_NEW_LIST = "com.cenotes.app.NEW_LIST"
        const val ACTION_SEARCH = "com.cenotes.app.SEARCH"
        const val ACTION_OPEN_NOTE = "com.cenotes.app.OPEN_NOTE"

        /** Widgets pass the action as an extra (Glance action parameters). */
        const val EXTRA_ACTION = "cenotes_action"
        const val EXTRA_NOTE_ID = "note_id"

        fun from(intent: Intent?): LaunchAction? {
            if (intent == null) return null
            return when (intent.getStringExtra(EXTRA_ACTION) ?: intent.action) {
                ACTION_NEW_NOTE -> NewNote()
                ACTION_NEW_LIST -> NewNote("- [ ] ")
                ACTION_SEARCH -> Search
                ACTION_OPEN_NOTE -> intent.getLongExtra(EXTRA_NOTE_ID, -1L).takeIf { it > 0 }?.let(::OpenNote)
                Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE -> shared(intent)
                else -> null
            }
        }

        private fun shared(intent: Intent): Shared? {
            val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty().trim()
            val title = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty().trim()
            val images = if (intent.type?.startsWith("image/") == true) {
                if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION") intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                    }.orEmpty()
                } else {
                    listOfNotNull(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                        } else {
                            @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
                        },
                    )
                }
            } else {
                emptyList()
            }
            if (text.isEmpty() && title.isEmpty() && images.isEmpty()) return null
            // A shared link often comes as "Page title https://…" with the title also as subject: no need to repeat it.
            return Shared(title.takeIf { it != text }.orEmpty(), text, images.take(10))
        }
    }
}
