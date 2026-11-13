package com.cenotes.app.ui.markdown

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cenotes.app.ui.theme.CheckBoxIcon
import com.cenotes.app.ui.theme.CheckBoxOutlineIcon

@Composable
fun inlineColors() = InlineColors(
    link = MaterialTheme.colorScheme.primary,
    codeBackground = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
)

/** Compact, non-interactive rendering for the note cards in the list. */
@Composable
fun MarkdownPreview(source: String, maxBlocks: Int, modifier: Modifier = Modifier) {
    val colors = inlineColors()
    val style = MaterialTheme.typography.bodyMedium
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    // Images are shown as the card's cover instead.
    val blocks = parseMarkdown(source).filter { it != MdBlock.Blank && it !is MdBlock.Image }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        blocks.take(maxBlocks).forEach { block ->
            when (block) {
                is MdBlock.Task -> Row(
                    Modifier.padding(start = (block.indent * 12).dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    com.cenotes.app.ui.glass.TaskCheckbox(block.checked, onCheckedChange = null, size = 15.dp) {
                        Icon(
                            if (block.checked) CheckBoxIcon else CheckBoxOutlineIcon,
                            contentDescription = null,
                            tint = if (block.checked) MaterialTheme.colorScheme.primary else muted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        inlineMarkdown(block.text, colors, links = false),
                        style = if (block.checked) style.copy(textDecoration = TextDecoration.LineThrough, color = muted)
                        else style,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                is MdBlock.Heading -> Text(
                    inlineMarkdown(block.text, colors, links = false),
                    style = style, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                is MdBlock.Bullet -> Text(
                    AnnotatedString("•  ") + inlineMarkdown(block.text, colors, links = false),
                    style = style, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = (block.indent * 12).dp),
                )
                is MdBlock.Numbered -> Text(
                    AnnotatedString("${block.number}  ") + inlineMarkdown(block.text, colors, links = false),
                    style = style, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = (block.indent * 12).dp),
                )
                is MdBlock.Quote -> Text(
                    inlineMarkdown(block.text, colors, links = false),
                    style = style.copy(fontStyle = FontStyle.Italic, color = muted),
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                is MdBlock.Code -> Text(
                    block.text, style = style.copy(fontFamily = FontFamily.Monospace),
                    maxLines = 3, overflow = TextOverflow.Ellipsis,
                )
                MdBlock.Rule -> HorizontalDivider(Modifier.padding(vertical = 4.dp))
                is MdBlock.Paragraph -> Text(
                    inlineMarkdown(block.text, colors, links = false),
                    style = style, maxLines = 3, overflow = TextOverflow.Ellipsis,
                )
                MdBlock.Blank, is MdBlock.Image -> Unit
            }
        }
        if (blocks.size > maxBlocks) Text("…", style = style, color = muted)
    }
}
