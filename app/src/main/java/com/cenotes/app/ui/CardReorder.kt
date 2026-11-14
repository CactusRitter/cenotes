package com.cenotes.app.ui

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemInfo
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize

/**
 * Sorting notes by hand (Settings → Notes → By hand), like Google Keep: hold a card and drag it. While it moves, the
 * other cards make room for it at once; when it is let go, the new order is saved. Pinned notes and the others are
 * ordered separately (a card only swaps places with cards of its own group).
 */
@Stable
class CardReorder(private val grid: LazyStaggeredGridState) {
    /** The note being dragged, or null. */
    var dragId by mutableStateOf<Long?>(null)
        private set

    /** The order on screen while dragging (and until the saved order comes back from the database); null = as stored. */
    var order by mutableStateOf<List<Long>?>(null)
        private set

    private var original: List<Long> = emptyList()
    private var start = Offset.Zero // where the card was in the list (its top-left) when it was picked up
    private var drag by mutableStateOf(Offset.Zero) // how far the finger has moved since
    private var lastSwap: Long? = null // the card just swapped with, ignored until the dragged card leaves it
    private var moved = false

    private fun info(id: Long): LazyStaggeredGridItemInfo? = grid.layoutInfo.visibleItemsInfo.firstOrNull { it.key == id }

    fun start(id: Long, shown: List<Long>) {
        val info = info(id) ?: return
        original = shown
        order = shown
        dragId = id
        start = info.offset.toOffset()
        drag = Offset.Zero
        lastSwap = null
        moved = false
    }

    fun dragBy(amount: Offset, sameGroup: (Long, Long) -> Boolean) {
        drag += amount
        if (drag.getDistance() > 24f) moved = true
        retarget(sameGroup)
    }

    /** Where the middle of the dragged card is now, in the list's coordinates. */
    private fun center(id: Long): Offset? = info(id)?.let { start + drag + Offset(it.size.width / 2f, it.size.height / 2f) }

    /** Moves the dragged card in front of the card under its middle (after the list scrolled, too). */
    fun retarget(sameGroup: (Long, Long) -> Boolean) {
        val id = dragId ?: return
        val center = center(id) ?: return
        val under = grid.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            val key = item.key
            key is Long && key != id && Rect(item.offset.toOffset(), item.size.toSize()).contains(center)
        }
        val target = under?.key as? Long
        if (target != lastSwap) lastSwap = null
        if (target == null || target == lastSwap || !sameGroup(id, target)) return
        val current = order ?: return
        val from = current.indexOf(id)
        val to = current.indexOf(target)
        if (from < 0 || to < 0) return
        order = current.toMutableList().apply {
            removeAt(from)
            add(to, id)
        }
        lastSwap = target
    }

    /** How far the dragged card is drawn from its place in the list, so it stays under the finger. */
    fun translation(id: Long): Offset {
        if (id != dragId) return Offset.Zero
        val info = info(id) ?: return drag
        return start + drag - info.offset.toOffset()
    }

    /**
     * Scroll speed while the dragged card is near the top or bottom edge of the list (negative = up), so a note can
     * be taken further than the screen shows.
     */
    fun edgeSpeed(edge: Float): Float {
        val id = dragId ?: return 0f
        val y = center(id)?.y ?: return 0f
        val top = grid.layoutInfo.viewportStartOffset.toFloat()
        val bottom = grid.layoutInfo.viewportEndOffset.toFloat()
        return when {
            y < top + edge -> -(top + edge - y) / edge * 24f
            y > bottom - edge -> (y - (bottom - edge)) / edge * 24f
            else -> 0f
        }
    }

    /** Lets go of the card; returns the new order to save, or null when nothing changed (it was only held). */
    fun end(): List<Long>? {
        val result = order
        dragId = null
        drag = Offset.Zero
        if (!moved || result == null || result == original) {
            order = null
            return null
        }
        return result
    }

    fun cancel() {
        dragId = null
        drag = Offset.Zero
        order = null
    }

    /** The saved order has arrived from the database: the list shows it again. */
    fun settled() {
        if (dragId == null) order = null
    }

    /** Whether the card was really moved (not only held), once let go. */
    val wasMoved: Boolean get() = moved
}
