
import java.util.regex.Pattern

fun main() {
    val html = """
<div class="lightbox-wrapper"><a class="lightbox" href="https://linux.do/uploads/default/original/4X/3/d/8/3d8c08640fb808cf994cf3dba8e5d818e707fc82.jpeg" data-download-href="https://linux.do/uploads/default/3d8c08640fb808cf994cf3dba8e5d818e707fc82" title="IMG_5004.PNG"><img src="https://linux.do/uploads/default/optimized/4X/3/d/8/3d8c08640fb808cf994cf3dba8e5d818e707fc82_2_171x375.jpeg" alt="IMG_5004.PNG" data-base62-sha1="8Mt8XBsLk5BX1LNa9zCLDYEEGVc" width="171" height="375" srcset="https://linux.do/uploads/default/optimized/4X/3/d/8/3d8c08640fb808cf994cf3dba8e5d818e707fc82_2_171x375.jpeg, https://linux.do/uploads/default/optimized/4X/3/d/8/3d8c08640fb808cf994cf3dba8e5d818e707fc82_2_256x562.jpeg 1.5x, https://linux.do/uploads/default/optimized/4X/3/d/8/3d8c08640fb808cf994cf3dba8e5d818e707fc82_2_342x750.jpeg 2x" data-dominant-color="B6B6D8"><div class="meta"><svg class="fa d-icon d-icon-far-image svg-icon" aria-hidden="true"><use href="#far-image"></use></svg><span class="filename">IMG_5004.PNG</span><span class="informations">1206×2622 431 KB</span><svg class="fa d-icon d-icon-discourse-expand svg-icon" aria-hidden="true"><use href="#discourse-expand"></use></svg></div></a></div>
    """

    // Cleaning step 1: Remove meta div
    var text = html.replace(Regex("""<div class="meta">[\s\S]*?</div>"""), "")
    println("After cleaning meta: $text")

    // Current Regex tested in HtmlContent.kt
    // <div class="lightbox-wrapper"><a[^>]*href="([^"]+)"[^>]*class="[^"]*lightbox[^"]*"[^>]*>([\s\S]*?)</a></div>
    // NOTE: In the sample HTML, class comes BEFORE href!
    // Sample: <a class="lightbox" href="...">
    // Regex: <a[^>]*href="([^"]+)"[^>]*class="[^"]*lightbox[^"]*"[^>]*>
    // This expects href BEFORE class IF class is matched later.
    // Actually [^>]* matches everything including other attributes.
    // IF href comes first, it works.
    // IF class comes first, `href="..."` part won't match first group?
    // Wait, [^>]* matches "class=\"lightbox\" ".
    // Then href="...".
    // Then [^>]*class="[^"]*lightbox[^"]*". This expects ANOTHER class attribute?
    // YES! The regex requires href to appear, AND THEN class="lightbox" to appear AFTER it.
    // But in the sample, class="lightbox" is FIRST.
    // So the regex FAILS.

    val lightboxPattern = Pattern.compile("""<div class="lightbox-wrapper"><a[^>]*href="([^"]+)"[^>]*class="[^"]*lightbox[^"]*"[^>]*>([\s\S]*?)</a></div>""")
    val matcher = lightboxPattern.matcher(text)
    
    if (matcher.find()) {
        println("MATCH FOUND!")
        println("Href: " + matcher.group(1))
    } else {
        println("NO MATCH!")
    }

    // Proposed Better Regex:
    // Just match the <a> tag and extract href, agnostic of order.
    // <div class="lightbox-wrapper"><a([^>]+)>([\s\S]*?)</a></div>
    // Then inside group 1, find href.
}
