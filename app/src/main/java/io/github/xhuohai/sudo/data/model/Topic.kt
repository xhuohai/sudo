package io.github.xhuohai.sudo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TopicListResponse(
    val users: List<User> = emptyList(),
    @SerialName("topic_list")
    val topicList: TopicList
)

@Serializable
data class TopicList(
    val topics: List<Topic> = emptyList(),
    @SerialName("more_topics_url")
    val moreTopicsUrl: String? = null,
    @SerialName("per_page")
    val perPage: Int = 30
)

@Serializable
data class Topic(
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
    @SerialName("last_posted_at")
    val lastPostedAt: String? = null,
    val bumped: Boolean = false,
    @SerialName("bumped_at")
    val bumpedAt: String? = null,
    val pinned: Boolean = false,
    val visible: Boolean = true,
    val closed: Boolean = false,
    val archived: Boolean = false,
    @SerialName("image_url")
    val imageUrl: String? = null,
    val excerpt: String? = null,
    @SerialName("has_summary")
    val hasSummary: Boolean = false,
    val posters: List<Poster> = emptyList(),
    val tags: List<Tag> = emptyList(),
    @SerialName("last_poster_username")
    val lastPosterUsername: String? = null,
    val participants: List<Participant> = emptyList(),
    // Bookmark info
    val bookmarked: Boolean = false,
    @SerialName("bookmark_id")
    val bookmarkId: Int? = null
)

@Serializable
data class Participant(
    val id: Int,
    val username: String,
    val name: String? = null,
    @SerialName("avatar_template")
    val avatarTemplate: String,
    @SerialName("post_count")
    val postCount: Int = 0,
    @SerialName("primary_group_name")
    val primaryGroupName: String? = null,
    @SerialName("flair_name")
    val flairName: String? = null
) {
    fun getAvatarUrl(size: Int = 120): String {
        return "https://linux.do" + avatarTemplate.replace("{size}", size.toString())
    }
}

@Serializable
data class Tag(
    val id: Int? = null,
    val name: String = "",
    val slug: String? = null
)

@Serializable
data class Poster(
    val extras: String? = null,
    val description: String = "",
    @SerialName("user_id")
    val userId: Int,
    @SerialName("primary_group_id")
    val primaryGroupId: Int? = null
)

@Serializable
data class User(
    val id: Int,
    val username: String,
    val name: String? = null,
    @SerialName("avatar_template")
    val avatarTemplate: String = "",
    @SerialName("trust_level")
    val trustLevel: Int = 0,
    val admin: Boolean = false,
    val moderator: Boolean = false
) {
    fun getAvatarUrl(size: Int = 120): String {
        return "https://linux.do" + avatarTemplate.replace("{size}", size.toString())
    }
}
