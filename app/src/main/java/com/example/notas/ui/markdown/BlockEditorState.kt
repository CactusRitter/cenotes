package com.example.notas.ui.markdown

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

enum class BlockType { PARAGRAPH, H1, H2, H3, TASK, BULLET, NUMBERED, QUOTE, CODE, DIVIDER }

private val ListTypes = setOf(BlockType.TASK, BlockType.BULLET, BlockType.NUMBERED, BlockType.QUOTE)

/** One line of the note as the user sees it: a paragraph, a heading, a checkbox item… */
@Stable
class EditorBlock(
    val id: Long,
    type: BlockType,
    checked: Boolean = false,
    val indent: Int = 0,
    value: TextFieldValue = TextFieldValue(""),
) {
    var type by mutableStateOf(type)
    var checked by mutableStateOf(checked)
    /** Text with inline Markdown (`**bold**`…); the markers are hidden on screen. */
    var value by mutableStateOf(value)
}

/**
 * State of the visual (WYSIWYG) editor. The note is edited as a list of blocks and is
 * converted to and from Markdown, which stays the storage format.
 */
@Stable
class BlockEditorState(markdown: String) {
    private var nextId = 0L
    val blocks = mutableStateListOf<EditorBlock>()

    /** Block that currently has the keyboard (null when the title or nothing is focused). */
    var focusedId by mutableStateOf<Long?>(null)

    /** Block that should receive focus on the next frame (after a split, merge, etc.). */
    var pendingFocus by mutableStateOf<Long?>(null)

    init {
        load(markdown)
    }

    val focused: EditorBlock? get() = blocks.firstOrNull { it.id == focusedId }

    fun load(markdown: String) {
        blocks.clear()
        blocks += parseBlocks(markdown)
        if (blocks.isEmpty()) blocks += block(BlockType.PARAGRAPH)
    }

    private fun block(type: BlockType, text: String = "", checked: Boolean = false, indent: Int = 0, cursor: Int = 0) =
        EditorBlock(nextId++, type, checked, indent, TextFieldValue(text, TextRange(cursor.coerceIn(0, text.length))))

    // ---- Markdown <-> blocks ----

