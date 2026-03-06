package io.github.xhuohai.sudo.ui.profile.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.xhuohai.sudo.data.model.Topic
import io.github.xhuohai.sudo.data.model.User
import io.github.xhuohai.sudo.data.repository.AuthRepository
import io.github.xhuohai.sudo.data.repository.TopicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyBookmarksUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val topics: List<Topic> = emptyList(),
    val users: List<User> = emptyList(),
    val error: String? = null,
    val hasMore: Boolean = true,
    val page: Int = 0
)

@HiltViewModel
class MyBookmarksViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val topicRepository: TopicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyBookmarksUiState())
    val uiState: StateFlow<MyBookmarksUiState> = _uiState.asStateFlow()

    init {
        loadBookmarks(refresh = true)
    }

    fun loadBookmarks(refresh: Boolean = false) {
        viewModelScope.launch {
            val username = authRepository.username.first()
            if (username.isNullOrBlank()) {
                _uiState.update { it.copy(error = "未登录") }
                return@launch
            }

            if (refresh) {
                _uiState.update { it.copy(isRefreshing = true, error = null, page = 0, hasMore = true) }
            } else {
                if (!uiState.value.hasMore || uiState.value.isLoading) return@launch
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            val currentPage = if (refresh) 0 else _uiState.value.page
            
            topicRepository.getBookmarks(username, currentPage).fold(
                onSuccess = { (newTopics, newUsers) ->
                    
                    _uiState.update { state ->
                        // Merge users list
                        val accumulatedUsers = (state.users + newUsers).distinctBy { it.id }
                        
                        // Check for duplicates to prevent infinite loops if API ignores pagination
                        val existingIds = state.topics.map { it.id }.toSet()
                        val trulyNewTopics = newTopics.filter { !existingIds.contains(it.id) }
                        
                        val updatedTopics = if (refresh) newTopics else state.topics + trulyNewTopics
                        
                        // If we received topics but they were all duplicates, assume we reached the end
                        val hasMoreActual = if (refresh) newTopics.isNotEmpty() else trulyNewTopics.isNotEmpty()
                        
                        state.copy(
                            isLoading = false,
                            isRefreshing = false,
                            topics = updatedTopics,
                            users = accumulatedUsers,
                            page = currentPage + 1,
                            hasMore = hasMoreActual && newTopics.isNotEmpty()
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            isRefreshing = false, 
                            error = error.message ?: "加载失败"
                        ) 
                    }
                }
            )
        }
    }

    fun refresh() {
        loadBookmarks(refresh = true)
    }
    
    fun loadMore() {
        loadBookmarks(refresh = false)
    }
}
