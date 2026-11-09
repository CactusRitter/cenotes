package com.example.notas.ui.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.notas.ui.theme.CheckBoxIcon
import com.example.notas.ui.theme.CheckBoxOutlineIcon

@Composable
fun inlineColors() = InlineColors(
    link = MaterialTheme.colorScheme.primary,
    codeBackground = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
)

/** Full rendering for the note view: checkboxes are tappable, links open. */
@Composable
fun MarkdownView(
    source: String,
    onToggleTask: (line: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = inlineColors()
    val body = MaterialTheme.typography.bodyLarge
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        parseMarkdown(source).forEach { block ->
            when (block) {
                is MdBlock.Heading -> Text(
                    inlineMarkdown(block.text, colors),
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineSmall
                        2 -> MaterialTheme.typography.titleLarge
                        else -> MaterialTheme.typography.titleMedium
                    },
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                )
                is MdBlock.Task -> Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = (block.indent * 20).dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Checkbox) { onToggleTask(block.line) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // The whole row is the touch target, so the checkbox itself can be compact.
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                        Checkbox(
                            checked = block.checked,
                            onCheckedChange = { onToggleTask(block.line) },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.padding(start = 2.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                        )
                    }
                    Text(
                        inlineMarkdown(block.text, colors),
                        style = if (block.checked) {
                            body.copy(
                                textDecoration = TextDecoration.LineThrough,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else body,
                    )
                }
                is MdBlock.Bullet -> ListRow(block.indent, "•", inlineMarkdown(block.text, colors), body)
                is MdBlock.Numbered -> ListRow(block.indent, block.number, inlineMarkdown(block.text, colors), body)
                is MdBlock.Quote -> Row(Modifier.height(IntrinsicSize.Min).padding(vertical = 2.dp)) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        inlineMarkdown(block.text, colors),
                        style = body.copy(fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                }
                is MdBlock.Code -> Text(
                    block.text,
                    style = body.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.codeBackground)
                        .padding(12.dp),
                )
                MdBlock.Rule -> HorizontalDivider(Modifier.padding(vertical = 10.dp))
                is MdBlock.Paragraph -> Text(inlineMarkdown(block.text, colors), style = body)
                MdBlock.Blank -> Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ListRow(indent: Int, marker: String, text: AnnotatedString, style: TextStyle) {
    Row(Modifier.padding(start = (12 + indent * 20).dp, top = 2.dp, bottom = 2.dp)) {
        Text(
            marker,
            style = style,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp),
        )
        Text(text, style = style)
    }
}

/** Compact, non-interactive rendering for the note cards in the list. */
@Composable
fun MarkdownPreview(source: String, maxBlocks: Int, modifier: Modifier = Modifier) {
    val colors = inlineColors()
    val style = MaterialTheme.typography.bodyMedium
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val blocks = parseMarkdown(source).filter { it != MdBlock.Blank }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        blocks.take(maxBlocks).forEach { block ->
            when (block) {
                is MdBlock.Task -> Row(
                    Modifier.padding(start = (block.indent * 12).dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (block.checked) CheckBoxIcon else CheckBoxOutlineIcon,
                        contentDescription = null,
                        tint = if (block.checked) MaterialTheme.colorScheme.primary else muted,
                        modifier = Modifier.size(18.dp),
                    )
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
                MdBlock.Blank -> Unit
            }
        }
        if (blocks.size > maxBlocks) Text("…", style = style, color = muted)
    }
}
