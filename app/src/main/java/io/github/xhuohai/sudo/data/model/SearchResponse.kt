package io.github.xhuohai.sudo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response model for Discourse search API
 * The search API returns a different structure than topic list endpoints
 */
@Serializable
data class SearchResponse(
    val posts: List<SearchPost> = emptyList(),
    val topics: List<SearchTopic> = emptyList(),
    val users: List<User> = emptyList(),
    @SerialName("grouped_search_result")
    val groupedSearchResult: GroupedSearchResult? = null
)

@Serializable
data class SearchPost(
    val id: Int,
    @SerialName("topic_id")
    val topicId: Int,
    val username: String = "",
    @SerialName("avatar_template")
    val avatarTemplate: String = "",
    val blurb: String = "",
    @SerialName("post_number")
    val postNumber: Int = 1
)

@Serializable
data class SearchTopic(
    val id: Int,
    val title: String = "",
    val slug: String = "",
    @SerialName("posts_count")
    val postsCount: Int = 0,
    @SerialName("reply_count")
    val replyCount: Int = 0,
    val views: Int = 0,
    @SerialName("like_count")
    val likeCount: Int = 0,
    @SerialName("category_id")
    val categoryId: Int = 0,
    val closed: Boolean = false,
    val archived: Boolean = false
)

@Serializable
data class GroupedSearchResult(
    @SerialName("more_posts")
    val morePosts: Boolean? = null,
    @SerialName("more_users")
    val moreUsers: Boolean? = null,
    @SerialName("more_categories")
    val moreCategories: Boolean? = null,
    @SerialName("term")
    val term: String = "",
    @SerialName("search_log_id")
    val searchLogId: Long? = null,
    @SerialName("post_ids")
    val postIds: List<Int> = emptyList(),
    @SerialName("user_ids")
    val userIds: List<Int> = emptyList(),
    @SerialName("category_ids")
    val categoryIds: List<Int> = emptyList()
)
