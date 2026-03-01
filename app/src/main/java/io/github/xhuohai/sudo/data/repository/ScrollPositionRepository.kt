package io.github.xhuohai.sudo.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.scrollDataStore: DataStore<Preferences> by preferencesDataStore(name = "scroll_positions")

/**
 * Repository for storing and retrieving scroll positions for topics.
 * Persists the last viewed post index for each topic.
 */
@Singleton
class ScrollPositionRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Save the scroll position (first visible item index) for a topic.
     */
    suspend fun saveScrollPosition(topicId: Int, firstVisibleItemIndex: Int) {
        Log.d("ScrollPos", "SAVING topicId=$topicId, position=$firstVisibleItemIndex")
        val key = intPreferencesKey("topic_$topicId")
        context.scrollDataStore.edit { preferences ->
            preferences[key] = firstVisibleItemIndex
        }
        Log.d("ScrollPos", "SAVED topicId=$topicId, position=$firstVisibleItemIndex")
    }
    
    /**
     * Get the saved scroll position for a topic.
     * Returns 0 if no position was saved.
     */
    suspend fun getScrollPosition(topicId: Int): Int {
        val key = intPreferencesKey("topic_$topicId")
        val position = context.scrollDataStore.data
            .map { preferences -> preferences[key] ?: 0 }
            .first()
        Log.d("ScrollPos", "LOADED topicId=$topicId, position=$position")
        return position
    }
    
    /**
     * Clear scroll position for a specific topic.
     */
    suspend fun clearScrollPosition(topicId: Int) {
        val key = intPreferencesKey("topic_$topicId")
        context.scrollDataStore.edit { preferences ->
            preferences.remove(key)
        }
    }
}
