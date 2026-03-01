package io.github.xhuohai.sudo.data.remote

import io.github.xhuohai.sudo.data.model.CategoriesResponse
import io.github.xhuohai.sudo.data.model.PostsResponse
import io.github.xhuohai.sudo.data.model.SearchResponse
import io.github.xhuohai.sudo.data.model.TopicDetailResponse
import io.github.xhuohai.sudo.data.model.TopicListResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface DiscourseApi {

    // Topics
    @GET("latest.json")
    suspend fun getLatestTopics(
        @Header("Cookie") cookie: String? = null,
        @Query("page") page: Int = 0,
        @Query("per_page") perPage: Int = 30
    ): TopicListResponse

    @GET("top.json")
    suspend fun getTopTopics(
        @Header("Cookie") cookie: String? = null,
        @Query("period") period: String = "daily", // all, yearly, quarterly, monthly, weekly, daily
        @Query("page") page: Int = 0
    ): TopicListResponse

    @GET("latest.json")
    suspend fun getNewTopics(
        @Header("Cookie") cookie: String? = null,
        @Query("page") page: Int = 0,
        @Query("order") order: String = "created"
    ): TopicListResponse

    @GET("unread.json")
    suspend fun getUnreadTopics(
        @Header("Cookie") cookie: String? = null,
        @Query("page") page: Int = 0
    ): TopicListResponse

    // Topic Detail
    @GET("t/{slug}/{id}.json")
    suspend fun getTopicDetail(
        @Path("slug") slug: String,
        @Path("id") id: Int,
        @Header("Cookie") cookie: String? = null,
        @Header("X-Requested-With") requestedWith: String = "XMLHttpRequest",
        @Query("page") page: Int = 1,
        @Query("_") timestamp: Long = System.currentTimeMillis()
    ): TopicDetailResponse
    
    // Fetch specific posts by IDs
    @GET("t/{id}/posts.json")
    suspend fun getPostsByIds(
        @Path("id") topicId: Int,
        @Query("post_ids[]") postIds: List<Int>,
        @Header("Cookie") cookie: String? = null
    ): PostsResponse

    // Categories
    @GET("categories.json")
    suspend fun getCategories(
        @Header("Cookie") cookie: String? = null
    ): CategoriesResponse

    @GET("c/{slug}/{id}.json")
    suspend fun getCategoryTopics(
        @Path("slug") slug: String,
        @Path("id") id: Int,
        @Header("Cookie") cookie: String? = null,
        @Query("page") page: Int = 0
    ): TopicListResponse

    // Search
    @GET("search.json")
    suspend fun search(
        @Query("q") query: String,
        @Header("Cookie") cookie: String? = null,
        @Query("page") page: Int = 1
    ): SearchResponse
    
    // Get CSRF Token (required for session-based POST requests)
    @GET("session/csrf")
    suspend fun getCsrfToken(
        @Header("Cookie") cookie: String,
        @Header("X-Requested-With") requestedWith: String = "XMLHttpRequest"
    ): CsrfResponse
    
    // Post Actions - Like
    @retrofit2.http.FormUrlEncoded
    @retrofit2.http.POST("post_actions")
    suspend fun likePost(
        @Header("Cookie") cookie: String,
        @Header("X-CSRF-Token") csrfToken: String,
        @retrofit2.http.Field("id") postId: Int,
        @retrofit2.http.Field("post_action_type_id") actionTypeId: Int = 2, // 2 = like
        @retrofit2.http.Field("flag_topic") flagTopic: Boolean = false
    ): retrofit2.Response<Unit>
    
    // Unlike - Delete post action
    @retrofit2.http.DELETE("post_actions/{postId}")
    suspend fun unlikePost(
        @Header("Cookie") cookie: String,
        @Header("X-CSRF-Token") csrfToken: String,
        @Path("postId") postId: Int,
        @Query("post_action_type_id") actionTypeId: Int = 2
    ): retrofit2.Response<Unit>
    
    @retrofit2.http.FormUrlEncoded
    @retrofit2.http.POST("posts")
    suspend fun createTopic(
        @Header("Cookie") cookie: String,
        @Header("X-CSRF-Token") csrfToken: String,
        @retrofit2.http.Field("title") title: String,
        @retrofit2.http.Field("raw") rawContent: String,
        @retrofit2.http.Field("category") categoryId: Int? = null,
        @retrofit2.http.Field("tags[]") tags: List<String> = emptyList(),
        @retrofit2.http.Field("archetype") archetype: String = "regular",
        @retrofit2.http.Field("target_recipients") targetRecipients: String? = null
    ): retrofit2.Response<CreatePostResponse>

    // Private Messages
    @GET("topics/private-messages/{username}.json")
    suspend fun getPrivateMessages(
        @Path("username") username: String,
        @Header("Cookie") cookie: String,
        @Query("page") page: Int = 0
    ): TopicListResponse

    // Create a new post (reply)
    @retrofit2.http.FormUrlEncoded
    @retrofit2.http.POST("posts")
    suspend fun createPost(
        @Header("Cookie") cookie: String,
        @Header("X-CSRF-Token") csrfToken: String,
        @retrofit2.http.Field("topic_id") topicId: Int,
        @retrofit2.http.Field("raw") rawContent: String,
        @retrofit2.http.Field("reply_to_post_number") replyToPostNumber: Int? = null,
        @retrofit2.http.Field("archetype") archetype: String = "regular"
    ): retrofit2.Response<CreatePostResponse>
    
    // Bookmark a post
    @retrofit2.http.FormUrlEncoded
    @retrofit2.http.POST("bookmarks")
    suspend fun createBookmark(
        @Header("Cookie") cookie: String,
        @Header("X-CSRF-Token") csrfToken: String,
        @retrofit2.http.Field("bookmarkable_id") postId: Int,
        @retrofit2.http.Field("bookmarkable_type") bookmarkableType: String = "Post"
    ): retrofit2.Response<BookmarkResponse>
    
    // Remove bookmark
    @retrofit2.http.DELETE("bookmarks/{id}")
    suspend fun deleteBookmark(
        @Header("Cookie") cookie: String,
        @Header("X-CSRF-Token") csrfToken: String,
        @Path("id") bookmarkId: Int
    ): retrofit2.Response<Unit>
    
    // User Bookmarks
    @GET("u/{username}/bookmarks.json")
    suspend fun getUserBookmarks(
        @Path("username") username: String,
        @Query("page") page: Int = 0,
        @Header("Cookie") cookie: String? = null,
        @Header("X-Requested-With") requestedWith: String = "XMLHttpRequest"
    ): io.github.xhuohai.sudo.data.model.TopicListResponse

    // User Actions (My Posts, etc)
    // filter: 4,5 (posts, replies)
    @GET("user_actions.json")
    suspend fun getUserActions(
        @Query("offset") offset: Int = 0,
        @Query("username") username: String,
        @Query("filter") filter: String = "4,5",
        @Header("Cookie") cookie: String? = null
    ): io.github.xhuohai.sudo.data.model.UserActionsResponse
}

@kotlinx.serialization.Serializable
data class CsrfResponse(
    val csrf: String
)

@kotlinx.serialization.Serializable
data class CreatePostResponse(
    val id: Int? = null,
    @kotlinx.serialization.SerialName("post_number")
    val postNumber: Int? = null,
    val success: Boolean? = null,
    val action: String? = null,
    val errors: List<String>? = null,
    @kotlinx.serialization.SerialName("topic_id")
    val topicId: Int? = null,
    @kotlinx.serialization.SerialName("topic_slug")
    val topicSlug: String? = null
)

@kotlinx.serialization.Serializable
data class BookmarkResponse(
    val id: Int? = null,
    val success: String? = null
)


