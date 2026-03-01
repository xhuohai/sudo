package io.github.xhuohai.sudo.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "history", indices = [Index(value = ["topicId"], unique = true)])
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val topicId: Int,
    val title: String,
    val categoryId: Int? = null,
    val visitedAt: Long = System.currentTimeMillis(),
    val excerpt: String? = null,
    val imageUrl: String? = null,
    val replyCount: Int = 0,
    val likeCount: Int = 0,
    val views: Int = 0,
    val slug: String,
    val lastPosterUsername: String? = null
)
