package io.github.xhuohai.sudo.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.searchHistoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "search_history")

@Singleton
class SearchHistoryRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val HISTORY_KEY = stringPreferencesKey("search_history")
        private const val MAX_HISTORY_SIZE = 20
        private const val SEPARATOR = "|||"
    }
    
    val searchHistory: Flow<List<String>> = context.searchHistoryDataStore.data
        .map { preferences ->
            val historyString = preferences[HISTORY_KEY] ?: ""
            if (historyString.isEmpty()) {
                emptyList()
            } else {
                historyString.split(SEPARATOR)
            }
        }
    
    suspend fun addSearchQuery(query: String) {
        if (query.isBlank()) return
        
        context.searchHistoryDataStore.edit { preferences ->
            val currentHistory = preferences[HISTORY_KEY]?.split(SEPARATOR)?.toMutableList() ?: mutableListOf()
            
            // Remove if already exists (to move to top)
            currentHistory.remove(query)
            
            // Add to the beginning
            currentHistory.add(0, query)
            
            // Keep only MAX_HISTORY_SIZE items
            val trimmedHistory = currentHistory.take(MAX_HISTORY_SIZE)
            
            preferences[HISTORY_KEY] = trimmedHistory.joinToString(SEPARATOR)
        }
    }
    
    suspend fun removeSearchQuery(query: String) {
        context.searchHistoryDataStore.edit { preferences ->
            val currentHistory = preferences[HISTORY_KEY]?.split(SEPARATOR)?.toMutableList() ?: mutableListOf()
            currentHistory.remove(query)
            preferences[HISTORY_KEY] = currentHistory.joinToString(SEPARATOR)
        }
    }
    
    suspend fun clearHistory() {
        context.searchHistoryDataStore.edit { preferences ->
            preferences.remove(HISTORY_KEY)
        }
    }
}
