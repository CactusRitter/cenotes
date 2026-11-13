package com.cenotes.app.ui.markdown

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

enum class BlockType { PARAGRAPH, H1, H2, H3, TASK, BULLET, NUMBERED, QUOTE, CODE, DIVIDER, IMAGE }

/** Blocks that continue on Enter. */
private val ContinuingTypes = setOf(BlockType.TASK, BlockType.BULLET, BlockType.NUMBERED, BlockType.QUOTE)

/** Blocks that can be nested (sub-tasks, sub-bullets). */
val NestableTypes = setOf(BlockType.TASK, BlockType.BULLET, BlockType.NUMBERED)

const val MaxIndent = 4

/** One piece of the note as the user sees it: a paragraph, a heading, a checkbox item, an image… */
@Stable
class EditorBlock(
    val id: Long,
    type: BlockType,
    checked: Boolean = false,
    indent: Int = 0,
    value: TextFieldValue = TextFieldValue(""),
    imageName: String = "",
    small: Boolean = false,
) {
    var type by mutableStateOf(type)
    var checked by mutableStateOf(checked)
    var indent by mutableStateOf(indent)
    /** Text with inline Markdown (`**bold**`…); the markers are hidden on screen. */
    var value by mutableStateOf(value)
    /** For IMAGE blocks: file name inside the app's image folder. */
    var imageName by mutableStateOf(imageName)
    /** For IMAGE blocks: thumbnail (true) or wide (false). */
    var small by mutableStateOf(small)
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

    /** Last text block that had the keyboard; where images and links go after a dialog/picker. */
    var lastFocusedId by mutableStateOf<Long?>(null)

    /** Block that should receive focus on the next frame (after a split, merge, etc.). */
    var pendingFocus by mutableStateOf<Long?>(null)

    init {
        load(markdown)
    }

    val focused: EditorBlock? get() = blocks.firstOrNull { it.id == focusedId }

    fun onFocus(block: EditorBlock, focused: Boolean) {
        if (focused) {
            focusedId = block.id
            lastFocusedId = block.id
        } else if (focusedId == block.id) {
            focusedId = null
        }
    }

    fun load(markdown: String) {
        blocks.clear()
        blocks += parseBlocks(markdown)
        if (blocks.isEmpty()) blocks += block(BlockType.PARAGRAPH)
        recomputeParents()
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
            val image = ImageRegex.matchEntire(line)
            val task = TaskRegex.matchEntire(line)
            val heading = HeadingRegex.matchEntire(line)
            val bullet = BulletRegex.matchEntire(line)
            val numbered = NumberedRegex.matchEntire(line)
            val quote = QuoteRegex.matchEntire(line)
            out += when {
                image != null -> EditorBlock(
                    nextId++, BlockType.IMAGE, imageName = image.groupValues[2], small = image.groupValues[1] == "small",
                )
                task != null -> block(
                    BlockType.TASK, task.groupValues[3], task.groupValues[2] != " ",
                    indentLevel(task.groupValues[1]).coerceAtMost(MaxIndent),
                )
                heading != null -> block(
                    when (heading.groupValues[1].length) { 1 -> BlockType.H1; 2 -> BlockType.H2; else -> BlockType.H3 },
                    heading.groupValues[2],
                )
                RuleRegex.matches(line) -> block(BlockType.DIVIDER)
                bullet != null -> block(
                    BlockType.BULLET, bullet.groupValues[3],
                    indent = indentLevel(bullet.groupValues[1]).coerceAtMost(MaxIndent),
                )
                numbered != null -> block(
                    BlockType.NUMBERED, numbered.groupValues[4],
                    indent = indentLevel(numbered.groupValues[1]).coerceAtMost(MaxIndent),
                )
                quote != null -> block(BlockType.QUOTE, quote.groupValues[1])
                else -> block(BlockType.PARAGRAPH, line)
            }
            i++
        }
        return out
    }

    fun toMarkdown(): String {
        // Numbering restarts per nesting level, like in any word processor.
        val counters = IntArray(MaxIndent + 1)
        val lines = blocks.map { b ->
            val text = b.value.text
            val pad = "  ".repeat(b.indent)
            if (b.type == BlockType.NUMBERED) {
                counters[b.indent]++
                for (l in b.indent + 1..MaxIndent) counters[l] = 0
            } else if (b.type !in NestableTypes) {
                counters.fill(0)
            }
            when (b.type) {
                BlockType.PARAGRAPH -> text
                BlockType.H1 -> "# $text"
                BlockType.H2 -> "## $text"
                BlockType.H3 -> "### $text"
                BlockType.TASK -> "$pad- [${if (b.checked) "x" else " "}] $text"
                BlockType.BULLET -> "$pad- $text"
                BlockType.NUMBERED -> "$pad${counters[b.indent]}. $text"
                BlockType.QUOTE -> "> $text"
                BlockType.CODE -> "```\n$text\n```"
                BlockType.DIVIDER -> "---"
                BlockType.IMAGE -> "![${if (b.small) "small" else ""}](${b.imageName})"
            }
        }
        return lines.joinToString("\n").trimEnd('\n').let { if (it.isBlank()) "" else it }
    }

    /** Number shown in front of each NUMBERED block (per nesting level). */
    fun numberLabels(): Map<Long, Int> {
        val counters = IntArray(MaxIndent + 1)
        val labels = HashMap<Long, Int>()
        blocks.forEach { b ->
            if (b.type == BlockType.NUMBERED) {
                counters[b.indent]++
                for (l in b.indent + 1..MaxIndent) counters[l] = 0
                labels[b.id] = counters[b.indent]
            } else if (b.type !in NestableTypes) {
                counters.fill(0)
            }
        }
        return labels
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
            // Enter right after a bold/italic word: the hidden closing markers stay with that word.
            for (p in 1 until parts.size) {
                val lead = parts[p].takeWhile { it in "*_~`" }
                if (lead.isNotEmpty() && parts[p - 1].contains(lead)) {
                    parts[p - 1] += lead
                    parts[p] = parts[p].drop(lead.length)
                }
            }
            // Enter on an empty list item: first step out of a sub-list, then end the list.
            if (parts.size == 2 && parts.all { it.isEmpty() } && block.type in ContinuingTypes) {
                if (block.indent > 0) {
                    block.indent--
                } else {
                    block.type = BlockType.PARAGRAPH
                    block.checked = false
                }
                block.value = TextFieldValue("")
                recomputeParents()
                return
            }
            val nextType = if (block.type in ContinuingTypes) block.type else BlockType.PARAGRAPH
            block.value = TextFieldValue(parts[0], TextRange(parts[0].length))
            var insertAt = index + 1
            var last: EditorBlock? = null
            parts.drop(1).forEach { text ->
                val nb = block(nextType, text, indent = if (nextType in NestableTypes) block.indent else 0)
                applyShortcut(nb)
                blocks.add(insertAt++, nb)
                last = nb
            }
            // Plain Enter: cursor at the start of the new line. Paste: at the end of what was pasted.
            val target = last!!
            if (parts.size > 2) target.value = target.value.copy(selection = TextRange(target.value.text.length))
            pendingFocus = target.id
            recomputeParents()
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
            if (type !in NestableTypes) block.indent = 0
            val rest = text.substring(prefixLen)
            block.value = TextFieldValue(rest, TextRange((block.value.selection.start - prefixLen).coerceIn(0, rest.length)))
            recomputeParents()
        }
        val taskPrefix = Regex("""^(?:[-*+]\s+)?\[( |x|X)?]\s""").find(text)
        val numberPrefix = Regex("""^\d+[.)]\s""").find(text)
        when (block.type) {
            BlockType.PARAGRAPH -> when {
                taskPrefix != null -> convert(taskPrefix.value.length, BlockType.TASK, taskPrefix.groupValues[1].isNotBlank())
                text.startsWith("### ") -> convert(4, BlockType.H3)
                text.startsWith("## ") -> convert(3, BlockType.H2)
                text.startsWith("# ") -> convert(2, BlockType.H1)
                Regex("""^[-*+]\s""").containsMatchIn(text) -> convert(2, BlockType.BULLET)
                numberPrefix != null -> convert(numberPrefix.value.length, BlockType.NUMBERED)
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
        if (block.indent > 0) {
            block.indent--
            recomputeParents()
            return true
        }
        if (block.type != BlockType.PARAGRAPH) {
            // First backspace removes the formatting (checkbox, bullet…), like in Notion.
            block.type = BlockType.PARAGRAPH
            block.checked = false
            recomputeParents()
            return true
        }
        if (index == 0) return false
        val prev = blocks[index - 1]
        when (prev.type) {
            BlockType.DIVIDER -> {
                blocks.removeAt(index - 1)
                return true
            }
            // Images are only removed on purpose, from their own menu.
            BlockType.IMAGE -> return true
            else -> Unit
        }
        val join = prev.value.text.length
        prev.value = TextFieldValue(prev.value.text + block.value.text, TextRange(join))
        blocks.removeAt(index)
        pendingFocus = prev.id
        recomputeParents()
        return true
    }

    // ---- Checklists with sub-tasks ----

    /** Indices of the blocks nested under [index] (deeper indent, directly below it). */
    private fun descendants(index: Int): IntRange {
        val parent = blocks[index]
        var end = index
        while (end + 1 < blocks.size && blocks[end + 1].type in NestableTypes && blocks[end + 1].indent > parent.indent) end++
        return index + 1..end
    }

    /** Settings → Notes: ticked items sink to the end of their list, and unticked ones come back up (like Keep). */
    var checkedToBottom = false

    /** Ticking a task ticks all its sub-tasks; the parents then follow their children. */
    fun toggleChecked(block: EditorBlock) {
        val index = blocks.indexOf(block)
        if (index < 0) return
        val checked = !block.checked
        block.checked = checked
        for (i in descendants(index)) if (blocks[i].type == BlockType.TASK) blocks[i].checked = checked
        recomputeParents()
        if (checkedToBottom) sortChecked()
    }

    /**
     * Within every run of tasks that sit next to each other at the same level, the unticked ones go first and the ticked
     * ones last, each keeping its order and taking its sub-tasks along. Sub-task lists are sorted the same way.
     */
    private fun sortChecked() {
        val sorted = arrange(blocks.toList())
        if (sorted.map { it.id } != blocks.map { it.id }) {
            blocks.clear()
            blocks.addAll(sorted)
        }
    }

    /** One item of a list together with the items nested under it. */
    private class ListItem(val head: EditorBlock, val nested: List<EditorBlock>)

    private fun arrange(list: List<EditorBlock>): List<EditorBlock> {
        if (list.isEmpty()) return list
        val base = list.minOf { if (it.type in NestableTypes) it.indent else 0 }
        val units = mutableListOf<ListItem>()
        var i = 0
        while (i < list.size) {
            val head = list[i]
            var end = i
            if (head.type in NestableTypes && head.indent == base) {
                while (end + 1 < list.size && list[end + 1].type in NestableTypes && list[end + 1].indent > base) end++
            }
            units += ListItem(head, arrange(list.subList(i + 1, end + 1)))
            i = end + 1
        }
        val result = mutableListOf<EditorBlock>()
        val run = mutableListOf<ListItem>()
        fun flush() {
            (run.filterNot { it.head.checked } + run.filter { it.head.checked }).forEach { result += it.head; result += it.nested }
            run.clear()
        }
        for (u in units) {
            if (u.head.type == BlockType.TASK && u.head.indent == base) {
                run += u
            } else {
                flush()
                result += u.head
                result += u.nested
            }
        }
        flush()
        return result
    }

    /**
     * Moves [block] (with the items nested under it) next to [target]: just before it, or after it and its own
     * nested items. The moved item takes the target's level. Used when an item is dragged by its handle.
     */
    fun moveItem(block: EditorBlock, target: EditorBlock, after: Boolean) {
        if (block.id == target.id) return
        val from = blocks.indexOf(block)
        if (from < 0) return
        val last = maxOf(from, descendants(from).last)
        if (blocks.indexOf(target) in from..last) return
        val unit = blocks.subList(from, last + 1).toList()
        repeat(unit.size) { blocks.removeAt(from) }
        val t = blocks.indexOf(target)
        if (t < 0) {
            blocks.addAll(from, unit)
            return
        }
        val at = if (after) maxOf(t, descendants(t).last) + 1 else t
        val shift = target.indent - block.indent
        unit.forEach { it.indent = (it.indent + shift).coerceIn(0, MaxIndent) }
        blocks.addAll(at, unit)
        recomputeParents()
    }

    /** A task with sub-tasks is done exactly when all of its sub-tasks are done. */
    fun recomputeParents() {
        for (i in blocks.indices.reversed()) {
            val b = blocks[i]
            if (b.type != BlockType.TASK) continue
            val children = descendants(i).map { blocks[it] }.filter { it.type == BlockType.TASK }
            if (children.isNotEmpty()) b.checked = children.all { it.checked }
        }
    }

    fun canIndent(block: EditorBlock?): Boolean {
        if (block == null || block.type !in NestableTypes || block.indent >= MaxIndent) return false
        val index = blocks.indexOf(block)
        val prev = blocks.getOrNull(index - 1) ?: return false
        // A sub-item needs an item above it, at most one level shallower.
        return prev.type in NestableTypes && prev.indent >= block.indent
    }

    fun indentFocused() {
        val b = focused ?: return
        if (!canIndent(b)) return
        b.indent++
        recomputeParents()
    }

    fun outdentFocused() {
        val b = focused ?: return
        if (b.indent == 0) return
        b.indent--
        recomputeParents()
    }

    // ---- Toolbar ----

    /** Switch the focused block to [type], or back to plain text if it already is. */
    fun toggleType(type: BlockType) {
        val b = focused ?: return
        b.type = if (b.type == type) BlockType.PARAGRAPH else type
        b.checked = false
        if (b.type !in NestableTypes) b.indent = 0
        recomputeParents()
    }

    /** "H": text → H1 → H2 → H3 → text. */
    fun cycleHeading() {
        val b = focused ?: return
        b.type = when (b.type) {
            BlockType.H1 -> BlockType.H2
            BlockType.H2 -> BlockType.H3
            BlockType.H3 -> BlockType.PARAGRAPH
            else -> BlockType.H1
        }
        b.checked = false
        b.indent = 0
        recomputeParents()
    }

    fun editFocused(transform: (TextFieldValue) -> TextFieldValue) {
        val b = focused ?: return
        b.value = transform(b.value)
    }

    /** Replaces the selection of block [id] with a Markdown link. */
    fun insertLink(id: Long, selection: TextRange, label: String, url: String) {
        val b = blocks.firstOrNull { it.id == id } ?: return
        val t = b.value.text
        val start = minOf(selection.start, selection.end).coerceIn(0, t.length)
        val end = maxOf(selection.start, selection.end).coerceIn(0, t.length)
        val md = "[${label.ifBlank { url }}]($url)"
        b.value = TextFieldValue(t.substring(0, start) + md + t.substring(end), TextRange(start + md.length))
        pendingFocus = b.id
    }

    // ---- Images ----

    /**
     * Inserts images below the block the user was writing in. Images never go inside a list:
     * if that block is part of a list, they go below the whole list.
     */
    fun insertImages(names: List<String>) {
        if (names.isEmpty()) return
        var index = blocks.indexOfFirst { it.id == lastFocusedId }.takeIf { it >= 0 } ?: blocks.lastIndex
        if (blocks[index].type in ContinuingTypes) {
            while (index + 1 < blocks.size && blocks[index + 1].type in ContinuingTypes) index++
        }
        // Writing in an empty line: the images take its place (the empty line moves below them).
        val anchor = blocks[index]
        val insertAt = if (anchor.type == BlockType.PARAGRAPH && anchor.value.text.isEmpty()) index else index + 1
        val small = names.size > 1 // several at once look best as a gallery row
        names.forEachIndexed { i, name ->
            blocks.add(insertAt + i, EditorBlock(nextId++, BlockType.IMAGE, imageName = name, small = small))
        }
        val after = insertAt + names.size
        val next = blocks.getOrNull(after)
        if (next == null || next.type != BlockType.PARAGRAPH || next.value.text.isNotEmpty()) {
            blocks.add(after, block(BlockType.PARAGRAPH))
        }
        pendingFocus = blocks[after].id
    }

    fun removeImage(block: EditorBlock) {
        blocks.remove(block)
        if (blocks.isEmpty()) blocks += block(BlockType.PARAGRAPH)
    }

    fun imageNames(): List<String> = blocks.filter { it.type == BlockType.IMAGE }.map { it.imageName }

    // ---- Focus helpers ----

    /** Tap on the empty area under the note: continue writing at the end. */
    fun focusEnd() {
        val last = blocks.lastOrNull()
        if (last != null && last.type == BlockType.PARAGRAPH) {
            last.value = last.value.copy(selection = TextRange(last.value.text.length))
            pendingFocus = last.id
        } else {
            insertAfter(blocks.lastIndex, block(BlockType.PARAGRAPH))
        }
    }

    fun focusFirst() {
        blocks.firstOrNull { it.type != BlockType.IMAGE && it.type != BlockType.DIVIDER }?.let { pendingFocus = it.id }
    }
}
