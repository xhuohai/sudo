package io.github.xhuohai.sudo.data.repository

import io.github.xhuohai.sudo.data.local.HistoryDao
import io.github.xhuohai.sudo.data.local.HistoryEntity
import io.github.xhuohai.sudo.data.model.Topic
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao
) {
    val historyItems: Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    suspend fun addHistory(topic: Topic) {
        val entity = HistoryEntity(
            topicId = topic.id,
            title = topic.title,
            categoryId = topic.categoryId,
            visitedAt = System.currentTimeMillis(),
            excerpt = topic.excerpt,
            imageUrl = topic.imageUrl,
            replyCount = topic.postsCount ?: 0,
            likeCount = topic.likeCount,
            views = topic.views,
            slug = topic.slug ?: "",
            lastPosterUsername = topic.lastPosterUsername
        )
        // If it exists, we want to update it (especially visitedAt)
        // Since we have unique index on topicId, REPLACE strategy in DAO handles this?
        // Wait, REPLACE replaces the row. ID might change if auto-generate is true and we don't provide ID.
        // It's better to verify if it exists to keep the same ID or just let it replace.
        // If we replace, the ID changes. That's fine for history usually.
        // But if we want to check if existing one has same ID...
        // Actually, let's just use INSERT OR REPLACE.
        // However, we might want to preserve the ID if we used it for pagination/stable IDs?
        // Room's OnConflictStrategy.REPLACE will delete old and insert new.
        
        // Let's try to get existing one first to keep ID?
        val existing = historyDao.getHistoryByTopicId(topic.id)
        val entityToInsert = if (existing != null) {
            entity.copy(id = existing.id)
        } else {
            entity
        }
        historyDao.insertHistory(entityToInsert)
    }

    suspend fun clearHistory() {
        historyDao.clearAllHistory()
    }

    suspend fun deleteHistoryItem(history: HistoryEntity) {
        historyDao.deleteHistory(history)
    }
}