    private fun parseBlocks(markdown: String): List<EditorBlock> {
        if (markdown.isEmpty()) return emptyList()
        val out = mutableListOf<EditorBlock>()
        val lines = markdown.split('\n')
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (FenceRegex.matches(line)) {
                val code = mutableListOf<String>()
                i++
                while (i < lines.size && !FenceRegex.matches(lines[i])) code += lines[i++]
                out += block(BlockType.CODE, code.joinToString("\n"))
                i++
                continue
            }
            val task = TaskRegex.matchEntire(line)
            val heading = HeadingRegex.matchEntire(line)
            val bullet = BulletRegex.matchEntire(line)
            val numbered = NumberedRegex.matchEntire(line)
            val quote = QuoteRegex.matchEntire(line)
            out += when {
                task != null -> block(
                    BlockType.TASK, task.groupValues[3], task.groupValues[2] != " ", indentLevel(task.groupValues[1]),
                )
                heading != null -> block(
                    when (heading.groupValues[1].length) { 1 -> BlockType.H1; 2 -> BlockType.H2; else -> BlockType.H3 },
                    heading.groupValues[2],
                )
                RuleRegex.matches(line) -> block(BlockType.DIVIDER)
                bullet != null -> block(BlockType.BULLET, bullet.groupValues[3], indent = indentLevel(bullet.groupValues[1]))
                numbered != null -> block(
                    BlockType.NUMBERED, numbered.groupValues[4], indent = indentLevel(numbered.groupValues[1]),
                )
                quote != null -> block(BlockType.QUOTE, quote.groupValues[1])
                else -> block(BlockType.PARAGRAPH, line)
            }
            i++
        }
        return out
    }

    fun toMarkdown(): String {
        var number = 0
        val lines = blocks.map { b ->
            val text = b.value.text
            val pad = "  ".repeat(b.indent)
            if (b.type == BlockType.NUMBERED) number++ else number = 0
            when (b.type) {
                BlockType.PARAGRAPH -> text
                BlockType.H1 -> "# $text"
                BlockType.H2 -> "## $text"
                BlockType.H3 -> "### $text"
                BlockType.TASK -> "$pad- [${if (b.checked) "x" else " "}] $text"
                BlockType.BULLET -> "$pad- $text"
                BlockType.NUMBERED -> "$pad$number. $text"
                BlockType.QUOTE -> "> $text"
                BlockType.CODE -> "```\n$text\n```"
                BlockType.DIVIDER -> "---"
            }
        }
        // A single empty paragraph means an empty note.
        return lines.joinToString("\n").trimEnd('\n').let { if (it.isBlank()) "" else it }
    }

    // ---- Editing ----

    fun onValueChange(block: EditorBlock, new: TextFieldValue) {
        val index = blocks.indexOf(block)
        if (index < 0) return

        if (block.type == BlockType.CODE) {
            // Inside code, Enter is a normal line break; Enter on an empty last line leaves the block.
            if (new.text.endsWith("\n\n") && new.selection.start == new.text.length) {
                block.value = TextFieldValue(new.text.dropLast(2), TextRange(new.text.length - 2))
                insertAfter(index, block(BlockType.PARAGRAPH))
            } else {
                block.value = new
            }
            return
        }

        if ('\n' in new.text) {
            val parts = new.text.split('\n').toMutableList()
            // Enter right after a bold/italic word: the hidden closing markers stay with that word
            // instead of being pushed onto the new line.
            for (p in 1 until parts.size) {
                val lead = parts[p].takeWhile { it in "*_~`" }
                if (lead.isNotEmpty() && parts[p - 1].contains(lead)) {
                    parts[p - 1] += lead
                    parts[p] = parts[p].drop(lead.length)
                }
            }
            // Enter on an empty list item ends the list instead of adding another empty item.
            if (parts.size == 2 && parts.all { it.isEmpty() } && block.type in ListTypes) {
                block.type = BlockType.PARAGRAPH
                block.checked = false
                block.value = TextFieldValue("")
                return
            }
            block.value = TextFieldValue(parts[0], TextRange(parts[0].length))
            val nextType = if (block.type in ListTypes) block.type else BlockType.PARAGRAPH
            var insertAt = index + 1
            var last: EditorBlock? = null
            parts.drop(1).forEach { text ->
                val nb = block(nextType, text, indent = block.indent)
                applyShortcut(nb)
                blocks.add(insertAt++, nb)
                last = nb
            }
            // Plain Enter: cursor at the start of the new line. Paste: at the end of what was pasted.
            val target = last!!
            if (parts.size > 2) target.value = target.value.copy(selection = TextRange(target.value.text.length))
            pendingFocus = target.id
            return
        }

        block.value = new
        applyShortcut(block)
    }

    /** Markdown shortcuts typed at the start of a line turn into real formatting. */
    private fun applyShortcut(block: EditorBlock) {
        val text = block.value.text
        fun convert(prefixLen: Int, type: BlockType, checked: Boolean = false) {
            block.type = type
            block.checked = checked
            val rest = text.substring(prefixLen)
            block.value = TextFieldValue(rest, TextRange((block.value.selection.start - prefixLen).coerceIn(0, rest.length)))
        }
        val taskPrefix = Regex("""^(?:[-*+]\s+)?\[( |x|X)?]\s""").find(text)
        when (block.type) {
            BlockType.PARAGRAPH -> when {
                taskPrefix != null -> convert(taskPrefix.value.length, BlockType.TASK, taskPrefix.groupValues[1].isNotBlank())
                text.startsWith("### ") -> convert(4, BlockType.H3)
                text.startsWith("## ") -> convert(3, BlockType.H2)
                text.startsWith("# ") -> convert(2, BlockType.H1)
                Regex("""^[-*+]\s""").containsMatchIn(text) -> convert(2, BlockType.BULLET)
                Regex("""^\d+[.)]\s""").find(text) != null ->
                    convert(Regex("""^\d+[.)]\s""").find(text)!!.value.length, BlockType.NUMBERED)
                text.startsWith("> ") -> convert(2, BlockType.QUOTE)
                text.startsWith("```") -> convert(3, BlockType.CODE)
                text == "---" -> {
                    block.type = BlockType.DIVIDER
                    block.value = TextFieldValue("")
                    val index = blocks.indexOf(block)
                    if (index >= 0) insertAfter(index, block(BlockType.PARAGRAPH))
                }
            }
            // "- " already became a bullet; typing "[ ] " right after makes it a checkbox.
            BlockType.BULLET -> Regex("""^\[( |x|X)?]\s""").find(text)?.let {
                convert(it.value.length, BlockType.TASK, it.groupValues[1].isNotBlank())
            }
            else -> Unit
        }
    }

    private fun insertAfter(index: Int, block: EditorBlock) {
        blocks.add(index + 1, block)
        pendingFocus = block.id
    }

    /** Backspace with the cursor at the very start of a block. Returns true if it was handled. */
    fun onBackspaceAtStart(block: EditorBlock): Boolean {
        if (block.value.selection != TextRange(0)) return false
        val index = blocks.indexOf(block)
        if (index < 0) return false
        if (block.type != BlockType.PARAGRAPH) {
            // First backspace removes the formatting (checkbox, bullet…), like in Notion.
            block.type = BlockType.PARAGRAPH
            block.checked = false
            return true
        }
        if (index == 0) return false
        val prev = blocks[index - 1]
        if (prev.type == BlockType.DIVIDER) {
            blocks.removeAt(index - 1)
            return true
        }
        val join = prev.value.text.length
        prev.value = TextFieldValue(prev.value.text + block.value.text, TextRange(join))
        blocks.removeAt(index)
        pendingFocus = prev.id
        return true
    }

    fun toggleChecked(block: EditorBlock) {
        block.checked = !block.checked
    }

    /** Toolbar: switch the focused block to [type], or back to plain text if it already is. */
    fun toggleType(type: BlockType) {
        val b = focused ?: return
        b.type = if (b.type == type) BlockType.PARAGRAPH else type
        b.checked = false
    }

    /** Toolbar "H": text → H1 → H2 → H3 → text. */
    fun cycleHeading() {
        val b = focused ?: return
        b.type = when (b.type) {
            BlockType.H1 -> BlockType.H2
            BlockType.H2 -> BlockType.H3
            BlockType.H3 -> BlockType.PARAGRAPH
            else -> BlockType.H1
        }
        b.checked = false
    }

    fun editFocused(transform: (TextFieldValue) -> TextFieldValue) {
        val b = focused ?: return
        b.value = transform(b.value)
    }

    /** Tap on the empty area under the note: continue writing at the end. */
    fun focusEnd() {
        val last = blocks.lastOrNull()
        if (last != null && last.type == BlockType.PARAGRAPH && last.value.text.isEmpty()) {
            pendingFocus = last.id
        } else if (last != null && last.type == BlockType.PARAGRAPH) {
            last.value = last.value.copy(selection = TextRange(last.value.text.length))
            pendingFocus = last.id
        } else {
            insertAfter(blocks.lastIndex, block(BlockType.PARAGRAPH))
        }
    }

    fun focusFirst() {
        blocks.firstOrNull()?.let { pendingFocus = it.id }
    }
}
