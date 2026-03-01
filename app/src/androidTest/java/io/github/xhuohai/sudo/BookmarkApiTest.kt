package io.github.xhuohai.sudo

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.xhuohai.sudo.data.remote.DiscourseApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient

@RunWith(AndroidJUnit4::class)
class BookmarkApiTest {

    @Test
    fun fetchTopicAndCheckBookmarks() = runBlocking {
        // Need to get cookie from DataStore
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val authRepo = io.github.xhuohai.sudo.data.repository.AuthRepository(context)
        val cookie = authRepo.cookie.first()
        
        println("TEST-COOKIE: " + (cookie?.take(20) ?: "null"))
        
        if (cookie == null) {
            println("TEST-ERROR: No cookie found, user must be logged in on the device")
            return@runBlocking
        }
        
        // We can just use OkHttp directly to dump the raw JSON
        val client = OkHttpClient()
        
        // Let's get the latest topic from Activity marks
        val request = okhttp3.Request.Builder()
            .url("https://linux.do/u/null/activity/bookmarks.json") // We need username
            .header("Cookie", cookie)
            .header("X-Requested-With", "XMLHttpRequest")
            .build()
            
        // Wait, better yet, we just grab a topic that we know is bookmarked
        val username = authRepo.username.first() ?: return@runBlocking
        val bmReq = okhttp3.Request.Builder()
            .url("https://linux.do/u/$username/activity/bookmarks.json")
            .header("Cookie", cookie)
            .header("X-Requested-With", "XMLHttpRequest")
            .build()
            
        val bmRes = client.newCall(bmReq).execute()
        val bmJson = bmRes.body?.string()
        println("TEST-BOOKMARKS-JSON: $bmJson")
        
        // If there's a topic, fetch its details
        // We'll just print it and examine in logcat
    }
}
