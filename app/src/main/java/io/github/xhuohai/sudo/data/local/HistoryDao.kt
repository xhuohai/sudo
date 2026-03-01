package io.github.xhuohai.sudo.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY visitedAt DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clearAllHistory()
    
    @Delete
    suspend fun deleteHistory(history: HistoryEntity)
    
    // Check if duplicate topicId usually we only update visitedAt?
    // Since unique constraint is not on topicId but PrimaryKey is auto-generated ID.
    // However, we want one entry per topic. So we should probably handle duplicates.
    // Maybe query by topicId first? Or make topicId unique index.
    
    @Query("SELECT * FROM history WHERE topicId = :topicId LIMIT 1")
    suspend fun getHistoryByTopicId(topicId: Int): HistoryEntity?
    
    @Query("DELETE FROM history WHERE topicId = :topicId")
    suspend fun deleteHistoryByTopicId(topicId: Int)
}
