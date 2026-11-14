package com.cenotes.app.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cenotes.app.ui.theme.LocalSkin

/**
 * Scopes needed to animate a note card into the editor and back (like Google Keep): the card
 * grows to fill the screen when opened and shrinks back into its place in the list when closed.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
class NoteTransition(val shared: SharedTransitionScope, val visibility: AnimatedVisibilityScope)

val LocalNoteTransition = compositionLocalOf<NoteTransition?> { null }

/**
 * Marks a card (list) or a whole screen (editor) as the same element for note [noteId]; the
 * bounds morph from one to the other. Notes that don't exist yet (id 0) just fade.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.noteSharedBounds(noteId: Long, zIndex: Float = 0f): Modifier {
    val t = LocalNoteTransition.current ?: return this
    if (noteId <= 0L) return this
    val self = this
    // The same corners as the note cards in this design (square in Retro 98, small in Paper…), so the card does not
    // turn round while it grows or shrinks.
    val shape = LocalSkin.current.shape(RoundedCornerShape(24.dp))
    return with(t.shared) {
        self.sharedBounds(
            sharedContentState = rememberSharedContentState(key = "note-$noteId"),
            animatedVisibilityScope = t.visibility,
            boundsTransform = { _, _ -> spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow) },
            clipInOverlayDuringTransition = OverlayClip(shape),
            zIndexInOverlay = zIndex,
        )
    }
}
