package io.github.xhuohai.sudo.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.xhuohai.sudo.data.model.SearchTopic
import io.github.xhuohai.sudo.data.model.User
import io.github.xhuohai.sudo.data.repository.SearchHistoryRepository
import io.github.xhuohai.sudo.data.repository.TopicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val topics: List<SearchTopic> = emptyList(),
    val users: List<User> = emptyList(),
    val error: String? = null,
    val hasSearched: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: TopicRepository,
    private val searchHistoryRepository: SearchHistoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    val searchHistory: StateFlow<List<String>> = searchHistoryRepository.searchHistory
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    private var searchJob: Job? = null
    
    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        
        // Debounced search - don't save to history (intermediate input)
        searchJob?.cancel()
        if (query.length >= 2) {
            searchJob = viewModelScope.launch {
                delay(500) // Wait 500ms before searching
                performSearch(saveToHistory = false)
            }
        } else {
            _uiState.update { it.copy(topics = emptyList(), hasSearched = false) }
        }
    }
    
    fun search() {
        if (_uiState.value.query.isNotBlank()) {
            searchJob?.cancel()
            // Explicit search - save to history
            performSearch(saveToHistory = true)
        }
    }
    
    fun searchFromHistory(query: String) {
        _uiState.update { it.copy(query = query) }
        // From history - don't save again (already in history)
        performSearch(saveToHistory = false)
    }
    
    private fun performSearch(saveToHistory: Boolean = false) {
        val query = _uiState.value.query
        if (query.isBlank()) return
        
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            // Only save to history on explicit search action
            if (saveToHistory) {
                searchHistoryRepository.addSearchQuery(query)
            }
            
            val result = repository.search(query)
            
            result.fold(
                onSuccess = { response ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            topics = response.topics,
                            users = response.users,
                            hasSearched = true,
                            error = null
                        ) 
                    }
                },
                onFailure = { exception ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            error = exception.message ?: "搜索失败",
                            hasSearched = true
                        ) 
                    }
                }
            )
        }
    }
    
    fun removeHistoryItem(query: String) {
        viewModelScope.launch {
            searchHistoryRepository.removeSearchQuery(query)
        }
    }
    
    fun clearHistory() {
        viewModelScope.launch {
            searchHistoryRepository.clearHistory()
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

