package com.example.notas.ui.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle

/**
 * A small, line-based Markdown dialect for notes: headings, checklists (`- [ ]` / `- [x]`),
 * bullet and numbered lists, quotes, fenced code, rules, and inline bold/italic/strike/code/links.
 * Every block remembers the source line it came from, so checkboxes can be toggled in place.
 */
sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Task(val checked: Boolean, val text: String, val indent: Int, val line: Int) : MdBlock
    data class Bullet(val text: String, val indent: Int) : MdBlock
    data class Numbered(val number: String, val text: String, val indent: Int) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Code(val text: String) : MdBlock
    data object Rule : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data object Blank : MdBlock
}

internal val TaskRegex = Regex("""^(\s*)[-*+]\s+\[([ xX])]\s?(.*)$""")
internal val BulletRegex = Regex("""^(\s*)([-*+])\s+(.*)$""")
internal val NumberedRegex = Regex("""^(\s*)(\d+)([.)])\s+(.*)$""")
internal val HeadingRegex = Regex("""^(#{1,6})\s+(.*)$""")
internal val QuoteRegex = Regex("""^>\s?(.*)$""")
internal val RuleRegex = Regex("""^\s*([-*_])(\s*\1){2,}\s*$""")
internal val FenceRegex = Regex("""^\s*```.*$""")

internal fun indentLevel(spaces: String) = spaces.replace("\t", "    ").length / 2

fun parseMarkdown(source: String): List<MdBlock> {
    val lines = source.split('\n')
    val blocks = mutableListOf<MdBlock>()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        if (FenceRegex.matches(line)) {
            val code = StringBuilder()
            i++
            while (i < lines.size && !FenceRegex.matches(lines[i])) {
                if (code.isNotEmpty()) code.append('\n')
                code.append(lines[i])
                i++
            }
            blocks += MdBlock.Code(code.toString())
            i++ // closing fence (or end of text)
            continue
        }
        blocks += when {
            line.isBlank() -> MdBlock.Blank
            RuleRegex.matches(line) -> MdBlock.Rule
            else -> TaskRegex.matchEntire(line)?.let {
                MdBlock.Task(it.groupValues[2] != " ", it.groupValues[3], indentLevel(it.groupValues[1]), i)
            } ?: HeadingRegex.matchEntire(line)?.let {
                MdBlock.Heading(it.groupValues[1].length, it.groupValues[2])
            } ?: BulletRegex.matchEntire(line)?.let {
                MdBlock.Bullet(it.groupValues[3], indentLevel(it.groupValues[1]))
            } ?: NumberedRegex.matchEntire(line)?.let {
                MdBlock.Numbered(it.groupValues[2] + it.groupValues[3], it.groupValues[4], indentLevel(it.groupValues[1]))
            } ?: QuoteRegex.matchEntire(line)?.let {
                MdBlock.Quote(it.groupValues[1])
            } ?: MdBlock.Paragraph(line)
        }
        i++
    }
    // Trailing blank lines add nothing when rendering.
    while (blocks.lastOrNull() == MdBlock.Blank) blocks.removeAt(blocks.lastIndex)
    return blocks
}

data class TaskStats(val done: Int, val total: Int)

fun taskStats(source: String): TaskStats {
    var done = 0
    var total = 0
    source.lineSequence().forEach { line ->
        TaskRegex.matchEntire(line)?.let {
            total++
            if (it.groupValues[2] != " ") done++
        }
    }
    return TaskStats(done, total)
}

/** Plain text for sharing: drops the most noisy markers but keeps checkboxes readable. */
fun markdownToShareText(source: String): String =
    source.lines().joinToString("\n") { line ->
        TaskRegex.matchEntire(line)?.let {
            it.groupValues[1] + (if (it.groupValues[2] == " ") "☐ " else "☑ ") + it.groupValues[3]
        } ?: line
    }

private val InlineRegex = Regex(
    """(\*\*|__)(.+?)\1""" + // 1,2 bold
        """|~~(.+?)~~""" + // 3 strike
        """|`([^`]+)`""" + // 4 code
        """|\[([^\]]+)]\(([^)\s]+)\)""" + // 5,6 link
        """|(?<![\w*])\*(?![\s*])(.+?)(?<![\s*])\*(?![\w*])""" + // 7 italic *
        """|(?<![\w_])_(?![\s_])(.+?)(?<![\s_])_(?![\w_])""" + // 8 italic _
        """|(https?://[^\s)]+)""", // 9 bare URL
)

data class InlineColors(val link: Color, val codeBackground: Color)

/** Renders inline Markdown to styled text. [links] = false keeps links unclickable (for previews). */
fun inlineMarkdown(text: String, colors: InlineColors, links: Boolean = true): AnnotatedString =
    buildAnnotatedString { appendInline(text, colors, links) }

private fun AnnotatedString.Builder.appendInline(text: String, colors: InlineColors, links: Boolean) {
    var last = 0
    for (m in InlineRegex.findAll(text)) {
        append(text.substring(last, m.range.first))
        val g = m.groups
        when {
            g[2] != null -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { appendInline(g[2]!!.value, colors, links) }
            g[3] != null -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                appendInline(g[3]!!.value, colors, links)
            }
            g[4] != null -> withStyle(
                SpanStyle(fontFamily = FontFamily.Monospace, background = colors.codeBackground),
            ) { append(g[4]!!.value) }
            g[5] != null -> appendLink(g[5]!!.value, g[6]!!.value, colors, links)
            g[7] != null -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { appendInline(g[7]!!.value, colors, links) }
            g[8] != null -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { appendInline(g[8]!!.value, colors, links) }
            g[9] != null -> appendLink(g[9]!!.value, g[9]!!.value, colors, links)
        }
        last = m.range.last + 1
    }
    append(text.substring(last))
}

private fun AnnotatedString.Builder.appendLink(label: String, url: String, colors: InlineColors, links: Boolean) {
    val style = SpanStyle(color = colors.link, textDecoration = TextDecoration.Underline)
    if (links) {
        withLink(LinkAnnotation.Url(url, TextLinkStyles(style))) { append(label) }
    } else {
        withStyle(style) { append(label) }
    }
}
