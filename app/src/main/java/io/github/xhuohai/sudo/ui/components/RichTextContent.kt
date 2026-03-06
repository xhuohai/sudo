package io.github.xhuohai.sudo.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.github.xhuohai.sudo.utils.EmojiUtils

/**
 * Optimized rich text content with inline emoji images and clickable links.
 * Supports bold, italic, strikethrough, headings, inline code, and emoji rendering.
 */
@Composable
fun RichTextContent(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onLinkClick: ((String) -> Unit)? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: androidx.compose.ui.text.style.TextOverflow = androidx.compose.ui.text.style.TextOverflow.Clip
) {
    val context = LocalContext.current
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBackgroundColor = MaterialTheme.colorScheme.surfaceVariant
    
    // Memoize all expensive computations based on the text input
    val (annotatedString, inlineContent, hasLinks) = remember(text, linkColor, codeBackgroundColor, color) {
        computeRichTextContent(text, linkColor, codeBackgroundColor, color)
    }
    
    if (hasLinks) {
        // Use ClickableText for links with inline content
        ClickableText(
            text = annotatedString,
            modifier = modifier,
            style = style.copy(color = color),
            maxLines = maxLines,
            overflow = overflow,
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        var url = annotation.item
                        if (!url.startsWith("http")) {
                            url = "https://linux.do\$url"
                        }
                        
                        if (onLinkClick != null) {
                            onLinkClick(url)
                        } else {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
            }
        )
    } else if (inlineContent.isEmpty()) {
        // No inline images needed, use simple Text
        Text(
            text = annotatedString,
            modifier = modifier,
            style = style,
            color = color,
            overflow = overflow,
            maxLines = maxLines
        )
    } else {
        Text(
            text = annotatedString,
            modifier = modifier,
            style = style,
            color = color,
            inlineContent = inlineContent,
            overflow = overflow,
            maxLines = maxLines
        )
    }
}

/**
 * Horizontal rule (divider) composable
 */
