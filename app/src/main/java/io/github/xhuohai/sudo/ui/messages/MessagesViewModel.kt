package io.github.xhuohai.sudo.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.xhuohai.sudo.data.model.Topic
import io.github.xhuohai.sudo.data.model.User
import io.github.xhuohai.sudo.data.repository.AuthRepository
import io.github.xhuohai.sudo.data.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MessagesUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoggedIn: Boolean = false,
    val topics: List<Topic> = emptyList(),
    val users: List<User> = emptyList(),
    val error: String? = null,
    val page: Int = 0,
    val hasMore: Boolean = true
)

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val topicRepository: TopicRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    init {
        loadMessages()
    }

    fun loadMessages(refresh: Boolean = false) {
        viewModelScope.launch {
            if (refresh) {
                _uiState.update { it.copy(isRefreshing = true, page = 0, hasMore = true, error = null) }
            } else {
                if (_uiState.value.isLoading) return@launch
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            val username = authRepository.username.first()
            if (username.isNullOrEmpty()) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        isRefreshing = false,
                        isLoggedIn = false, 
                        error = "请先登录" 
                    ) 
                }
                return@launch
            } else {
                 _uiState.update { it.copy(isLoggedIn = true) }
            }

            val page = if (refresh) 0 else _uiState.value.page
            val result = topicRepository.getPrivateMessages(username, page)
            
            result.fold(
                onSuccess = { (newTopics, newUsers) ->
                    _uiState.update { currentState ->
                        val combinedTopics = if (refresh) newTopics else currentState.topics + newTopics
                        val combinedUsers = if (refresh) newUsers else (currentState.users + newUsers).distinctBy { it.id }
                        
                        currentState.copy(
                            isLoading = false,
                            isRefreshing = false,
                            topics = combinedTopics,
                            users = combinedUsers,
                            page = page + 1,
                            hasMore = newTopics.isNotEmpty()
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            isRefreshing = false,
                            error = error.message 
                        ) 
                    }
                }
            )
        }
    }
    
    fun refresh() {
        loadMessages(refresh = true)
    }
    
    fun loadMore() {
        if (!_uiState.value.isLoading && !_uiState.value.isRefreshing && _uiState.value.hasMore) {
            loadMessages(refresh = false)
        }
    }
}
