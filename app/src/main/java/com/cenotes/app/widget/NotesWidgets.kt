package com.cenotes.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.cenotes.app.data.Note
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.cenotes.app.CenotesApp
import com.cenotes.app.LaunchAction
import com.cenotes.app.MainActivity
import com.cenotes.app.R
import com.cenotes.app.ui.markdown.markdownToPlainText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** "Cenotes": notes with a few lines of their text. */
class NotesWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initial = loadWidgetData(context)
        provideContent { WidgetContent(context, liveWidgetData(context, initial), preview = true) }
    }
}

/** "Cenotes · Títulos": just the titles, compact. */
class TitlesWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val initial = loadWidgetData(context)
        provideContent { WidgetContent(context, liveWidgetData(context, initial), preview = false) }
    }
}

/**
 * Glance keeps a widget's session alive for a while and only recomposes it on update, so the
 * data must be observed (not loaded once) for new notes to show up right away.
 */
@Composable
private fun liveWidgetData(context: Context, initial: WidgetData): WidgetData {
    val app = context.applicationContext as CenotesApp
    val notesFlow = remember {
        app.database.noteDao().observeAll()
            .map { list -> list.filterNot { it.archived || it.locked }.take(30).map(::toWidgetNote) }
            .flowOn(Dispatchers.Default)
    }
    val notes by notesFlow.collectAsState(initial.notes)
    val settings by app.settings.state.collectAsState()
    return WidgetData(notes, settings.accentPalette.light, settings.accentPalette.dark)
}

class NotesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NotesWidget()
}

class TitlesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TitlesWidget()
}

private data class WidgetNote(val id: Long, val title: String, val body: String, val pinned: Boolean)

private data class WidgetData(val notes: List<WidgetNote>, val accentLight: Color, val accentDark: Color)

private val ActionKey = ActionParameters.Key<String>(LaunchAction.EXTRA_ACTION)
private val NoteIdKey = ActionParameters.Key<Long>(LaunchAction.EXTRA_NOTE_ID)

private suspend fun loadWidgetData(context: Context): WidgetData = withContext(Dispatchers.IO) {
    val app = context.applicationContext as CenotesApp
    // Archived notes stay out of the home screen, like out of the main list.
    val notes = app.database.noteDao().all().filterNot { it.archived || it.locked }.take(30).map(::toWidgetNote)
    val accent = app.settings.state.value.accentPalette
    WidgetData(notes, accent.light, accent.dark)
}

private fun toWidgetNote(n: Note): WidgetNote {
    val body = markdownToPlainText(n.content).trim()
    return if (n.title.isBlank()) {
        // Untitled: the first line works as the title, the rest as the preview.
        val lines = body.lines()
        WidgetNote(n.id, lines.firstOrNull().orEmpty().trim(), lines.drop(1).joinToString("\n").trim().take(400), n.pinned)
    } else {
        WidgetNote(n.id, n.title.trim(), body.take(400), n.pinned)
    }
}

// Frosted look for the home screen: translucent surfaces, light/dark aware.
private val WidgetBackground = ColorProvider(day = Color(0xE6F4F0FA), night = Color(0xE61A1E31))
private val CardBackground = ColorProvider(day = Color(0xB3FFFFFF), night = Color(0x1FFFFFFF))
private val TextColor = ColorProvider(day = Color(0xFF1B1B21), night = Color(0xFFF2F1F7))
private val MutedColor = ColorProvider(day = Color(0xFF5D5B66), night = Color(0xFFB5B3C2))

@Composable
private fun WidgetContent(context: Context, data: WidgetData, preview: Boolean) {
    val accent = ColorProvider(day = data.accentLight, night = data.accentDark)
    Column(
        GlanceModifier
            .fillMaxSize()
            .cornerRadius(26.dp)
            .background(WidgetBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            GlanceModifier.fillMaxWidth().padding(start = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                context.getString(R.string.app_name),
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextColor),
                modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>()),
            )
            Box(
                GlanceModifier
                    .size(36.dp)
                    .cornerRadius(18.dp)
                    .background(accent)
                    .clickable(
                        actionStartActivity<MainActivity>(actionParametersOf(ActionKey to LaunchAction.ACTION_NEW_NOTE)),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    ImageProvider(R.drawable.ic_widget_add),
                    contentDescription = context.getString(R.string.new_note),
                    modifier = GlanceModifier.size(20.dp),
                )
            }
        }

        if (data.notes.isEmpty()) {
            Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    context.getString(R.string.widget_empty),
                    style = TextStyle(color = MutedColor, fontSize = 13.sp, textAlign = TextAlign.Center),
                )
            }
            return@Column
        }

        LazyColumn(GlanceModifier.fillMaxSize()) {
            items(data.notes, itemId = { it.id }) { note ->
                val open = actionStartActivity<MainActivity>(
                    actionParametersOf(ActionKey to LaunchAction.ACTION_OPEN_NOTE, NoteIdKey to note.id),
                )
                Column(GlanceModifier.fillMaxWidth().padding(bottom = if (preview) 8.dp else 6.dp)) {
                    if (preview) {
                        PreviewItem(context, note, accent, open)
                    } else {
                        TitleItem(context, note, accent, open)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewItem(
    context: Context,
    note: WidgetNote,
    accent: ColorProvider,
    open: androidx.glance.action.Action,
) {
    Column(
        GlanceModifier
            .fillMaxWidth()
            .cornerRadius(18.dp)
            .background(CardBackground)
            .padding(12.dp)
            .clickable(open),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (note.pinned) {
                Text("★", style = TextStyle(color = accent, fontSize = 13.sp))
                Spacer(GlanceModifier.width(4.dp))
            }
            Text(
                note.title.ifBlank { context.getString(R.string.untitled) },
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (note.title.isBlank()) MutedColor else TextColor,
                ),
                maxLines = 1,
            )
        }
        if (note.body.isNotEmpty()) {
            Spacer(GlanceModifier.height(4.dp))
            Text(note.body, style = TextStyle(fontSize = 13.sp, color = TextColor), maxLines = 4)
        }
    }
}

@Composable
private fun TitleItem(
    context: Context,
    note: WidgetNote,
    accent: ColorProvider,
    open: androidx.glance.action.Action,
) {
    // Untitled notes show their first line instead, so every row says something.
    val label = note.title.ifBlank { note.body.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty() }
        .ifBlank { context.getString(R.string.untitled) }
    Row(
        GlanceModifier
            .fillMaxWidth()
            .cornerRadius(14.dp)
            .background(CardBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clickable(open),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (note.pinned) "★" else "•", style = TextStyle(color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold))
        Spacer(GlanceModifier.width(8.dp))
        Text(
            label,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextColor),
            maxLines = 1,
        )
    }
}
