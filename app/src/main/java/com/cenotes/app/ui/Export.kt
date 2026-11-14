package com.cenotes.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.cenotes.app.R
import com.cenotes.app.data.ImageStore
import com.cenotes.app.ui.glass.GlassKind
import com.cenotes.app.ui.glass.glass
import com.cenotes.app.ui.glass.popIn
import com.cenotes.app.ui.markdown.ImageRegex
import com.cenotes.app.ui.markdown.imageNames
import com.cenotes.app.ui.markdown.markdownToShareText
import com.cenotes.app.ui.theme.OpenInNewIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** The formats a note can be exported to (Phase 3 of the roadmap). */
enum class ExportFormat(@StringRes val label: Int, @StringRes val body: Int) {
    PDF(R.string.export_pdf, R.string.export_pdf_body),
    MARKDOWN(R.string.export_markdown, R.string.export_markdown_body),
    TEXT(R.string.export_text, R.string.export_text_body),
}

/** "Export": the three formats as rows of a glass menu, like the reminder choices. */
@Composable
fun ExportDialog(onPick: (ExportFormat) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .popIn(initialScale = 0.75f)
                .fillMaxWidth()
                .glass(RoundedCornerShape(28.dp), GlassKind.Menu, elevation = 12.dp)
                .padding(vertical = 16.dp),
        ) {
            Text(stringResource(R.string.export), Modifier.padding(horizontal = 24.dp, vertical = 8.dp), style = MaterialTheme.typography.titleMedium)
            ExportFormat.entries.forEach { format ->
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Button) { onPick(format) }.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(OpenInNewIcon, null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(stringResource(format.label), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(format.body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/**
 * Writes the note as a file of [format] in the app's cache and returns the Android share sheet for it (to save it in
 * Drive or Files, or send it). Markdown keeps the note's own format and takes its pictures along as files; PDF lays
 * the text and pictures out on A4 pages; text is plain, with ☐/☑ for the checkboxes.
 */
suspend fun exportNote(context: Context, title: String, markdown: String, images: ImageStore, format: ExportFormat): Intent =
    withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "exports").apply {
            deleteRecursively() // only the latest export is kept
            mkdirs()
        }
        val base = title.trim().ifEmpty { context.getString(R.string.untitled) }
            .replace(Regex("""[\\/:*?"<>|\n\r]"""), " ").trim().take(60).ifEmpty { "Cenotes" }
        fun uri(f: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.files", f)

        val send = when (format) {
            ExportFormat.TEXT -> {
                val f = File(dir, "$base.txt")
                f.writeText(listOf(title.trim(), markdownToShareText(markdown).trim()).filter { it.isNotEmpty() }.joinToString("\n\n"))
                Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_STREAM, uri(f))
            }
            ExportFormat.MARKDOWN -> {
                val f = File(dir, "$base.md")
                f.writeText((if (title.isNotBlank()) "# ${title.trim()}\n\n" else "") + markdown)
                // The pictures go along under their own names, which is how the Markdown refers to them.
                val pictures = imageNames(markdown).map(images::file).filter { it.exists() }
                if (pictures.isEmpty()) {
                    Intent(Intent.ACTION_SEND).setType("text/markdown").putExtra(Intent.EXTRA_STREAM, uri(f))
                } else {
                    Intent(Intent.ACTION_SEND_MULTIPLE).setType("*/*")
                        .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList((listOf(f) + pictures).map(::uri)))
                }
            }
            ExportFormat.PDF -> {
                val f = File(dir, "$base.pdf")
                writePdf(f, title, markdown, images)
                Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, uri(f))
            }
        }
        // The files also go as ClipData, so the share sheet itself may read them (for its preview).
        val streams = androidx.core.content.IntentCompat.getParcelableArrayListExtra(send, Intent.EXTRA_STREAM, Uri::class.java)
            ?: listOfNotNull(androidx.core.content.IntentCompat.getParcelableExtra(send, Intent.EXTRA_STREAM, Uri::class.java))
        if (streams.isNotEmpty()) {
            send.clipData = android.content.ClipData.newRawUri(null, streams.first()).apply {
                streams.drop(1).forEach { addItem(android.content.ClipData.Item(it)) }
            }
        }
        Intent.createChooser(send.putExtra(Intent.EXTRA_SUBJECT, title).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null)
    }

/** A4 at 72 dpi, with 48 pt margins. */
private const val PAGE_W = 595
private const val PAGE_H = 842
private const val MARGIN = 48

private fun writePdf(file: File, title: String, markdown: String, images: ImageStore) {
    val doc = PdfDocument()
    val width = PAGE_W - 2 * MARGIN
    var pageNumber = 0
    var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, ++pageNumber).create())
    var y = MARGIN.toFloat()
    fun newPage() {
        doc.finishPage(page)
        page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, ++pageNumber).create())
        y = MARGIN.toFloat()
    }
    fun room() = PAGE_H - MARGIN - y

    fun text(content: String, size: Float, bold: Boolean) {
        if (content.isBlank()) return
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        val layout = StaticLayout.Builder.obtain(content, 0, content.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0f, 1.25f).build()
        // Line by line, so a long text goes on to the next page instead of being cut.
        for (i in 0 until layout.lineCount) {
            val h = (layout.getLineBottom(i) - layout.getLineTop(i)).toFloat()
            if (h > room()) newPage()
            val c = page.canvas
            c.save()
            c.translate(MARGIN.toFloat(), y - layout.getLineTop(i))
            c.clipRect(0, layout.getLineTop(i), width, layout.getLineBottom(i))
            layout.draw(c)
            c.restore()
            y += h
        }
        y += size * 0.6f
    }

    fun picture(name: String) {
        val f = images.file(name)
        if (!f.exists()) return
        val bitmap = BitmapFactory.decodeFile(f.path) ?: return
        val scale = minOf(width.toFloat() / bitmap.width, (PAGE_H - 2 * MARGIN).toFloat() / bitmap.height, 1.5f)
        val w = bitmap.width * scale
        val h = bitmap.height * scale
        if (h > room()) newPage()
        page.canvas.drawBitmap(bitmap, null, Rect(MARGIN, y.toInt(), (MARGIN + w).toInt(), (y + h).toInt()), Paint(Paint.FILTER_BITMAP_FLAG))
        bitmap.recycle()
        y += h + 12f
    }

    if (title.isNotBlank()) text(title.trim(), 20f, bold = true)
    // Text and pictures in the note's order: text between pictures is laid out as one block.
    val pending = StringBuilder()
    fun flush() {
        text(markdownToShareText(pending.toString()).trim(), 12f, bold = false)
        pending.clear()
    }
    for (line in markdown.lines()) {
        val img = ImageRegex.find(line)
        if (img != null) {
            flush()
            picture(img.groupValues[2])
        } else {
            pending.appendLine(line)
        }
    }
    flush()
    doc.finishPage(page)
    file.outputStream().use(doc::writeTo)
    doc.close()
}
