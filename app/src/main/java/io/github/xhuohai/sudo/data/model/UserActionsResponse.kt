package io.github.xhuohai.sudo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserActionsResponse(
    @SerialName("user_actions")
    val userActions: List<UserAction> = emptyList()
)

@Serializable
data class UserAction(
    @SerialName("action_type")
    val actionType: Int,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("avatar_template")
    val avatarTemplate: String = "",
    @SerialName("user_id")
    val userId: Int,
    val username: String,
    @SerialName("topic_id")
    val topicId: Int,
    @SerialName("topic_slug")
    val topicSlug: String = "",
    val title: String,
    val excerpt: String = "",
    @SerialName("category_id")
    val categoryId: Int = 0,
    @SerialName("post_id")
    val postId: Int? = null,
    @SerialName("post_number")
    val postNumber: Int = 1,
    @SerialName("reply_count")
    val replyCount: Int = 0,
    @SerialName("like_count")
    val likeCount: Int = 0,
    @SerialName("views")
    val views: Int = 0,
    val name: String? = null
) {
    fun toTopic(): Topic {
        // Map UserAction to Topic for display in list
        return Topic(
            id = topicId,
            title = title,
            slug = topicSlug,
            postsCount = 0, // Not provided in user_actions
            replyCount = replyCount,
            views = views,
            likeCount = likeCount,
            categoryId = categoryId,
            createdAt = createdAt,
            lastPostedAt = createdAt,
            excerpt = excerpt,
            posters = emptyList(), // Not provided
            lastPosterUsername = username // Use action user
        )
    }

    fun toUser(): User {
         return User(
            id = userId,
            username = username,
            name = name,
            avatarTemplate = avatarTemplate
        )
    }
}