@Composable
fun HorizontalRule(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

/**
 * Pre-compute the annotated string and inline content map.
 * This is done outside of composition to avoid blocking the UI thread.
 */
private fun computeRichTextContent(
    text: String, 
    linkColor: Color,
    codeBackgroundColor: Color,
    textColor: Color
): Triple<androidx.compose.ui.text.AnnotatedString, Map<String, InlineTextContent>, Boolean> {
    // First convert standard emojis using static map
    val textWithStandardEmoji = EmojiUtils.convertShortcodes(text)
    
    // Define patterns for various formatting
    // Use non-greedy matching (.*?) to allow emoji placeholders inside
    val linkPattern = Regex("""\[([^\]]+)\]\(([^)]+)\)""")
    val inlineCodePattern = Regex("""`([^`]+)`""")
    val emojiImagePattern = Regex("""\[\[EMOJI:([^|]+)\|([^\]]*)\]\]""")
    // Bold: match ** followed by content (can include emoji placeholders) until **
    val boldPattern = Regex("""\*\*(.+?)\*\*""")
    // Italic: single * not followed by another *, match until single *
    val italicPattern = Regex("""(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)""")
    val strikethroughPattern = Regex("""~~(.+?)~~""")
    // Heading patterns (must be at start of line) - allow any content including emoji
    val headingPattern = Regex("""(?m)^(#{1,6})\s+(.+)$""")
    // Table row pattern
    val tableRowPattern = Regex("""\|([^|\n]+(?:\|[^|\n]+)+)\|?""")
    
    // Combined segment data
    data class TextSegment(
        val start: Int,
        val end: Int,
        val type: String,
        val content: String,
        val extra: String = ""
    )
    
    val segments = mutableListOf<TextSegment>()
    
    // Find all patterns and add to segments
    // IMPORTANT: Detect formatting patterns BEFORE emoji so that **[[EMOJI]]text** is matched as bold
    
    linkPattern.findAll(textWithStandardEmoji).forEach { match ->
        segments.add(TextSegment(
            start = match.range.first,
            end = match.range.last + 1,
            type = "link",
            content = match.groupValues[1],
            extra = match.groupValues[2]
        ))
    }
    
    inlineCodePattern.findAll(textWithStandardEmoji).forEach { match ->
        val overlaps = segments.any { seg ->
            match.range.first < seg.end && match.range.last >= seg.start
        }
        if (!overlaps) {
            segments.add(TextSegment(
                start = match.range.first,
                end = match.range.last + 1,
                type = "code",
                content = match.groupValues[1]
            ))
        }
    }
    
    // Find headings FIRST (before emoji and other formatting)
    headingPattern.findAll(textWithStandardEmoji).forEach { match ->
        val overlaps = segments.any { seg ->
            match.range.first < seg.end && match.range.last >= seg.start
        }
        if (!overlaps) {
            val level = match.groupValues[1].length
            segments.add(TextSegment(
                start = match.range.first,
                end = match.range.last + 1,
                type = "heading",
                content = match.groupValues[2],  // Keep emoji placeholders in content
                extra = level.toString()
            ))
        }
    }
    
    // Find bold BEFORE emoji (so **[[EMOJI]] text** works)
    boldPattern.findAll(textWithStandardEmoji).forEach { match ->
        val overlaps = segments.any { seg ->
            match.range.first < seg.end && match.range.last >= seg.start
        }
        if (!overlaps) {
            segments.add(TextSegment(
                start = match.range.first,
                end = match.range.last + 1,
                type = "bold",
                content = match.groupValues[1]  // Keep emoji placeholders in content
            ))
        }
    }
    
    italicPattern.findAll(textWithStandardEmoji).forEach { match ->
        val overlaps = segments.any { seg ->
            match.range.first < seg.end && match.range.last >= seg.start
        }
        if (!overlaps) {
            segments.add(TextSegment(
                start = match.range.first,
                end = match.range.last + 1,
                type = "italic",
                content = match.groupValues[1]
            ))
        }
    }
    
    strikethroughPattern.findAll(textWithStandardEmoji).forEach { match ->
        val overlaps = segments.any { seg ->
            match.range.first < seg.end && match.range.last >= seg.start
        }
        if (!overlaps) {
            segments.add(TextSegment(
                start = match.range.first,
                end = match.range.last + 1,
                type = "strikethrough",
                content = match.groupValues[1]
            ))
        }
    }
    
    // Find emoji LAST - if emoji is inside formatted text, it will be handled during rendering
    emojiImagePattern.findAll(textWithStandardEmoji).forEachIndexed { index, match ->
        val overlaps = segments.any { seg ->
            match.range.first < seg.end && match.range.last >= seg.start
        }
        if (!overlaps) {
            segments.add(TextSegment(
                start = match.range.first,
                end = match.range.last + 1,
                type = "emoji_image",
                content = "emoji_img_$index",
                extra = match.groupValues[1]
            ))
        }
    }
    
    val hasLinks = segments.any { it.type == "link" }
    
    // Sort by position
    segments.sortBy { it.start }
    
    // Build annotated string
    val finalAnnotatedString = buildAnnotatedString {
        
        // Helper function to append content with emoji placeholders AND links
        fun appendContentWithEmojiAndLinks(content: String, emojiPattern: Regex) {
            // Combined pattern: match either emoji or link
            val combinedPattern = Regex("""(\[\[EMOJI:([^|]+)\|([^\]]*)\]\])|(\[([^\]]+)\]\(([^)]+)\))""")
            var lastIndex = 0
            
            combinedPattern.findAll(content).forEach { match ->
                // Append text before this match
                if (match.range.first > lastIndex) {
                    append(content.substring(lastIndex, match.range.first))
                }
                
                if (match.groupValues[1].isNotEmpty()) {
                    // It's an emoji: [[EMOJI:url|alt]]
                    val emojiUrl = match.groupValues[2]
                    val fullTextMatches = emojiPattern.findAll(textWithStandardEmoji).toList()
                    val emojiIndex = fullTextMatches.indexOfFirst { it.groupValues[1] == emojiUrl }
                    if (emojiIndex >= 0) {
                        appendInlineContent("emoji_img_$emojiIndex", "[emoji]")
                    } else {
                        append(match.groupValues[3].ifEmpty { "🙂" })
                    }
                } else if (match.groupValues[4].isNotEmpty()) {
                    // It's a link: [text](url)
                    val linkText = match.groupValues[5]
                    val linkUrl = match.groupValues[6]
                    pushStringAnnotation(tag = "URL", annotation = linkUrl)
                    withStyle(style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                        append(linkText)
                    }
                    pop()
                }
                
                lastIndex = match.range.last + 1
            }
            // Append remaining text
            if (lastIndex < content.length) {
                append(content.substring(lastIndex))
            }
        }
        
        var currentIndex = 0
        
        segments.forEach { segment ->
            // Add text before this segment
            if (segment.start > currentIndex) {
                append(textWithStandardEmoji.substring(currentIndex, segment.start))
            }
            
            when (segment.type) {
                "link" -> {
                    pushStringAnnotation(tag = "URL", annotation = segment.extra)
                    withStyle(style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                        append(segment.content)
                    }
                    pop()
                }
                "code" -> {
                    withStyle(style = SpanStyle(
                        background = codeBackgroundColor,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )) {
                        append(" ${segment.content} ")
                    }
                }
                "emoji_image" -> {
                    appendInlineContent(segment.content, "[emoji]")
                }
                "bold" -> {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        // Parse emoji and links in content
                        appendContentWithEmojiAndLinks(segment.content, emojiImagePattern)
                    }
                }
                "italic" -> {
                    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                        appendContentWithEmojiAndLinks(segment.content, emojiImagePattern)
                    }
                }
                "strikethrough" -> {
                    withStyle(style = SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        appendContentWithEmojiAndLinks(segment.content, emojiImagePattern)
                    }
                }
                "heading" -> {
                    val level = segment.extra.toIntOrNull() ?: 1
                    val fontSize = when (level) {
                        1 -> 24.sp
                        2 -> 22.sp
                        3 -> 20.sp
                        4 -> 18.sp
                        5 -> 16.sp
                        else -> 14.sp
                    }
                    withStyle(style = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize
                    )) {
                        appendContentWithEmojiAndLinks(segment.content, emojiImagePattern)
                    }
                    append("\n")
                }
            }
            
            currentIndex = segment.end
        }
        
        // Add remaining text
        if (currentIndex < textWithStandardEmoji.length) {
            val remaining = textWithStandardEmoji.substring(currentIndex)
            // Handle horizontal rules in remaining text
            val processedRemaining = remaining.replace("---", "───────────")
            append(processedRemaining)
        }

        // Apply paragraph hanging indent to list items
        val resultingString = this.toAnnotatedString().text
        val listRegex = Regex("""(?m)^[•*-]\s+|^[0-9]+[.)]\s+""")
        listRegex.findAll(resultingString).forEach { match ->
            val startIdx = match.range.first
            val endOfLine = resultingString.indexOf('\n', startIdx)
            val endIdx = if (endOfLine == -1) resultingString.length else endOfLine
            
            addStyle(
                style = ParagraphStyle(
                    textIndent = TextIndent(firstLine = 0.sp, restLine = 12.sp)
                ),
                start = startIdx,
                end = endIdx
            )
        }
    }
    
    // Create inline content for emoji images
    val emojiImageMatches = emojiImagePattern.findAll(textWithStandardEmoji).toList()
    val inlineContent = emojiImageMatches.mapIndexed { index, match ->
        val emojiUrl = match.groupValues[1]
        val emojiAlt = match.groupValues[2]
        
        "emoji_img_$index" to InlineTextContent(
            placeholder = Placeholder(
                width = 20.sp,
                height = 20.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
            )
        ) {
            AsyncImage(
                model = emojiUrl,
                contentDescription = emojiAlt,
                contentScale = ContentScale.Fit
            )
        }
    }.toMap()
    
    return Triple(finalAnnotatedString, inlineContent, hasLinks)
}
