package io.github.xhuohai.sudo.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.WrapText
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import io.github.xhuohai.sudo.utils.EmojiUtils
import java.util.regex.Pattern

/**
 * Data class representing a content block (text, image, code, emoji, divider, or table)
 */
sealed class ContentBlock {
    data class TextBlock(val text: String) : ContentBlock()
    data class ImageBlock(
        val url: String,
        val isSmall: Boolean = false,  // For favicons, small icons
        val isFavicon: Boolean = false
    ) : ContentBlock()
    data class CodeBlock(
        val code: String,
        val language: String = ""
    ) : ContentBlock()
    data class QuoteBlock(
        val content: String,
        val username: String,
        val rawHtml: String = ""
    ) : ContentBlock()
    data class EmojiBlock(
        val url: String,
        val alt: String
    ) : ContentBlock()
    data object DividerBlock : ContentBlock()
    data class TableBlock(
        val headers: List<String>,
        val rows: List<List<String>>
    ) : ContentBlock()
    data class OneboxBlock(
        val url: String,
        val title: String,
        val description: String,
        val imageUrl: String,
        val siteIcon: String,
        val siteName: String
    ) : ContentBlock()
    data class AlertBlock(
        val type: String,
        val text: String,
        val html: String
    ) : ContentBlock()
}

/**
 * Parse HTML content into blocks of text, images, code, and emoji, preserving order
 */
