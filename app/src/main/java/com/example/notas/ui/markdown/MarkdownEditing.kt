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

private val CodeSpan = Regex("""`([^`\n]+)`""")
private val LinkSpan = Regex("""\[([^\]\n]+)]\(([^)\s]*)\)""")
private val BoldSpan = Regex("""(\*\*|__)(?=\S)(.+?)(?<=\S)\1""")
private val StrikeSpan = Regex("""~~(?=\S)(.+?)(?<=\S)~~""")
private val ItalicStarSpan = Regex("""(?<![\w*])\*(?![\s*])(.+?)(?<![\s*])\*(?![\w*])""")
private val ItalicUnderscoreSpan = Regex("""(?<![\w_])_(?![\s_])(.+?)(?<![\s_])_(?![\w_])""")
/** Marker pairs inserted by the toolbar before anything is typed between them. */
private val EmptyPairs = Regex("""\*\*\*\*|~~~~|(?<![\w_])__(?![\w_])|(?<!`)``(?!`)""")
private val BareUrl = Regex("""https?://[^\s)]+""")

/**
 * WYSIWYG for inline Markdown: `**bold**` is shown as **bold** with the asterisks hidden.
 * The underlying text keeps the markers (that is what gets saved); the offset mapping skips them.
 */
data class LiveInlineMarkdown(val linkColor: Color, val codeBackground: Color) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val s = text.text
        val n = s.length
        val hidden = BooleanArray(n)
        val inCode = BooleanArray(n)
        val styles = ArrayList<Triple<SpanStyle, Int, Int>>()

        fun free(range: IntRange) = range.none { inCode[it] }
        fun hide(from: Int, toExclusive: Int) {
            for (i in from until toExclusive) hidden[i] = true
        }

        CodeSpan.findAll(s).forEach { m ->
            val a = m.range.first
            val b = m.range.last
            hide(a, a + 1)
            hide(b, b + 1)
            for (i in a + 1 until b) inCode[i] = true
            styles += Triple(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground), a + 1, b)
        }
        LinkSpan.findAll(s).filter { free(it.range) }.forEach { m ->
            val label = m.groups[1]!!.range
            hide(m.range.first, label.first)
            hide(label.last + 1, m.range.last + 1)
            styles += Triple(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline), label.first, label.last + 1)
        }
        fun pair(regex: Regex, markerLen: Int, style: SpanStyle, inner: Int) {
            regex.findAll(s).filter { free(it.range) }.forEach { m ->
                hide(m.range.first, m.range.first + markerLen)
                hide(m.range.last + 1 - markerLen, m.range.last + 1)
                val r = m.groups[inner]!!.range
                styles += Triple(style, r.first, r.last + 1)
            }
        }
        pair(BoldSpan, 2, SpanStyle(fontWeight = FontWeight.Bold), 2)
        pair(StrikeSpan, 2, SpanStyle(textDecoration = TextDecoration.LineThrough), 1)
        pair(ItalicStarSpan, 1, SpanStyle(fontStyle = FontStyle.Italic), 1)
        pair(ItalicUnderscoreSpan, 1, SpanStyle(fontStyle = FontStyle.Italic), 1)
        EmptyPairs.findAll(s).filter { free(it.range) }.forEach { hide(it.range.first, it.range.last + 1) }
        BareUrl.findAll(s).filter { free(it.range) }.forEach { m ->
            styles += Triple(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline), m.range.first, m.range.last + 1)
        }

        // keptBefore[i] = how many visible characters come before original index i.
        val keptBefore = IntArray(n + 1)
        val visible = IntArray(n)
        var count = 0
        val out = StringBuilder(n)
        for (i in 0 until n) {
            keptBefore[i] = count
            if (!hidden[i]) {
                visible[count++] = i
                out.append(s[i])
            }
        }
        keptBefore[n] = count

        val builder = AnnotatedString.Builder(out.toString())
        styles.forEach { (style, a, b) ->
            val ta = keptBefore[a]
            val tb = keptBefore[b]
            if (tb > ta) builder.addStyle(style, ta, tb)
        }

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = keptBefore[offset.coerceIn(0, n)]
            override fun transformedToOriginal(offset: Int) =
                if (offset >= count) n else visible[offset.coerceAtLeast(0)]
        }
        return TransformedText(builder.toAnnotatedString(), mapping)
    }
}

/** Expands a collapsed cursor to the word around it (so "B" with no selection bolds that word). */
private fun selectWord(value: TextFieldValue): TextFieldValue {
    if (!value.selection.collapsed) return value
    val t = value.text
    var a = value.selection.start
    var b = a
    while (a > 0 && t[a - 1].isLetterOrDigit()) a--
    while (b < t.length && t[b].isLetterOrDigit()) b++
    return value.copy(selection = TextRange(a, b))
}

private fun runBefore(t: String, index: Int, c: Char): Int {
    var k = 0
    while (index - k - 1 >= 0 && t[index - k - 1] == c) k++
    return k
}

private fun runAfter(t: String, index: Int, c: Char): Int {
    var k = 0
    while (index + k < t.length && t[index + k] == c) k++
    return k
}

/**
 * Toolbar bold / italic. With a selection (or a word under the cursor) it toggles the style on
 * it; with the cursor on empty space it inserts an (invisible) empty pair to type into.
 */
fun toggleEmphasis(value: TextFieldValue, bold: Boolean): TextFieldValue {
    val v = selectWord(value)
    val start = minOf(v.selection.start, v.selection.end)
    val end = maxOf(v.selection.start, v.selection.end)
    val t = v.text
    if (start == end) {
        val pair = if (bold) "**" else "_"
        return TextFieldValue(t.substring(0, start) + pair + pair + t.substring(start), TextRange(start + pair.length))
    }
    for (c in listOf('*', '_')) {
        val k = minOf(runBefore(t, start, c), runAfter(t, end, c))
        val active = if (bold) k >= 2 else (k == 1 || k >= 3)
        if (active) {
            val m = if (bold) 2 else 1
            val newText = t.substring(0, start - m) + t.substring(start, end) + t.substring(end + m)
            return TextFieldValue(newText, TextRange(start - m, end - m))
        }
    }
    val marker = if (bold) "**" else "*"
    val newText = t.substring(0, start) + marker + t.substring(start, end) + marker + t.substring(end)
    return TextFieldValue(newText, TextRange(start + marker.length, end + marker.length))
}

/** Toolbar strikethrough / code: wraps the selection (or word) in [marker], or unwraps it. */
fun toggleWrap(value: TextFieldValue, marker: String): TextFieldValue {
    val v = selectWord(value)
    val text = v.text
    val start = minOf(v.selection.start, v.selection.end)
    val end = maxOf(v.selection.start, v.selection.end)
    val m = marker.length
    if (start == end) {
        return TextFieldValue(text.substring(0, start) + marker + marker + text.substring(start), TextRange(start + m))
    }
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
