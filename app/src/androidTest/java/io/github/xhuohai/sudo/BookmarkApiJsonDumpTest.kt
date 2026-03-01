package io.github.xhuohai.sudo

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import okhttp3.OkHttpClient

@RunWith(AndroidJUnit4::class)
class BookmarkApiJsonDumpTest {

    @Test
    fun fetchTopicAndDumpJson() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val authRepo = io.github.xhuohai.sudo.data.repository.AuthRepository(context)
        val cookie = authRepo.cookie.first()
        val username = authRepo.username.first()
        
        if (cookie == null || username == null) {
            println("TEST-JSON-DUMP: No cookie or username")
            return@runBlocking
        }
        
        val client = OkHttpClient()
        
        // Let's fetch the user's bookmarks list first to get a known bookmarked topic ID
        val bmReq = okhttp3.Request.Builder()
            .url("https://linux.do/u/$username/activity/bookmarks.json")
            .header("Cookie", cookie)
            .header("X-Requested-With", "XMLHttpRequest")
            .build()
            
        val bmRes = client.newCall(bmReq).execute()
        val bmJson = bmRes.body?.string() ?: ""
        
        // Very basic regex to find the first topic_id
        val topicIdMatch = "\"topic_id\":\\s*(\\d+)".toRegex().find(bmJson)
        if (topicIdMatch == null) {
            println("TEST-JSON-DUMP: No bookmarked topics found in list.")
            return@runBlocking
        }
        
        val topicId = topicIdMatch.groupValues[1]
        println("TEST-JSON-DUMP: Found bookmarked topic ID $topicId")
        
        // Fetch topic detail for that topic
        val topicReq = okhttp3.Request.Builder()
            .url("https://linux.do/t/$topicId.json?print=true")
            .header("Cookie", cookie)
            .header("X-Requested-With", "XMLHttpRequest")
            .build()
            
        val topicRes = client.newCall(topicReq).execute()
        val topicStr = topicRes.body?.string() ?: ""
        
        // Dump chunks so logcat doesn't truncate it
        println("TEST-JSON-DUMP: >> START TOPIC DETAIL")
        topicStr.chunked(3000).forEach { chunk ->
            println("TEST-JSON-DUMP: $chunk")
        }
        println("TEST-JSON-DUMP: >> END TOPIC DETAIL")
    }
}