fun parseHtmlToBlocks(html: String): List<ContentBlock> {
    val blocks = mutableListOf<ContentBlock>()
    var text = html
    
    // Reverse Discourse optimization for large image quotes so they are parsed naturally as ImageBlocks
    // Match [image], [图片], or any bracket-enclosed placeholder text inside <a> tags
    text = text.replace(Regex("""<a[^>]*href="([^"]+)"[^>]*>\[(?:image|图片|圖片|imagen|imagem|Bild)\]</a>""")) { match ->
        """<img src="${match.groupValues[1]}" />"""
    }
    // Also catch generic pattern: <a href="...">[ any short text ]</a> where href looks like an image URL
    text = text.replace(Regex("""<a[^>]*href="([^"]*(?:\.(?:jpg|jpeg|png|gif|webp|svg|bmp))[^"]*)"[^>]*>\[[^\]]{1,10}\]</a>""", RegexOption.IGNORE_CASE)) { match ->
        """<img src="${match.groupValues[1]}" />"""
    }
    
    // Convert Obsidian-style callouts: <blockquote><p>[!type]<br>content</p></blockquote>
    // Discourse on linux.do does NOT convert these to <div class="markdown-alert">.
    // They remain as raw text like: <blockquote>\n<p>[!question]<br>\ncontent</p>\n</blockquote>
    text = text.replace(Regex("""<blockquote>\s*<p>\[!(\w+)\](?:\s*<br\s*/?>)?\s*([\s\S]*?)</p>\s*</blockquote>""", RegexOption.IGNORE_CASE)) { match ->
        val alertType = match.groupValues[1].lowercase()
        val content = match.groupValues[2]
        """<div class="markdown-alert markdown-alert-$alertType"><p class="markdown-alert-title">$alertType</p><p>$content</p></div>"""
    }
    
    // Extract Onebox blocks first
    val oneboxes = mutableListOf<ContentBlock.OneboxBlock>()
    val oneboxPattern = Pattern.compile("""<aside[^>]*class="[^"]*onebox[^"]*"[^>]*>([\s\S]*?)</aside>""")
    val oneboxMatcher = oneboxPattern.matcher(text)
    val oneboxReplacements = mutableListOf<Triple<Int, Int, String>>()
    var oneboxIndex = 0
    
    while (oneboxMatcher.find()) {
        val fullTag = oneboxMatcher.group(0) ?: ""
        val content = oneboxMatcher.group(1) ?: ""
        
        // Extract URL
        var url = ""
        val urlMatcher = Pattern.compile("""data-onebox-src="([^"]+)"""").matcher(fullTag)
        if (urlMatcher.find()) {
            url = urlMatcher.group(1) ?: ""
        }
        if (url.isEmpty()) {
            val linkMatcher = Pattern.compile("""<header[^>]*>[\s\S]*?<a[^>]*href="([^"]+)"""").matcher(content)
            if (linkMatcher.find()) url = linkMatcher.group(1) ?: ""
        }
        
        // Extract Image (High Res favored)
        var imageUrl = ""
        val imgTagMatcher = Pattern.compile("""<img[^>]*class="[^"]*thumbnail[^"]*"[^>]*>""").matcher(content)
        if (imgTagMatcher.find()) {
            val imgTag = imgTagMatcher.group(0) ?: ""
            // Try data-orig-src first
            val origSrcMatcher = Pattern.compile("""data-orig-src=["']([^"']+)["']""").matcher(imgTag)
            if (origSrcMatcher.find()) {
                imageUrl = origSrcMatcher.group(1) ?: ""
            }
            // Fallback to src
            if (imageUrl.isEmpty()) {
                val srcMatcher = Pattern.compile("""src=["']([^"']+)["']""").matcher(imgTag)
                if (srcMatcher.find()) {
                    imageUrl = srcMatcher.group(1) ?: ""
                }
            }
        }
        
        // Extract Title
        var title = ""
        val titleMatcher = Pattern.compile("""<h3><a[^>]*>([\s\S]*?)</a></h3>""").matcher(content)
        if (titleMatcher.find()) {
            title = titleMatcher.group(1) ?: ""
        }
        
        // Extract Description
        var description = ""
        val descMatcher = Pattern.compile("""<p>([\s\S]*?)</p>""").matcher(content)
        if (descMatcher.find()) {
            description = descMatcher.group(1) ?: ""
        }
        
        // Extract Site Icon
        var siteIcon = ""
        val iconMatcher = Pattern.compile("""<img[^>]*class="[^"]*site-icon[^"]*"[^>]*src="([^"]+)"""").matcher(content)
        if (iconMatcher.find()) {
            siteIcon = iconMatcher.group(1) ?: ""
        }
        
        // Extract Site Name
        var siteName = ""
        val nameMatcher = Pattern.compile("""<header[^>]*>[\s\S]*?<a[^>]*>([\s\S]*?)</a>""").matcher(content)
        if (nameMatcher.find()) {
            siteName = nameMatcher.group(1) ?: ""
        }
        
        // Normalize URLs
        if (url.isNotEmpty()) {
            if (url.startsWith("//")) url = "https:$url"
            else if (!url.startsWith("http")) url = "https://linux.do$url"
        }
        if (imageUrl.isNotEmpty()) {
            if (imageUrl.startsWith("//")) imageUrl = "https:$imageUrl"
            else if (!imageUrl.startsWith("http")) imageUrl = "https://linux.do$imageUrl"
        }
        if (siteIcon.isNotEmpty()) {
            if (siteIcon.startsWith("//")) siteIcon = "https:$siteIcon"
            else if (!siteIcon.startsWith("http")) siteIcon = "https://linux.do$siteIcon"
        }
        
        // Clean text
        title = cleanHtmlContentForBlock(title)
        description = cleanHtmlContentForBlock(description)
        siteName = cleanHtmlContentForBlock(siteName)

        oneboxes.add(ContentBlock.OneboxBlock(url, title, description, imageUrl, siteIcon, siteName))
        oneboxReplacements.add(Triple(oneboxMatcher.start(), oneboxMatcher.end(), "%%ONEBOX_${oneboxIndex}%%"))
        oneboxIndex++
    }
    
    // Apply Onebox replacements in reverse order
    for ((start, end, placeholder) in oneboxReplacements.reversed()) {
        text = text.substring(0, start) + placeholder + text.substring(end)
    }

    // Extract Alert blocks
    val alerts = mutableListOf<ContentBlock.AlertBlock>()
    val alertRegex = Regex("""<div[^>]*class="[^"]*markdown-alert[^"]*markdown-alert-([^" ]+)[^"]*"[^>]*>|<div[^>]*data-wrap="([^"]+)"[^>]*>""", RegexOption.IGNORE_CASE)
    val alertBlocks = extractBalancedBlocks(text, "div", alertRegex)
    val alertReplacements = mutableListOf<Triple<Int, Int, String>>()
    var alertIndex = 0
    
    for (block in alertBlocks) {
        val type1 = try { block.matchResult.groups[1]?.value } catch (e: Exception) { null }
        val type2 = try { block.matchResult.groups[2]?.value } catch (e: Exception) { null }
        
        val type = type1 ?: type2 ?: "note"
        val content = block.innerHtml
        
        // Inside markdown-alerts, there is often a title <p class="markdown-alert-title">... Note</p>
        // We can strip it to avoid duplication if we provide a standard UI
        val cleanContentRaw = content.replace(Regex("""<p[^>]*class="[^"]*markdown-alert-title[^"]*"[^>]*>[\s\S]*?</p>"""), "")
        val textContent = cleanTextBlock(cleanContentRaw.trim())
        
        alerts.add(ContentBlock.AlertBlock(type.lowercase(), textContent, cleanContentRaw))
        alertReplacements.add(Triple(block.matchStart, block.matchEnd, "%%ALERT_${alertIndex}%%"))
        alertIndex++
    }
    
    // Apply Alert replacements in reverse order
    for ((start, end, placeholder) in alertReplacements.reversed()) {
        text = text.substring(0, start) + placeholder + text.substring(end)
    }

    // Parse aside quote blocks and replace with placeholder
    val quoteRegex = Regex("""<aside([^>]+)>""", RegexOption.IGNORE_CASE)
    val quoteBlocks = extractBalancedBlocks(text, "aside", quoteRegex)
    val quoteReplacements = mutableListOf<Triple<Int, Int, String>>()
    var quoteIndex = 0
    val quotes = mutableListOf<ContentBlock.QuoteBlock>()
    
    for (block in quoteBlocks) {
        val attributes = try { block.matchResult.groups[1]?.value ?: "" } catch (e: Exception) { "" }
        val content = block.innerHtml
        
        // Check if it has class="...quote..."
        if (attributes.contains("quote")) {
            // Extract username if present
            val usernameMatcher = Pattern.compile("""data-username="([^"]+)"""").matcher(attributes)
            var username = if (usernameMatcher.find()) usernameMatcher.group(1) ?: "" else ""
            
            // Fallback: try to find username in title if not in data-username
            // <div class="title">... oahek:</div>
            // Relaxed regex to capture usernames with dots, hyphens etc. (anything until colon)
            if (username.isEmpty() && content.contains("""class="title"""")) {
                 val titleMatcher = Pattern.compile("""<div class="title">[\s\S]*?([^:\s]+):</div>""").matcher(content)
                 if (titleMatcher.find()) {
                     username = titleMatcher.group(1) ?: ""
                 }
            }
            
            // Clean up title/avatar from content if captured
            // Default structure: <div class="title">...</div><blockquote>...</blockquote>
            val cleanContent = if (content.contains("<blockquote>")) {
                val m = Pattern.compile("""<blockquote>([\s\S]*?)</blockquote>""").matcher(content)
                if (m.find()) m.group(1) ?: "" else content
            } else {
                // If no blockquote tag, maybe remove the title div manually
                content.replace(Regex("""<div class="title">[\s\S]*?</div>"""), "")
            }
            
            // Remove HTML tags from content for simpler display, or keep logic to render RichText?
            // Use cleanTextBlock to convert HTML to Markdown (preserving links, bold, etc.)
            val textContent = cleanTextBlock(cleanContent)
            
            quotes.add(ContentBlock.QuoteBlock(textContent, username, cleanContent))
            quoteReplacements.add(Triple(block.matchStart, block.matchEnd, "%%QUOTE_${quoteIndex}%%"))
            quoteIndex++
        }
    }
    
    // Apply Quote replacements in reverse order
    for ((start, end, placeholder) in quoteReplacements.reversed()) {
        text = text.substring(0, start) + placeholder + text.substring(end)
    }

    // Generic blockquotes (outside of aside)
    // <blockquote ...>content</blockquote>
    val blockquotePattern = Pattern.compile("""<blockquote[^>]*>([\s\S]*?)</blockquote>""")
    val blockquoteMatcher = blockquotePattern.matcher(text)
    val blockquoteReplacements = mutableListOf<Triple<Int, Int, String>>()
    var blockquoteIndex = 0
    // Reuse existing quotes list but with higher index base if needed, or just append
    // To handle indexing safely, we'll continue quoteIndex from where aside quotes left off.
    
    while (blockquoteMatcher.find()) {
        val content = blockquoteMatcher.group(1) ?: ""
        
        // Clean content (convert HTML to Markdown)
        val textContent = cleanTextBlock(content)
        
        // Add as QuoteBlock with empty username
        quotes.add(ContentBlock.QuoteBlock(textContent, "", content))
        blockquoteReplacements.add(Triple(blockquoteMatcher.start(), blockquoteMatcher.end(), "%%QUOTE_${quoteIndex}%%"))
        quoteIndex++
    }

    // Apply blockquote replacements in reverse order
    for ((start, end, placeholder) in blockquoteReplacements.reversed()) {
        text = text.substring(0, start) + placeholder + text.substring(end)
    }
    
    // Remove lightbox meta info logic:
    // Instead of stripping <a> wrapper, we want to extract href from it if it's a lightbox
    // and inject it into the img as data-orig-src if missing.
    // Regex to match: <a href="(url)" class="lightbox" ...><img ...></a>
    
    // First, remove the meta div inside lightbox-wrapper if present
    text = text.replace(Regex("""<div class="meta">[\s\S]*?</div>"""), "")
    
    // Pattern to find lightbox wrapper
    // We capture the attributes of the <a> tag (group 1) and the content (group 2)
    val lightboxPattern = Pattern.compile("""<div class="lightbox-wrapper"><a([^>]+)>([\s\S]*?)</a></div>""")
    val lightboxMatcher = lightboxPattern.matcher(text)
    val sb = StringBuffer()
    
    while (lightboxMatcher.find()) {
        val attributes = lightboxMatcher.group(1) ?: ""
        val imgContent = lightboxMatcher.group(2) ?: ""
        
        // Check if it has class="lightbox" (simple check)
        if (attributes.contains("class=\"lightbox\"") || attributes.contains("class='lightbox'")) {
             // Extract href
             val hrefMatcher = Pattern.compile("""href=["']([^"']+)["']""").matcher(attributes)
             val originalHref = if (hrefMatcher.find()) hrefMatcher.group(1) else ""
             
             if (originalHref.isNotEmpty()) {
                 // Inject data-orig-src if not present
                val newImgContent = if (!imgContent.contains("data-orig-src")) {
                    if (imgContent.contains("<img ")) {
                        imgContent.replace("<img ", "<img data-orig-src=\"$originalHref\" ")
                    } else {
                        imgContent
                    }
                } else {
                    imgContent
                }
                lightboxMatcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(newImgContent))
             } else {
                 lightboxMatcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(imgContent))
             }
        } else {
             // Not a lightbox link, keep content but remove wrapper
             lightboxMatcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(imgContent))
        }
    }
    lightboxMatcher.appendTail(sb)
    text = sb.toString()
    
    // Cleanup any remaining lightbox artifacts if regex didn't match perfectly
    text = text.replace(Regex("""<div class="lightbox-wrapper">"""), "")
    text = text.replace(Regex("""<a[^>]*class="lightbox"[^>]*>"""), "")
    text = text.replace(Regex("""</a></div>"""), "")
    text = text.replace("</a>", "") // Be careful with this, might remove other links?
    // Actually standard cleanup:
    // text = text.replace(Regex("""</a></div>"""), "</a>") was previous.
    // We already handled the lightbox ones.
    // Let's just ensure we don't break other links.
    // The previous logic was safeish.
    // Let's stick to safe replacements for what we missed.
    
    // Extract emoji images and replace with inline format containing URL
    // Format: [[EMOJI:url|alt]] - will be parsed by RichTextContent for inline rendering
    val emojiImgPattern = Pattern.compile("""<img[^>]*class="[^"]*emoji[^"]*"[^>]*src="([^"]+)"[^>]*alt="([^"]*)"[^>]*>|<img[^>]*src="([^"]+)"[^>]*class="[^"]*emoji[^"]*"[^>]*alt="([^"]*)"[^>]*>|<img[^>]*alt="([^"]*)"[^>]*class="[^"]*emoji[^"]*"[^>]*src="([^"]+)"[^>]*>""")
    val emojiMatcher = emojiImgPattern.matcher(text)
    val emojiReplacements = mutableListOf<Triple<Int, Int, String>>()
    
    while (emojiMatcher.find()) {
        val url = emojiMatcher.group(1) ?: emojiMatcher.group(3) ?: emojiMatcher.group(6) ?: continue
        val alt = emojiMatcher.group(2) ?: emojiMatcher.group(4) ?: emojiMatcher.group(5) ?: ""
        val fullUrl = if (url.startsWith("http")) url else "https://linux.do$url"
        // Use inline format that includes URL directly
        emojiReplacements.add(Triple(emojiMatcher.start(), emojiMatcher.end(), "[[EMOJI:$fullUrl|$alt]]"))
    }
    
    // Apply emoji replacements in reverse order
    for ((start, end, placeholder) in emojiReplacements.reversed()) {
        text = text.substring(0, start) + placeholder + text.substring(end)
    }
    
    // Remove avatar images
    text = text.replace(Regex("""<img[^>]*class="avatar[^"]*"[^>]*>"""), "")
    
    // Step 1: Extract code blocks first and replace with placeholders
    val codeBlocks = mutableListOf<Pair<String, String>>() // (code, language)
    val codeBlockPattern = Pattern.compile("""<pre[^>]*>\s*<code(?:\s+class="([^"]*)")?[^>]*>([\s\S]*?)</code>\s*</pre>""")
    val codeMatcher = codeBlockPattern.matcher(text)
    var codeIndex = 0
    val codeReplacements = mutableListOf<Triple<Int, Int, String>>()
    
    while (codeMatcher.find()) {
        val langClass = codeMatcher.group(1) ?: ""
        val codeContent = codeMatcher.group(2) ?: ""
        
        // Extract language from class like "lang-kotlin" or "language-kotlin"
        val language = when {
            langClass.startsWith("lang-") -> langClass.removePrefix("lang-")
            langClass.startsWith("language-") -> langClass.removePrefix("language-")
            langClass.isNotEmpty() -> langClass.split(" ").firstOrNull { it.isNotEmpty() } ?: ""
            else -> ""
        }
        
        codeBlocks.add(Pair(decodeHtmlEntities(codeContent), language))
        codeReplacements.add(Triple(codeMatcher.start(), codeMatcher.end(), "%%CODE_BLOCK_${codeIndex}%%"))
        codeIndex++
    }
    
    // Apply code block replacements in reverse order
    for ((start, end, placeholder) in codeReplacements.reversed()) {
        text = text.substring(0, start) + placeholder + text.substring(end)
    }
    
    // Step 1.5: Extract tables and replace with placeholders
    val tables = mutableListOf<ContentBlock.TableBlock>()
    val tablePattern = Pattern.compile("""<table[^>]*>([\s\S]*?)</table>""")
    val tableMatcher = tablePattern.matcher(text)
    var tableIndex = 0
    val tableReplacements = mutableListOf<Triple<Int, Int, String>>()
    
    
    while (tableMatcher.find()) {
        val tableHtml = tableMatcher.group(1) ?: ""
        
        // Extract headers
        val headers = mutableListOf<String>()
        val thPattern = Pattern.compile("""<th[^>]*>([\s\S]*?)</th>""")
        val thMatcher = thPattern.matcher(tableHtml)
        while (thMatcher.find()) {
            headers.add(cleanHtmlContentForBlock(thMatcher.group(1) ?: ""))
        }
        
        // Extract rows (from tbody or all tr outside thead)
        val rows = mutableListOf<List<String>>()
        val tbodyPattern = Pattern.compile("""<tbody[^>]*>([\s\S]*?)</tbody>""")
        val tbodyMatcher = tbodyPattern.matcher(tableHtml)
        val bodyContent = if (tbodyMatcher.find()) tbodyMatcher.group(1) ?: "" else tableHtml
        
        val trPattern = Pattern.compile("""<tr[^>]*>([\s\S]*?)</tr>""")
        val trMatcher = trPattern.matcher(bodyContent)
        while (trMatcher.find()) {
            val rowHtml = trMatcher.group(1) ?: ""
            // Skip rows with th (header rows)
            if (rowHtml.contains("<th")) continue
            
            val cells = mutableListOf<String>()
            val tdPattern = Pattern.compile("""<td[^>]*>([\s\S]*?)</td>""")
            val tdMatcher = tdPattern.matcher(rowHtml)
            while (tdMatcher.find()) {
                cells.add(cleanHtmlContentForBlock(tdMatcher.group(1) ?: ""))
            }
            if (cells.isNotEmpty()) {
                rows.add(cells)
            }
        }
        
        if (headers.isNotEmpty() || rows.isNotEmpty()) {
            tables.add(ContentBlock.TableBlock(headers, rows))
            tableReplacements.add(Triple(tableMatcher.start(), tableMatcher.end(), "%%TABLE_${tableIndex}%%"))
            tableIndex++
        }
    }
    
    // Apply table replacements in reverse order
    for ((start, end, placeholder) in tableReplacements.reversed()) {
        text = text.substring(0, start) + placeholder + text.substring(end)
    }
    
    // Step 1.6: Replace hr tags with placeholders
    var hrIndex = 0
    val hrPattern = Pattern.compile("""<hr[^>]*/?>""")
    val hrMatcher = hrPattern.matcher(text)
    val hrReplacements = mutableListOf<Triple<Int, Int, String>>()
    while (hrMatcher.find()) {
        hrReplacements.add(Triple(hrMatcher.start(), hrMatcher.end(), "%%HR_${hrIndex}%%"))
        hrIndex++
    }
    for ((start, end, placeholder) in hrReplacements.reversed()) {
        text = text.substring(0, start) + placeholder + text.substring(end)
    }
    
    // Step 2: Now process mixed content (text, images, code placeholders, table placeholders, hr placeholders, onebox placeholders)
    // Note: Emoji [[EMOJI:url|alt]] stay in text and are handled by RichTextContent
    // Step 2: Now process mixed content (text, images, code placeholders, table placeholders, hr placeholders, onebox placeholders)
    // Note: Emoji [[EMOJI:url|alt]] stay in text and are handled by RichTextContent
    // Updated regex to capture full img tag for better attribute parsing
    val combinedPattern = Pattern.compile("""(<img[^>]+>|%%CODE_BLOCK_(\d+)%%|%%TABLE_(\d+)%%|%%HR_(\d+)%%|%%ONEBOX_(\d+)%%|%%QUOTE_(\d+)%%)""")
    val combinedMatcher = combinedPattern.matcher(text)
    
    var lastEnd = 0
    while (combinedMatcher.find()) {
        val match = combinedMatcher.group(0) ?: continue
        
        // Add text before this match
        val textBefore = text.substring(lastEnd, combinedMatcher.start())
        val cleanedText = cleanTextBlock(textBefore)
        if (cleanedText.isNotBlank()) {
            blocks.add(ContentBlock.TextBlock(cleanedText))
        }
        
        if (match.startsWith("%%ALERT_")) {
            val idx = Regex("""%%ALERT_(\d+)%%""").find(match)?.groupValues?.get(1)?.toIntOrNull()
            if (idx != null && idx < alerts.size) {
                blocks.add(alerts[idx])
            }
        } else if (match.startsWith("%%CODE_BLOCK_")) {
            // Code block placeholder
            val idx = combinedMatcher.group(2)?.toIntOrNull()
            if (idx != null && idx < codeBlocks.size) {
                val (code, lang) = codeBlocks[idx]
                blocks.add(ContentBlock.CodeBlock(code.trim(), lang))
            }
        } else if (match.startsWith("%%TABLE_")) {
            // Table placeholder
            val idx = combinedMatcher.group(3)?.toIntOrNull()
            if (idx != null && idx < tables.size) {
                blocks.add(tables[idx])
            }
        } else if (match.startsWith("%%HR_")) {
            // Horizontal rule placeholder
            val idx = combinedMatcher.group(4)?.toIntOrNull()
            blocks.add(ContentBlock.DividerBlock)
        } else if (match.startsWith("%%ONEBOX_")) {
            // Onebox placeholder
            val idx = combinedMatcher.group(5)?.toIntOrNull()
            if (idx != null && idx < oneboxes.size) {
                blocks.add(oneboxes[idx])
            }
        } else if (match.startsWith("%%QUOTE_")) {
            // Quote placeholder
            val idx = combinedMatcher.group(6)?.toIntOrNull()
            if (idx != null && idx < quotes.size) {
                blocks.add(quotes[idx])
            }
        } else {
            // Image - Parse attributes manually to get best quality
            val imgTag = match
            
            // Skip emoji/avatar if they slipped through (though we filtered them earlier)
            if (imgTag.contains("/images/emoji/") || imgTag.contains("class=\"emoji\"") || imgTag.contains("class=\"avatar\"")) {
                lastEnd = combinedMatcher.end()
                continue
            }
            
            // Extract src (handle double or single quotes)
            val srcMatcher = Pattern.compile("""src=["']([^"']+)["']""").matcher(imgTag)
            var src = if (srcMatcher.find()) srcMatcher.group(1) else ""
            
            // Extract data-orig-src (High Res) - handle double or single quotes
            val origSrcMatcher = Pattern.compile("""data-orig-src=["']([^"']+)["']""").matcher(imgTag)
            val origSrc = if (origSrcMatcher.find()) origSrcMatcher.group(1) else ""
            
            // Use original source if available
            var finalUrl = if (origSrc.isNotEmpty()) origSrc else src
            
            if (finalUrl.isEmpty()) {
                // Fallback: try to find loose src=... (no quotes? unlikely but possible in bad HTML)
                val fallbackSrcMatcher = Pattern.compile("""src=([^\s>]+)""").matcher(imgTag)
                if (fallbackSrcMatcher.find()) {
                    src = fallbackSrcMatcher.group(1)
                    finalUrl = src
                } else {
                    lastEnd = combinedMatcher.end()
                    continue
                }
            }

            val isFavicon = finalUrl.contains("favicon") || 
                            finalUrl.contains("/icon") ||
                            finalUrl.contains("site-icon") ||
                            imgTag.contains("class=\"site-icon\"") ||
                            imgTag.contains("class=\"favicon\"") ||
                            imgTag.contains("width=\"16\"") ||
                            imgTag.contains("width=\"20\"") ||
                            imgTag.contains("width=\"24\"") ||
                            imgTag.contains("height=\"16\"") ||
                            imgTag.contains("height=\"20\"") ||
                            imgTag.contains("height=\"24\"") ||
                            finalUrl.matches(Regex(".*[/]favicon[^/]*\\.(?:ico|png|jpg).*", RegexOption.IGNORE_CASE)) ||
                            finalUrl.contains("/apple-touch-icon") ||
                            finalUrl.contains("_16x16") || finalUrl.contains("_20x20") || finalUrl.contains("_24x24") ||
                            finalUrl.contains("/icons/") ||
                            finalUrl.matches(Regex(".*[/_-](16|20|24)x(16|20|24)[/_.-].*"))
            
            val isSmall = isFavicon || 
                          imgTag.contains("width=\"32\"") || 
                          imgTag.contains("height=\"32\"") ||
                          finalUrl.contains("_32x32") ||
                          finalUrl.matches(Regex(".*[/_-]32x32[/_.-].*"))
            
            val fullUrl = if (finalUrl.startsWith("http")) finalUrl else "https://linux.do$finalUrl"
            blocks.add(ContentBlock.ImageBlock(fullUrl, isSmall, isFavicon))
        }
        
        lastEnd = combinedMatcher.end()
    }
    
    // Add remaining text
    val remainingText = text.substring(lastEnd)
    val cleanedRemaining = cleanTextBlock(remainingText)
    if (cleanedRemaining.isNotBlank()) {
        blocks.add(ContentBlock.TextBlock(cleanedRemaining))
    }
    
    return blocks
}

private fun decodeHtmlEntities(text: String): String {
    return text
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")
}

/**
 * Clean a text block: convert HTML to plain text
 */
private fun cleanTextBlock(html: String): String {
    var text = html
    
    // Remove image dimension metadata
    text = text.replace(Regex("""\d+%?x\d+%?\s*\d+x\d+\s*[\d.]+[KMG]?B""", RegexOption.IGNORE_CASE), "")
    text = text.replace(Regex("""\d+x\d+\s*[\d.]+\s*[KMG]B""", RegexOption.IGNORE_CASE), "")
    
    // Handle user mentions
    text = text.replace(Regex("""<a[^>]*class="mention[^"]*"[^>]*>(@[^<]+)</a>""")) { it.groupValues[1] }
    
    // Convert links to markdown format
    text = text.replace(Regex("""<a[^>]*href="([^"]+)"[^>]*>([\s\S]*?)</a>""")) { matchResult ->
        val url = matchResult.groupValues[1]
        val linkText = matchResult.groupValues[2].trim()
        
        // Skip user mentions, empty links, and quote/lightbox image tags
        if (linkText.isBlank() || linkText.contains("@") || url.contains("user-card") || linkText == "[image]" || linkText == "[图片]" || linkText.contains("<img")) {
            linkText
        } else {
            "[$linkText]($url)"
        }
    }
    
    // Clean raw markdown alert syntax [!note], [!question], etc. to a friendly label
    text = text.replace(Regex("""\[!(note|tip|important|warning|caution|question|info|success|danger)\]""", RegexOption.IGNORE_CASE)) { match ->
        "⚠ ${match.groupValues[1].replaceFirstChar { it.uppercase() }}:"
    }
    
    // Helper to process heading content - convert links to markdown, then strip other tags
    fun extractHeadingContent(html: String): String {
        var content = html
        // First convert links to markdown format
        content = content.replace(Regex("""<a[^>]*href="([^"]+)"[^>]*>([^<]*)</a>""")) { match ->
            "[${match.groupValues[2]}](${match.groupValues[1]})"
        }
        // Then strip remaining HTML tags
        content = content.replace(Regex("<[^>]+>"), "")
        return content.trim()
    }
    
    // Handle headings (match any content including nested HTML)
    text = text.replace(Regex("""<h1[^>]*>([\s\S]*?)</h1>""")) { 
        "# ${extractHeadingContent(it.groupValues[1])}\n" 
    }
    text = text.replace(Regex("""<h2[^>]*>([\s\S]*?)</h2>""")) { 
        "## ${extractHeadingContent(it.groupValues[1])}\n" 
    }
    text = text.replace(Regex("""<h3[^>]*>([\s\S]*?)</h3>""")) { 
        "### ${extractHeadingContent(it.groupValues[1])}\n" 
    }
    text = text.replace(Regex("""<h4[^>]*>([\s\S]*?)</h4>""")) { 
        "#### ${extractHeadingContent(it.groupValues[1])}\n" 
    }
    text = text.replace(Regex("""<h5[^>]*>([\s\S]*?)</h5>""")) { 
        "##### ${extractHeadingContent(it.groupValues[1])}\n" 
    }
    text = text.replace(Regex("""<h6[^>]*>([\s\S]*?)</h6>""")) { 
        "###### ${extractHeadingContent(it.groupValues[1])}\n" 
    }
    
    // Handle bold and strong (match any content including nested HTML)
    text = text.replace(Regex("""<strong[^>]*>([\s\S]*?)</strong>""")) { 
        "**${it.groupValues[1].replace(Regex("<[^>]+>"), "")}**" 
    }
    text = text.replace(Regex("""<b[^>]*>([\s\S]*?)</b>""")) { 
        "**${it.groupValues[1].replace(Regex("<[^>]+>"), "")}**" 
    }
    
    // Handle italic and emphasis
    text = text.replace(Regex("""<em[^>]*>([\s\S]*?)</em>""")) { 
        "*${it.groupValues[1].replace(Regex("<[^>]+>"), "")}*" 
    }
    text = text.replace(Regex("""<i[^>]*>([\s\S]*?)</i>""")) { 
        "*${it.groupValues[1].replace(Regex("<[^>]+>"), "")}*" 
    }
    
    // Handle strikethrough
    text = text.replace(Regex("""<s[^>]*>([\s\S]*?)</s>""")) { 
        "~~${it.groupValues[1].replace(Regex("<[^>]+>"), "")}~~" 
    }
    text = text.replace(Regex("""<del[^>]*>([\s\S]*?)</del>""")) { 
        "~~${it.groupValues[1].replace(Regex("<[^>]+>"), "")}~~" 
    }

    
    // Handle unordered list items
    text = text.replace(Regex("""<li[^>]*>"""), "\n• ")
    text = text.replace(Regex("""</li>"""), "")
    text = text.replace(Regex("""<ul[^>]*>"""), "\n")
    text = text.replace(Regex("""</ul>"""), "\n")
    
    // Handle ordered list items - convert to numbered format
    var listCounter = 0
    text = text.replace(Regex("""<ol[^>]*>""")) { 
        listCounter = 0
        "\n"
    }
    text = text.replace(Regex("""</ol>"""), "\n")
    // For ordered list items, we'll just use bullets since tracking numbers is complex
    
    // Note: Tables and horizontal rules are now handled as separate blocks, not text

    
    // Convert br and p tags
    text = text.replace(Regex("""<br\s*/?>"""), "\n")
    text = text.replace(Regex("""</p>"""), "\n")
    text = text.replace(Regex("""<p[^>]*>"""), "")
    
    // Handle blockquotes
    text = text.replace(Regex("""<blockquote[^>]*>"""), "\n> ")
    text = text.replace(Regex("""</blockquote>"""), "\n")
    
    // Handle div tags (often used for formatting)
    text = text.replace(Regex("""<div[^>]*>"""), "")
    text = text.replace(Regex("""</div>"""), "\n")
    
    // Handle spans (usually just remove them)
    text = text.replace(Regex("""<span[^>]*>"""), "")
    text = text.replace(Regex("""</span>"""), "")
    
    // Code blocks
    text = text.replace(Regex("""<pre[^>]*><code[^>]*>"""), "\n```\n")
    text = text.replace(Regex("""</code></pre>"""), "\n```\n")
    text = text.replace(Regex("""<code[^>]*>"""), "`")
    text = text.replace(Regex("""</code>"""), "`")
    
    // Remove all other HTML tags
    text = text.replace(Regex("""<[^>]+>"""), "")
    
    // Decode HTML entities
    text = text.replace("&amp;", "&")
    text = text.replace("&lt;", "<")
    text = text.replace("&gt;", ">")
    text = text.replace("&quot;", "\"")
    text = text.replace("&#39;", "'")
    text = text.replace("&nbsp;", " ")
    
    // Convert emoji shortcodes
    text = EmojiUtils.convertShortcodes(text)
    
    // Clean up whitespace
    text = text.replace(Regex("""\n{3,}"""), "\n\n")
    
    return text.trim()
}

/**
 * Composable that renders HTML content with inline images
 */
@Composable
fun HtmlContent(
    html: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onImageClick: (List<String>, Int) -> Unit = { _, _ -> },
    onLinkClick: ((String) -> Unit)? = null
) {
    val blocks = remember(html) { parseHtmlToBlocks(html) }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Collect all image URLs for viewer (excluding only favicons)
        val allImages = remember(blocks) {
            blocks.filterIsInstance<ContentBlock.ImageBlock>()
                .filter { !it.isFavicon }
                .map { it.url }
        }
        
        blocks.forEach { block ->
            when (block) {
                is ContentBlock.TextBlock -> {
                    if (block.text.isNotBlank()) {
                        RichTextContent(
                            text = block.text,
                            style = textStyle,
                            color = textColor,
                            onLinkClick = onLinkClick
                        )
                    }
                }
                is ContentBlock.ImageBlock -> {
                    val isClickable = !block.isFavicon
                    val index = if (isClickable) allImages.indexOf(block.url) else -1
                    
                    val baseModifier = when {
                        block.isFavicon -> Modifier.size(16.dp)
                        block.isSmall -> Modifier.size(32.dp)
                        else -> Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                    }
                    
                    val imageModifier = if (isClickable && index >= 0) {
                        baseModifier.clickable { onImageClick(allImages, index) }
                    } else {
                        baseModifier
                    }
                    
                    AsyncImage(
                        model = block.url,
                        contentDescription = null,
                        modifier = imageModifier,
                        contentScale = if (block.isFavicon || block.isSmall) ContentScale.Fit else ContentScale.FillWidth
                    )
                }
                is ContentBlock.CodeBlock -> {
                    CodeBlockView(
                        code = block.code,
                        language = block.language
                    )
                }
                is ContentBlock.AlertBlock -> {
                    AlertBlockView(
                        block = block,
                        textStyle = textStyle,
                        textColor = textColor,
                        onImageClick = onImageClick,
                        onLinkClick = onLinkClick
                    )
                }
                is ContentBlock.EmojiBlock -> {
                    AsyncImage(
                        model = block.url,
                        contentDescription = block.alt,
                        modifier = Modifier.size(24.dp)
                    )
                }
                is ContentBlock.DividerBlock -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
                is ContentBlock.TableBlock -> {
                    TableView(
                        headers = block.headers,
                        rows = block.rows,
                        textStyle = textStyle,
                        textColor = textColor,
                        onLinkClick = onLinkClick
                    )
                }
                is ContentBlock.OneboxBlock -> {
                    OneboxView(
                        block = block,
                        onLinkClick = onLinkClick
                    )
                }
                is ContentBlock.QuoteBlock -> {
                    QuoteBlockView(
                        block = block,
                        textStyle = textStyle,
                        textColor = textColor,
                        onImageClick = onImageClick,
                        onLinkClick = onLinkClick
                    )
                }
            }
        }
    }
}

@Composable
internal fun QuoteBlockView(
    block: ContentBlock.QuoteBlock,
    textStyle: TextStyle,
    textColor: Color,
    modifier: Modifier = Modifier,
    onImageClick: (List<String>, Int) -> Unit = { _, _ -> },
    onLinkClick: ((String) -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        // Vertical accent bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .heightIn(min = 20.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                .clickable { isExpanded = !isExpanded }
        )
        
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            // Username header (clickable to toggle)
            if (block.username.isNotEmpty()) {
                Text(
                    text = block.username,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .clickable { isExpanded = !isExpanded }
                )
            }
            
            if (!isExpanded) {
                // Collapsed: simple text preview with height constraint
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 60.dp)
                        .clip(androidx.compose.ui.graphics.RectangleShape)
                ) {
                    RichTextContent(
                        text = block.content,
                        style = textStyle,
                        color = textColor,
                        onLinkClick = onLinkClick,
                        maxLines = 3,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            } else {
                // Expanded: WebView for full-fidelity HTML rendering
                val webViewHtml = remember(block.rawHtml, isDark) {
                    buildQuoteWebViewHtml(block.rawHtml, isDark)
                }
                var webViewHeight by remember { mutableStateOf(100) }
                
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        android.webkit.WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.domStorageEnabled = true
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            
                            webViewClient = object : android.webkit.WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: android.webkit.WebView?,
                                    request: android.webkit.WebResourceRequest?
                                ): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    if (onLinkClick != null) {
                                        onLinkClick(url)
                                    } else {
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                            ctx.startActivity(intent)
                                        } catch (e: Exception) { e.printStackTrace() }
                                    }
                                    return true
                                }
                                
                                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                    // Auto-size WebView to content height
                                    view?.evaluateJavascript("document.body.scrollHeight") { height ->
                                        val h = height?.toIntOrNull() ?: 100
                                        webViewHeight = h
                                    }
                                }
                            }
                            
                            loadDataWithBaseURL("https://linux.do", webViewHtml, "text/html", "UTF-8", null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(webViewHeight.dp)
                )
            }

            // Expand/Collapse toggle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { isExpanded = !isExpanded })
                    .padding(top = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = if (isExpanded) 0.5f else 1f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Build a complete HTML page for WebView rendering of quote content.
 * Includes CSS that matches the app's theme and handles dark mode.
 */
private fun buildQuoteWebViewHtml(rawHtml: String, isDarkTheme: Boolean): String {
    val bgColor = if (isDarkTheme) "#1C1B1F" else "#FAFAFA"
    val textColor = if (isDarkTheme) "#E6E1E5" else "#1C1B1F"
    val linkColor = if (isDarkTheme) "#D0BCFF" else "#6750A4"
    val codeBackground = if (isDarkTheme) "#2B2930" else "#E8E0E5"
    val alertNoteBg = if (isDarkTheme) "#1A2332" else "#E8F4FD"
    val alertNoteBorder = "#0969DA"
    
    return """
    <!DOCTYPE html>
    <html>
    <head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
    <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body {
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        font-size: 14px;
        line-height: 1.6;
        color: $textColor;
        background: transparent;
        padding: 4px 0;
        word-wrap: break-word;
        overflow-wrap: break-word;
    }
    p { margin: 4px 0; }
    a { color: $linkColor; text-decoration: none; }
    img { max-width: 100%; height: auto; border-radius: 6px; margin: 4px 0; }
    code { background: $codeBackground; padding: 2px 4px; border-radius: 3px; font-size: 13px; }
    pre { background: $codeBackground; padding: 8px 12px; border-radius: 6px; overflow-x: auto; margin: 8px 0; }
    pre code { background: transparent; padding: 0; }
    blockquote { border-left: 3px solid $linkColor; padding-left: 12px; margin: 8px 0; opacity: 0.9; }
    ul, ol { padding-left: 20px; margin: 4px 0; }
    li { margin: 2px 0; }
    table { border-collapse: collapse; width: 100%; margin: 8px 0; }
    th, td { border: 1px solid ${if (isDarkTheme) "#444" else "#ddd"}; padding: 6px 10px; text-align: left; }
    th { background: $codeBackground; font-weight: 600; }
    .markdown-alert { border-radius: 4px; padding: 10px 12px; margin: 8px 0; border-left: 4px solid $alertNoteBorder; background: $alertNoteBg; }
    .markdown-alert-title { font-weight: bold; margin-bottom: 4px; }
    .lightbox img, .d-lazyload img { cursor: pointer; }
    details { margin: 4px 0; }
    summary { cursor: pointer; font-weight: 500; }
    .emoji { width: 20px; height: 20px; vertical-align: middle; }
    aside.quote { background: ${if (isDarkTheme) "#2B2930" else "#F0EDF2"}; border-left: 3px solid $linkColor; border-radius: 4px; padding: 8px 12px; margin: 8px 0; }
    aside.quote .title { font-weight: 600; margin-bottom: 4px; font-size: 13px; color: ${if (isDarkTheme) "#CAC4D0" else "#49454F"}; }
    </style>
    </head>
    <body>$rawHtml</body>
    </html>
    """.trimIndent()
}

@Composable
internal fun AlertBlockView(
    block: ContentBlock.AlertBlock,
    textStyle: TextStyle,
    textColor: Color,
    modifier: Modifier = Modifier,
    onImageClick: (List<String>, Int) -> Unit = { _, _ -> },
    onLinkClick: ((String) -> Unit)? = null
) {
    val (bgColor, borderColor, iconColor) = when (block.type) {
        "note", "info" -> Triple(Color(0xFFE8F4FD), Color(0xFF0969DA), Color(0xFF0969DA))
        "tip", "success" -> Triple(Color(0xFFEAF5EA), Color(0xFF1A7F37), Color(0xFF1A7F37))
        "important" -> Triple(Color(0xFFF3E8FB), Color(0xFF8250DF), Color(0xFF8250DF))
        "warning" -> Triple(Color(0xFFFCF4E4), Color(0xFF9A6700), Color(0xFF9A6700))
        "caution", "danger" -> Triple(Color(0xFFFCEAEB), Color(0xFFD1242F), Color(0xFFD1242F))
        "question" -> Triple(Color(0xFFF0EAFB), Color(0xFF6F42C1), Color(0xFF6F42C1))
        else -> Triple(Color(0xFFE8F4FD), Color(0xFF0969DA), Color(0xFF0969DA))
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(4.dp))
            .border(1.dp, borderColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .heightIn(min = 20.dp)
                .background(borderColor, RoundedCornerShape(2.dp))
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                text = block.type.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium,
                color = iconColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            HtmlContent(
                html = block.html,
                textStyle = textStyle,
                textColor = textColor.copy(alpha = 0.9f), // Slightly darker contrast
                onImageClick = onImageClick,
                onLinkClick = onLinkClick
            )
        }
    }
}


