package com.cenotes.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.cenotes.app.R
import com.cenotes.app.ui.glass.GlassKind
import com.cenotes.app.ui.glass.LocalListOnRight
import com.cenotes.app.ui.glass.bounceSpring
import com.cenotes.app.ui.glass.glass
import com.cenotes.app.ui.glass.glassSpring
import com.cenotes.app.ui.glass.onBackdrop
import com.cenotes.app.ui.glass.softBounceSpring
import com.cenotes.app.ui.theme.NotesIcon
import kotlinx.coroutines.launch

// Tablets and open foldables, the same way as in Cemail (ui/mail/MailApp.kt): the list and the open note side by side,
// single screens centred, and Settings in a column that can be moved to either side.

/** From this window width (dp) the list and the open note are shown side by side: tablets, open foldables. */
const val TWO_PANE_MIN_WIDTH = 840

/** Widest a single screen grows: on a tablet or an open foldable it stays centred instead of stretching. */
private val MAX_CONTENT_WIDTH = 720.dp

/** [content] centred, no wider than [MAX_CONTENT_WIDTH] (on a phone it simply fills the screen). */
@Composable
fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.fillMaxHeight().widthIn(max = MAX_CONTENT_WIDTH).fillMaxWidth()) { content() }
    }
}

/**
 * The list on the start side (or the end side, as chosen in Settings) and the open note beside it (wide windows). The
 * list takes about a third of the width (between 360 and 480 dp); with no note open, the other side says where to tap.
 * [openKey] tells one opened note from another (a new key = another note, which comes in with its own animation).
 */
@Composable
fun TwoPane(list: @Composable () -> Unit, openKey: String?, note: @Composable (String) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val listWidth = (maxWidth * 0.36f).coerceIn(360.dp, 480.dp)
        val listOnRight = LocalListOnRight.current
        Row(Modifier.fillMaxSize()) {
            if (!listOnRight) Box(Modifier.width(listWidth).fillMaxHeight()) { list() }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                AnimatedContent(
                    targetState = openKey,
                    transitionSpec = {
                        (fadeIn(glassSpring()) + scaleIn(softBounceSpring(), initialScale = 0.96f)) togetherWith fadeOut(glassSpring())
                    },
                    label = "notePane",
                ) { key ->
                    // The card-to-note growing of phones does not apply here: the note has its own side.
                    CompositionLocalProvider(LocalNoteTransition provides null) {
                        if (key != null) {
                            note(key)
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(NotesIcon, null, Modifier.size(48.dp), tint = onBackdrop(MaterialTheme.colorScheme.onSurfaceVariant))
                                    Text(
                                        stringResource(R.string.pane_empty),
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center,
                                        color = onBackdrop(MaterialTheme.colorScheme.onSurfaceVariant),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (listOnRight) Box(Modifier.width(listWidth).fillMaxHeight()) { list() }
        }
    }
}

/**
 * Settings on a wide screen: a column that sits on the left, in the centre or on the right, and stays where it was
 * left. Dragging in the empty space beside it (never inside the cards) moves it one step that way: from the centre to
 * a side, and from a side back to the centre. It follows the finger and settles with a spring. Faint arrows show
 * where it can go; they are drawn with the design's own surface (glass, bevel, outline…), so they match every look.
 * Narrower windows (phones) just centre it.
 */
@Composable
fun Dockable(dock: Int, onDock: (Int) -> Unit, content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val column = minOf(maxWidth, MAX_CONTENT_WIDTH)
        val free = maxWidth - column
        if (maxWidth < TWO_PANE_MIN_WIDTH.dp || free < 120.dp) {
            Centered(content)
            return@BoxWithConstraints
        }
        val density = LocalDensity.current
        val freePx = with(density) { free.toPx() }
        val columnPx = with(density) { column.toPx() }
        fun restX(d: Int) = freePx * (d + 1) / 2f
        val x = remember { Animatable(restX(dock)) }
        var pulled by remember { mutableFloatStateOf(0f) }
        val scope = rememberCoroutineScope()
        LaunchedEffect(dock, freePx) { x.animateTo(restX(dock), bounceSpring()) }
        // The empty space: the column lies on top of it, so a drag that starts on a card never reaches it.
        Box(
            Modifier.fillMaxSize().pointerInput(dock, freePx) {
                val step = 56.dp.toPx()
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val next = when {
                            pulled > step -> (dock + 1).coerceAtMost(1)
                            pulled < -step -> (dock - 1).coerceAtLeast(-1)
                            else -> dock
                        }
                        pulled = 0f
                        if (next != dock) onDock(next) else scope.launch { x.animateTo(restX(dock), bounceSpring()) }
                    },
                    onDragCancel = {
                        pulled = 0f
                        scope.launch { x.animateTo(restX(dock), bounceSpring()) }
                    },
                ) { change, amount ->
                    change.consume()
                    pulled += amount
                    // The column follows the finger a little more slowly than it moves: it feels held by a spring.
                    scope.launch { x.snapTo((restX(dock) + pulled * 0.6f).coerceIn(0f, freePx)) }
                }
            },
        )
        val gap = with(density) { 14.dp.toPx() }
        val arrowWidth = with(density) { ARROW_WIDTH.toPx() }
        // While pulling, the arrow on the side being pulled to grows a little.
        val grow = (kotlin.math.abs(pulled) / with(density) { 300.dp.toPx() }).coerceAtMost(0.35f)
        // An arrow on each side the column can still move to, pointing that way.
        if (dock > -1) MoveArrow(x.value - gap - arrowWidth, pointsRight = false, scale = 1f + if (pulled < 0f) grow else 0f)
        if (dock < 1) MoveArrow(x.value + columnPx + gap, pointsRight = true, scale = 1f + if (pulled > 0f) grow else 0f)
        Box(
            Modifier
                .absoluteOffset { IntOffset(x.value.roundToIntSafe(), 0) }
                .width(column)
                .fillMaxHeight(),
        ) { content() }
    }
}

/** Size of the arrows beside Settings: an equilateral triangle (width = height × √3 / 2). */
private val ARROW_HEIGHT = 30.dp
private val ARROW_WIDTH = 26.dp

/**
 * The arrow beside the Settings column that tells it can move that way: an equilateral triangle without its back side
 * (a chevron), made of the design's own floating surface, so it is frosted glass, a bevel, an outline… as the look is.
 */
@Composable
private fun BoxScope.MoveArrow(left: Float, pointsRight: Boolean, scale: Float) {
    Box(
        Modifier
            .align(AbsoluteAlignment.CenterLeft)
            .absoluteOffset { IntOffset(left.roundToIntSafe(), 0) }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(width = ARROW_WIDTH, height = ARROW_HEIGHT)
            .glass(ChevronShape(pointsRight), GlassKind.Chrome),
    )
}

/** A chevron (› or ‹): the two sides of an equilateral triangle that meet at its tip, about a third as thick as wide. */
private class ChevronShape(private val pointsRight: Boolean) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val t = w * 0.36f // thickness of each arm, measured across
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(t, 0f)
            lineTo(w, h / 2f)
            lineTo(t, h)
            lineTo(0f, h)
            lineTo(w - t, h / 2f)
            close()
            if (!pointsRight) {
                transform(Matrix().apply { scale(-1f, 1f); translate(-w, 0f) })
            }
        }
        return Outline.Generic(path)
    }

    override fun equals(other: Any?) = other is ChevronShape && other.pointsRight == pointsRight
    override fun hashCode() = pointsRight.hashCode()
}

private fun Float.roundToIntSafe(): Int = if (isNaN()) 0 else kotlin.math.round(this).toInt()
