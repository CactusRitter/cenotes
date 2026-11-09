package com.example.notas.ui.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/**
 * Live highlighting while editing: the text stays exactly as typed (identity offsets), but headings
 * grow, markers are tinted, and bold/italic/strike/code show their style.
 */
class MarkdownHighlighter(
    private val marker: Color,
    private val accent: Color,
    private val muted: Color,
    private val codeBackground: Color,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val s = text.text
        val b = AnnotatedString.Builder(s)
        val markerStyle = SpanStyle(color = marker)
        val accentMarker = SpanStyle(color = accent, fontWeight = FontWeight.Bold)

        var start = 0
        for (line in s.split('\n')) {
            val end = start + line.length
            TaskRegex.matchEntire(line)?.let { m ->
                val boxEnd = start + m.groups[2]!!.range.last + 2
                b.addStyle(accentMarker, start, boxEnd.coerceAtMost(end))
                if (m.groupValues[2] != " ") {
                    b.addStyle(SpanStyle(color = muted, textDecoration = TextDecoration.LineThrough), boxEnd.coerceAtMost(end), end)
                }
            } ?: HeadingRegex.matchEntire(line)?.let { m ->
                val size = when (m.groupValues[1].length) { 1 -> 24.sp; 2 -> 21.sp; else -> 18.sp }
                b.addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = size), start, end)
                b.addStyle(markerStyle, start, start + m.groupValues[1].length)
            } ?: BulletRegex.matchEntire(line)?.let { m ->
                val r = m.groups[2]!!.range
                b.addStyle(accentMarker, start + r.first, start + r.last + 1)
            } ?: NumberedRegex.matchEntire(line)?.let { m ->
                b.addStyle(accentMarker, start + m.groups[2]!!.range.first, start + m.groups[3]!!.range.last + 1)
            } ?: QuoteRegex.matchEntire(line)?.let {
                b.addStyle(accentMarker, start, start + 1)
                b.addStyle(SpanStyle(fontStyle = FontStyle.Italic, color = muted), start + 1, end)
            }
            start = end + 1
        }

        fun inline(regex: Regex, markerLen: Int, style: SpanStyle) {
            regex.findAll(s).forEach { m ->
                val r = m.range
                b.addStyle(style, r.first, r.last + 1)
                b.addStyle(markerStyle, r.first, r.first + markerLen)
                b.addStyle(markerStyle, r.last + 1 - markerLen, r.last + 1)
            }
        }
        inline(Regex("""(\*\*|__)[^\n]+?\1"""), 2, SpanStyle(fontWeight = FontWeight.Bold))
        inline(Regex("""~~[^\n]+?~~"""), 2, SpanStyle(textDecoration = TextDecoration.LineThrough))
        inline(Regex("""(?<![\w*])\*(?![\s*])[^\n*]+?(?<![\s*])\*(?![\w*])"""), 1, SpanStyle(fontStyle = FontStyle.Italic))
        inline(Regex("""(?<![\w_])_(?![\s_])[^\n_]+?(?<![\s_])_(?![\w_])"""), 1, SpanStyle(fontStyle = FontStyle.Italic))
        inline(Regex("""`[^`\n]+`"""), 1, SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground))

        return TransformedText(b.toAnnotatedString(), OffsetMapping.Identity)
    }

    override fun equals(other: Any?) = other is MarkdownHighlighter && other.marker == marker &&
        other.accent == accent && other.muted == muted && other.codeBackground == codeBackground

    override fun hashCode() = listOf(marker, accent, muted, codeBackground).hashCode()
}

private fun lineStartAt(text: String, index: Int) = text.lastIndexOf('\n', (index - 1).coerceAtLeast(0)).let {
    if (index == 0) 0 else it + 1
}

private fun lineEndAt(text: String, index: Int) = text.indexOf('\n', index).let { if (it == -1) text.length else it }

/**
 * Called on every edit. When Enter was just pressed at the end of a list item, the next line gets
 * the same marker (a new unchecked box for checklists, the next number for numbered lists).
 * Pressing Enter on an empty item ends the list instead.
 */