/**
 * Table view with proper layout and horizontal scrolling for wide tables
 */
@Composable
internal fun TableView(
    headers: List<String>,
    rows: List<List<String>>,
    textStyle: TextStyle,
    textColor: Color,
    modifier: Modifier = Modifier,
    onLinkClick: ((String) -> Unit)? = null
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val headerBackground = MaterialTheme.colorScheme.surfaceVariant
    val cellWidth = 120.dp
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
    ) {
        // Header row
        if (headers.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .background(headerBackground),
                verticalAlignment = Alignment.CenterVertically
            ) {
                headers.forEach { header ->
                    RichTextContent(
                        text = "**$header**",
                        style = textStyle,
                        color = textColor,
                        modifier = Modifier
                            .width(cellWidth)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        onLinkClick = onLinkClick
                    )
                }
            }
            HorizontalDivider(color = borderColor)
        }
        
        // Data rows
        rows.forEachIndexed { index, row ->
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ensure the row has the same number of cells as headers for perfect alignment
                val maxColumns = if (headers.isNotEmpty()) headers.size else row.size
                for (colIndex in 0 until maxColumns) {
                    val cellText = if (colIndex < row.size) row[colIndex] else ""
                    RichTextContent(
                        text = cellText,
                        style = textStyle,
                        color = textColor,
                        modifier = Modifier
                            .width(cellWidth)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        onLinkClick = onLinkClick
                    )
                }
            }
            if (index < rows.size - 1) {
                HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
            }
        }
    }
}

