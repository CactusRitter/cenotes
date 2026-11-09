package com.cenotes.app

import android.content.Intent

/** What the app was opened for: from the launcher shortcuts or from a widget. */
sealed interface LaunchAction {
    /** A new note; [template] is its initial Markdown (e.g. an empty checkbox for a list). */
    data class NewNote(val template: String = "") : LaunchAction
    data class OpenNote(val id: Long) : LaunchAction
    data object Search : LaunchAction

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
                else -> null
            }
        }
    }
}