fun continueList(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    val text = new.text
    val cursor = new.selection.start
    val insertedNewline = new.selection.collapsed && text.length == old.text.length + 1 &&
        cursor > 0 && text[cursor - 1] == '\n' && old.selection.collapsed
    if (!insertedNewline) return new

    val prevStart = lineStartAt(text, cursor - 1)
    val prevLine = text.substring(prevStart, cursor - 1)

    val (prefix, body) = TaskRegex.matchEntire(prevLine)?.let {
        "${it.groupValues[1]}- [ ] " to it.groupValues[3]
    } ?: BulletRegex.matchEntire(prevLine)?.let {
        "${it.groupValues[1]}${it.groupValues[2]} " to it.groupValues[3]
    } ?: NumberedRegex.matchEntire(prevLine)?.let {
        "${it.groupValues[1]}${it.groupValues[2].toInt() + 1}${it.groupValues[3]} " to it.groupValues[4]
    } ?: return new

    if (body.isBlank()) {
        // Empty item: remove its marker and the new line, leaving an empty line.
        val newText = text.removeRange(prevStart, cursor)
        return TextFieldValue(newText, TextRange(prevStart))
    }
    val newText = text.substring(0, cursor) + prefix + text.substring(cursor)
    return TextFieldValue(newText, TextRange(cursor + prefix.length))
}

enum class LineFormat { TASK, BULLET, NUMBERED, QUOTE, HEADING }

/** Toggles a line marker on every line touched by the selection (or cycles heading levels). */
fun toggleLineFormat(value: TextFieldValue, format: LineFormat): TextFieldValue {
    val text = value.text
    val selStart = minOf(value.selection.start, value.selection.end)
    val selEnd = maxOf(value.selection.start, value.selection.end)
    val blockStart = lineStartAt(text, selStart)
    val blockEnd = lineEndAt(text, selEnd)
    val lines = text.substring(blockStart, blockEnd).split('\n')

    val allHave = lines.all { line ->
        when (format) {
            LineFormat.TASK -> TaskRegex.matches(line)
            LineFormat.BULLET -> BulletRegex.matches(line) && !TaskRegex.matches(line)
            LineFormat.NUMBERED -> NumberedRegex.matches(line)
            LineFormat.QUOTE -> QuoteRegex.matches(line)
            LineFormat.HEADING -> false
        }
    }

    fun stripMarkers(line: String): Pair<String, String> {
        TaskRegex.matchEntire(line)?.let { return it.groupValues[1] to it.groupValues[3] }
        BulletRegex.matchEntire(line)?.let { return it.groupValues[1] to it.groupValues[3] }
        NumberedRegex.matchEntire(line)?.let { return it.groupValues[1] to it.groupValues[4] }
        QuoteRegex.matchEntire(line)?.let { return "" to it.groupValues[1] }
        HeadingRegex.matchEntire(line)?.let { return "" to it.groupValues[2] }
        val indent = line.takeWhile { it == ' ' || it == '\t' }
        return indent to line.substring(indent.length)
    }

    var number = 1
    val newLines = lines.map { line ->
        if (format == LineFormat.HEADING) {
            val level = HeadingRegex.matchEntire(line)?.groupValues?.get(1)?.length ?: 0
            val body = stripMarkers(line).second
            return@map when (level) {
                0 -> "# $body"
                1 -> "## $body"
                2 -> "### $body"
                else -> body
            }
        }
        val (indent, body) = stripMarkers(line)
        if (allHave) {
            indent + body
        } else when (format) {
            LineFormat.TASK -> "$indent- [ ] $body"
            LineFormat.BULLET -> "$indent- $body"
            LineFormat.NUMBERED -> "$indent${number++}. $body"
            LineFormat.QUOTE -> "> $body"
            LineFormat.HEADING -> body
        }
    }
    val replaced = newLines.joinToString("\n")
    val newText = text.substring(0, blockStart) + replaced + text.substring(blockEnd)
    // Cursor goes to the end of the edited lines, ready to keep typing.
    return TextFieldValue(newText, TextRange(blockStart + replaced.length))
}

/** Wraps the selection in [marker] (e.g. `**`), or removes it if it's already wrapped. */
fun toggleWrap(value: TextFieldValue, marker: String): TextFieldValue {
    val text = value.text
    val start = minOf(value.selection.start, value.selection.end)
    val end = maxOf(value.selection.start, value.selection.end)
    val m = marker.length
    val wrapped = start >= m && end + m <= text.length &&
        text.substring(start - m, start) == marker && text.substring(end, end + m) == marker
    return if (wrapped) {
        val newText = text.substring(0, start - m) + text.substring(start, end) + text.substring(end + m)
        TextFieldValue(newText, TextRange(start - m, end - m))
    } else {
        val newText = text.substring(0, start) + marker + text.substring(start, end) + marker + text.substring(end)
        TextFieldValue(newText, TextRange(start + m, end + m))
    }
}
