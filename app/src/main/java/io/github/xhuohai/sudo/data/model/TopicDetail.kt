package io.github.xhuohai.sudo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TopicDetailResponse(
    val id: Int,
    val title: String,
    val slug: String,
    @SerialName("posts_count")
    val postsCount: Int = 0,
    @SerialName("reply_count")
    val replyCount: Int = 0,
    val views: Int = 0,
    @SerialName("like_count")
    val likeCount: Int = 0,
    @SerialName("category_id")
    val categoryId: Int? = null,
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("post_stream")
    val postStream: PostStream,
    val details: TopicDetails? = null,
    val tags: List<Tag> = emptyList(),
    // Bookmark fields
    val bookmarked: Boolean = false,
    val bookmarks: List<TopicBookmark> = emptyList()
)

@Serializable
data class TopicBookmark(
    val id: Int,
    @SerialName("bookmarkable_id")
    val bookmarkableId: Int? = null,
    @SerialName("bookmarkable_type")
    val bookmarkableType: String? = null
)

@Serializable
data class PostStream(
    val posts: List<Post> = emptyList(),
    val stream: List<Int> = emptyList()
)

@Serializable
data class PostsResponse(
    @SerialName("post_stream")
    val postStream: PostStreamPosts? = null
)

@Serializable
data class PostStreamPosts(
    val posts: List<Post> = emptyList()
)

@Serializable
data class Post(
    val id: Int,
    val name: String? = null,
    val username: String = "",
    @SerialName("avatar_template")
    val avatarTemplate: String = "",
    @SerialName("created_at")
    val createdAt: String = "",
    val cooked: String = "", // HTML content
    @SerialName("post_number")
    val postNumber: Int = 1,
    @SerialName("post_type")
    val postType: Int = 1,
    @SerialName("reply_count")
    val replyCount: Int = 0,
    @SerialName("reply_to_post_number")
    val replyToPostNumber: Int? = null,
    @SerialName("quote_count")
    val quoteCount: Int = 0,
    @SerialName("reads")
    val reads: Int = 0,
    @SerialName("readers_count")
    val readersCount: Int = 0,
    val score: Double = 0.0,
    val yours: Boolean = false,
    @SerialName("topic_id")
    val topicId: Int = 0,
    @SerialName("topic_slug")
    val topicSlug: String = "",
    @SerialName("display_username")
    val displayUsername: String? = null,
    @SerialName("user_title")
    val userTitle: String? = null,
    @SerialName("trust_level")
    val trustLevel: Int = 0,
    val admin: Boolean = false,
    val moderator: Boolean = false,
    @SerialName("actions_summary")
    val actionsSummary: List<ActionSummary> = emptyList(),
    @SerialName("can_edit")
    val canEdit: Boolean = false,
    @SerialName("can_delete")
    val canDelete: Boolean = false,
    @SerialName("can_recover")
    val canRecover: Boolean = false,
    @SerialName("link_counts")
    val linkCounts: List<LinkCount> = emptyList(),
    val bookmarked: Boolean = false,
    @SerialName("bookmark_id")
    val bookmarkId: Int? = null
) {
    fun getAvatarUrl(size: Int = 120): String {
        return "https://linux.do" + avatarTemplate.replace("{size}", size.toString())
    }
    
    val isLiked: Boolean
        get() = actionsSummary.find { it.id == 2 }?.acted == true
        
    val likeCount: Int
        get() = actionsSummary.find { it.id == 2 }?.count ?: 0
        
    val canLike: Boolean
        get() = actionsSummary.find { it.id == 2 }?.canAct == true
}

@Serializable
data class ActionSummary(
    val id: Int,
    val count: Int = 0,
    @SerialName("can_act")
    val canAct: Boolean = false,
    val acted: Boolean = false
)

@Serializable
data class LinkCount(
    val url: String,
    val internal: Boolean = false,
    val reflection: Boolean = false,
    val title: String? = null,
    val clicks: Int = 0
)

@Serializable
data class TopicDetails(
    @SerialName("can_edit")
    val canEdit: Boolean = false,
    @SerialName("can_delete")
    val canDelete: Boolean = false,
    @SerialName("can_create_post")
    val canCreatePost: Boolean = false,
    @SerialName("can_reply_as_new_topic")
    val canReplyAsNewTopic: Boolean = false,
    @SerialName("can_invite_to")
    val canInviteTo: Boolean = false,
    val participants: List<Participant> = emptyList(),
    @SerialName("created_by")
    val createdBy: ParticipantUser? = null,
    @SerialName("last_poster")
    val lastPoster: ParticipantUser? = null,
    val bookmarks: List<TopicBookmark> = emptyList(),
    val bookmarked: Boolean = false
)
@Serializable
data class ParticipantUser(
    val id: Int,
    val username: String,
    val name: String? = null,
    @SerialName("avatar_template")
    val avatarTemplate: String = ""
)
