package io.github.xhuohai.sudo.data.repository

import io.github.xhuohai.sudo.data.model.Category
import io.github.xhuohai.sudo.data.model.Post
import io.github.xhuohai.sudo.data.model.PostsResponse
import io.github.xhuohai.sudo.data.model.SearchResponse
import io.github.xhuohai.sudo.data.model.Topic
import io.github.xhuohai.sudo.data.model.TopicDetailResponse
import io.github.xhuohai.sudo.data.model.User
import io.github.xhuohai.sudo.data.remote.CreatePostResponse
import io.github.xhuohai.sudo.data.remote.DiscourseApi
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TopicRepository @Inject constructor(
    private val api: DiscourseApi,
    private val authRepository: AuthRepository
) {
    private suspend fun getCookie(): String? = authRepository.cookie.first()

    suspend fun getLatestTopics(page: Int = 0): Result<Pair<List<Topic>, List<User>>> {
        return try {
            val response = api.getLatestTopics(
                cookie = getCookie(),
                page = page
            )
            Result.success(Pair(response.topicList.topics, response.users))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTopTopics(period: String = "daily", page: Int = 0): Result<Pair<List<Topic>, List<User>>> {
        return try {
            val response = api.getTopTopics(
                cookie = getCookie(),
                period = period,
                page = page
            )
            Result.success(Pair(response.topicList.topics, response.users))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNewTopics(page: Int = 0): Result<Pair<List<Topic>, List<User>>> {
        return try {
            val response = api.getNewTopics(
                cookie = getCookie(),
                page = page
            )
            Result.success(Pair(response.topicList.topics, response.users))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUnreadTopics(page: Int = 0): Result<Pair<List<Topic>, List<User>>> {
        return try {
            val response = api.getUnreadTopics(
                cookie = getCookie(),
                page = page
            )
            Result.success(Pair(response.topicList.topics, response.users))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBookmarks(username: String, page: Int = 0): Result<Pair<List<Topic>, List<User>>> {
        return try {
            val response = api.getUserBookmarks(
                username = username,
                page = page,
                cookie = getCookie()
            )
            Result.success(Pair(response.topicList.topics, response.users))
        } catch (e: Exception) {
            android.util.Log.e("TopicRepository", "Error fetching bookmarks for $username", e)
            Result.failure(e)
        }
    }

    suspend fun getMyPosts(username: String, offset: Int = 0): Result<Pair<List<Topic>, List<User>>> {
        return try {
            val response = api.getUserActions(
                offset = offset,
                username = username,
                cookie = getCookie()
            )
            val topics = response.userActions.map { it.toTopic() }
            val users = response.userActions.map { it.toUser() }.distinctBy { it.id }
            Result.success(Pair(topics, users))
        } catch (e: Exception) {
            android.util.Log.e("TopicRepository", "Error fetching posts for $username", e)
            Result.failure(e)
        }
    }

    enum class TopicType {
        LATEST, TOP, NEW, UNREAD, BOOKMARKS, MY_POSTS
    }

    suspend fun fetchTopics(type: TopicType, page: Int = 0, username: String? = null): Result<Pair<List<Topic>, List<User>>> {
        return when (type) {
            TopicType.LATEST -> getLatestTopics(page)
            TopicType.TOP -> getTopTopics(page = page)
            TopicType.NEW -> getNewTopics(page)
            TopicType.UNREAD -> getUnreadTopics(page)
            TopicType.BOOKMARKS -> {
                if (username == null) Result.failure(Exception("Username required for bookmarks"))
                else getBookmarks(username, page)
            }
            TopicType.MY_POSTS -> {
                if (username == null) Result.failure(Exception("Username required for my posts"))
                else getMyPosts(username, offset = page * 30) // Approximation for user_actions offset
            }
        }
    }

    suspend fun getTopicDetail(slug: String, id: Int, page: Int = 1): Result<TopicDetailResponse> {
        return try {
            val cookie = getCookie()
            android.util.Log.d("BookmarkDebug", "getTopicDetail: cookie=${if (cookie != null) "present(${cookie.take(50)}...)" else "null"}")
            val response = api.getTopicDetail(
                slug = slug,
                id = id,
                cookie = cookie,
                page = page
            )
            android.util.Log.d("BookmarkDebug", "API response: bookmarked=${response.bookmarked}, bookmarks=${response.bookmarks}")
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createTopic(title: String, raw: String, categoryId: Int, tags: List<String> = emptyList()): Result<CreatePostResponse> {
        return try {
            val cookie = getCookie() ?: return Result.failure(Exception("Not logged in"))
            val csrfToken = getCsrfToken(cookie) ?: return Result.failure(Exception("Failed to get CSRF token"))
            
            val response = api.createTopic(
                cookie = cookie,
                csrfToken = csrfToken,
                title = title,
                rawContent = raw,
                categoryId = categoryId,
                tags = tags
            )
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception("Failed to create topic: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createPrivateMessage(title: String, raw: String, targetRecipients: String): Result<CreatePostResponse> {
        return try {
            val cookie = getCookie() ?: return Result.failure(Exception("Not logged in"))
            val csrfToken = getCsrfToken(cookie) ?: return Result.failure(Exception("Failed to get CSRF token"))
            
            val response = api.createTopic(
                cookie = cookie,
                csrfToken = csrfToken,
                title = title,
                rawContent = raw,
                categoryId = null,
                archetype = "private_message",
                targetRecipients = targetRecipients
            )
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception("Failed to send message: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPrivateMessages(username: String, page: Int = 0): Result<Pair<List<Topic>, List<User>>> {
        return try {
            val cookie = getCookie() ?: return Result.failure(Exception("Not logged in"))
            val response = api.getPrivateMessages(
                username = username,
                cookie = cookie,
                page = page
            )
            Result.success(Pair(response.topicList.topics, response.users))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPostsByIds(topicId: Int, postIds: List<Int>): Result<List<Post>> {
        return try {
            val response = api.getPostsByIds(
                topicId = topicId,
                postIds = postIds,
                cookie = getCookie()
            )
            Result.success(response.postStream?.posts ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Categories logic moved to CategoryRepository

    suspend fun getCategoryTopics(slug: String, id: Int, page: Int = 0): Result<Pair<List<Topic>, List<User>>> {
        return try {
            val response = api.getCategoryTopics(
                slug = slug,
                id = id,
                cookie = getCookie(),
                page = page
            )
            Result.success(Pair(response.topicList.topics, response.users))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun search(query: String, page: Int = 1): Result<SearchResponse> {
        return try {
            val response = api.search(
                query = query,
                cookie = getCookie(),
                page = page
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun getCsrfToken(cookie: String): String? {
        return try {
            val response = api.getCsrfToken(cookie)
            response.csrf
        } catch (e: Exception) {
            android.util.Log.e("CSRF", "Failed to get CSRF token", e)
            null
        }
    }
    
    suspend fun likePost(postId: Int): Result<Boolean> {
        val cookie = getCookie() ?: return Result.failure(Exception("请先登录"))
        val csrfToken = getCsrfToken(cookie) ?: return Result.failure(Exception("获取CSRF token失败"))
        return try {
            val response = api.likePost(cookie = cookie, csrfToken = csrfToken, postId = postId)
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("点赞失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun unlikePost(postId: Int): Result<Boolean> {
        val cookie = getCookie() ?: return Result.failure(Exception("请先登录"))
        val csrfToken = getCsrfToken(cookie) ?: return Result.failure(Exception("获取CSRF token失败"))
        return try {
            val response = api.unlikePost(cookie = cookie, csrfToken = csrfToken, postId = postId)
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("取消点赞失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createPost(
        topicId: Int,
        content: String,
        replyToPostNumber: Int? = null
    ): Result<CreatePostResponse> {
        val cookie = getCookie() ?: return Result.failure(Exception("请先登录"))
        val csrfToken = getCsrfToken(cookie) ?: return Result.failure(Exception("获取CSRF token失败"))
        return try {
            val response = api.createPost(
                cookie = cookie,
                csrfToken = csrfToken,
                topicId = topicId,
                rawContent = content,
                replyToPostNumber = replyToPostNumber
            )
            if (response.isSuccessful && response.body()?.id != null) {
                Result.success(response.body()!!)
            } else {
                val errors = response.body()?.errors?.joinToString(", ") ?: "未知错误"
                Result.failure(Exception("发送失败: $errors"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun bookmarkPost(postId: Int): Result<Int> {
        val cookie = getCookie() ?: return Result.failure(Exception("请先登录"))
        val csrfToken = getCsrfToken(cookie) ?: return Result.failure(Exception("获取CSRF token失败"))
        return try {
            val response = api.createBookmark(cookie = cookie, csrfToken = csrfToken, postId = postId)
            if (response.isSuccessful && response.body()?.id != null) {
                Result.success(response.body()!!.id!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                Result.failure(Exception("收藏失败: ${response.code()} - $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun unbookmarkPost(bookmarkId: Int): Result<Boolean> {
        val cookie = getCookie() ?: return Result.failure(Exception("请先登录"))
        val csrfToken = getCsrfToken(cookie) ?: return Result.failure(Exception("获取CSRF token失败"))
        return try {
            val response = api.deleteBookmark(cookie = cookie, csrfToken = csrfToken, bookmarkId = bookmarkId)
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                Result.failure(Exception("取消收藏失败: ${response.code()} - $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserBookmarks(username: String, page: Int = 0): Result<io.github.xhuohai.sudo.data.model.TopicListResponse> {
        val cookie = getCookie() ?: return Result.failure(Exception("请先登录"))
        return try {
            val response = api.getUserBookmarks(
                username = username,
                page = page,
                cookie = cookie
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

