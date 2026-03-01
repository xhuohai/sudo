
import java.util.regex.Pattern

fun main() {
    val html = """
        <p>This is a reply.</p>
        <aside class="quote no-group" data-username="oahek" data-post="1" data-topic="1606708">
        <div class="title">
        <div class="quote-controls"></div>
        <img loading="lazy" alt="" width="24" height="24" src="https://linux.do/user_avatar/linux.do/oahek/48/12345.png" class="avatar"> oahek:</div>
        <blockquote>
        <p>This is the quoted text.</p>
        </blockquote>
        </aside>
        <p>Reply content.</p>
    """.trimIndent()

    val quotePattern = Pattern.compile("""<aside[^>]*class="[^"]*quote[^"]*"[^>]*data-username="([^"]*)"[^>]*>([\s\S]*?)</aside>""")
    val matcher = quotePattern.matcher(html)

    if (matcher.find()) {
        println("Match found!")
        println("Username: ${matcher.group(1)}")
        println("Content: ${matcher.group(2)}")
    } else {
        println("No match found.")
    }
    
    // Test case 2: Simple quote
     val html2 = """
        <aside class="quote" data-username="user2">
        <blockquote>Simple quote</blockquote>
        </aside>
    """.trimIndent()
    val matcher2 = quotePattern.matcher(html2)
    if (matcher2.find()) {
         println("Match 2 found!")
         println("Username: ${matcher2.group(1)}")
    } else {
         println("Match 2 NOT found.")
    }
}