/**
 * Styled code block with monospace font, background, line numbers, and wrap toggle
 */
@Composable
internal fun CodeBlockView(
    code: String,
    language: String,
    modifier: Modifier = Modifier
) {
    var wrapLines by remember { mutableStateOf(false) }
    var showLineNumbers by remember { mutableStateOf(true) }
    
    val lines = remember(code) { code.lines() }
    val lineNumberWidth = remember(lines) { 
        "${lines.size}".length * 10 + 8 // Approximate width based on line count
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // Header with language label and controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Language label
            Text(
                text = language.ifBlank { "code" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Line numbers toggle
                IconButton(
                    onClick = { showLineNumbers = !showLineNumbers },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatListNumbered,
                        contentDescription = if (showLineNumbers) "隐藏行号" else "显示行号",
                        tint = if (showLineNumbers) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                // Wrap toggle
                IconButton(
                    onClick = { wrapLines = !wrapLines },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WrapText,
                        contentDescription = if (wrapLines) "不换行" else "换行",
                        tint = if (wrapLines) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        // Code content
        val scrollState = rememberScrollState()
        val codeModifier = if (wrapLines) {
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        } else {
            Modifier
                .horizontalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        }
        
        Row(modifier = codeModifier) {
            // Line numbers column
            if (showLineNumbers) {
                Column(
                    modifier = Modifier.padding(end = 12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    lines.forEachIndexed { index, _ ->
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            
            // Code column
            Column {
                lines.forEach { line ->
                    Text(
                        text = line.ifEmpty { " " },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        softWrap = wrapLines,
                        maxLines = if (wrapLines) Int.MAX_VALUE else 1
                    )
                }
            }
        }
    }
}

@Composable
internal fun OneboxView(
    block: ContentBlock.OneboxBlock,
    onLinkClick: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable {
                if (onLinkClick != null) {
                    onLinkClick(block.url)
                } else {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(block.url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Icon + Site Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                if (block.siteIcon.isNotEmpty()) {
                    AsyncImage(
                        model = block.siteIcon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        contentScale = ContentScale.Fit
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                }
                
                if (block.siteName.isNotEmpty()) {
                    Text(
                        text = block.siteName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Content
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (block.title.isNotEmpty()) {
                        Text(
                            text = block.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    
                    if (block.description.isNotEmpty()) {
                        Text(
                            text = block.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                
                if (block.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = block.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

/**
 * Clean HTML content for block text: remove tags, decode entities, process emoji
 */
private fun cleanHtmlContentForBlock(html: String): String {
    var content = html
    // Convert emoji images to [[EMOJI:url|alt]] format
    val emojiPattern = Pattern.compile("""<img[^>]*class="[^"]*emoji[^"]*"[^>]*src="([^"]+)"[^>]*alt="([^"]*)"[^>]*>|<img[^>]*src="([^"]+)"[^>]*class="[^"]*emoji[^"]*"[^>]*alt="([^"]*)"[^>]*>""")
    val emojiMatcher = emojiPattern.matcher(content)
    val sb = StringBuffer()
    while (emojiMatcher.find()) {
        val url = emojiMatcher.group(1) ?: emojiMatcher.group(3) ?: ""
        val alt = emojiMatcher.group(2) ?: emojiMatcher.group(4) ?: ""
        val fullUrl = if (url.startsWith("http")) url else "https://linux.do$url"
        emojiMatcher.appendReplacement(sb, "[[EMOJI:$fullUrl|$alt]]")
    }
    emojiMatcher.appendTail(sb)
    content = sb.toString()
    // Remove remaining HTML tags
    content = content.replace(Regex("<[^>]+>"), "")
    // Decode HTML entities
    content = content.replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")
    return content.trim()
}

data class HtmlBlockInfo(val matchStart: Int, val matchEnd: Int, val outerHtml: String, val innerHtml: String, val matchResult: MatchResult)

/**
 * Extracts balanced HTML tags accurately without truncating on nested duplicates.
 */
fun extractBalancedBlocks(html: String, tagName: String, openTagRegex: Regex): List<HtmlBlockInfo> {
    val results = mutableListOf<HtmlBlockInfo>()
    var searchStart = 0
    
    while (true) {
        val match = openTagRegex.find(html, searchStart) ?: break
        val startIdx = match.range.first
        val openTagEnd = match.range.last + 1
        
        var openCount = 1
        var currIdx = openTagEnd
        
        val tagOpenRegex = Regex("<$tagName\\b", RegexOption.IGNORE_CASE)
        val tagCloseRegex = Regex("</$tagName>", RegexOption.IGNORE_CASE)
        
        while (openCount > 0 && currIdx < html.length) {
            val nextOpenMatch = tagOpenRegex.find(html, currIdx)
            val nextCloseMatch = tagCloseRegex.find(html, currIdx)
            
            val openIdx = nextOpenMatch?.range?.first ?: Int.MAX_VALUE
            val closeIdx = nextCloseMatch?.range?.first ?: Int.MAX_VALUE
            
            if (closeIdx == Int.MAX_VALUE) {
                currIdx = html.length
                break
            }
            
            if (openIdx < closeIdx) {
                openCount++
                currIdx = nextOpenMatch!!.range.last + 1
            } else {
                openCount--
                currIdx = nextCloseMatch!!.range.last + 1
            }
        }
        
        val endIdx = currIdx
        val outerHtml = html.substring(startIdx, endIdx)
        val closingTagLen = "</$tagName>".length
        val innerHtml = if (endIdx >= closingTagLen && html.substring(endIdx - closingTagLen, endIdx).equals("</$tagName>", ignoreCase = true)) {
            html.substring(openTagEnd, endIdx - closingTagLen)
        } else {
            html.substring(openTagEnd, endIdx)
        }
        
        results.add(HtmlBlockInfo(startIdx, endIdx, outerHtml, innerHtml, match))
        searchStart = endIdx
    }
    
    return results
}
