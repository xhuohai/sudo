package io.github.xhuohai.sudo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope


import io.github.xhuohai.sudo.data.model.Topic
import io.github.xhuohai.sudo.data.model.User
import io.github.xhuohai.sudo.data.repository.TopicRepository
import io.github.xhuohai.sudo.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val topics: List<Topic> = emptyList(),
    val users: List<User> = emptyList(),
    val error: String? = null,
    val currentTab: TopicTab = TopicTab.Latest,
    val currentPage: Int = 0,
    val hasMore: Boolean = true
)

enum class TopicTab(val label: String, val type: TopicRepository.TopicType) {
    Latest("最新", TopicRepository.TopicType.LATEST),
    Top("热门", TopicRepository.TopicType.TOP),
    New("新帖", TopicRepository.TopicType.NEW),
    Unread("未读", TopicRepository.TopicType.UNREAD),
    Bookmarks("书签", TopicRepository.TopicType.BOOKMARKS),
    MyPosts("我的帖子", TopicRepository.TopicType.MY_POSTS)
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val topicRepository: TopicRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Cache for each tab: Tab -> (Topics, Users, Page, HasMore)
    private val tabDataCache = mutableMapOf<TopicTab, TabData>()

    private data class TabData(
        val topics: List<Topic>,
        val users: List<User>,
        val currentPage: Int,
        val hasMore: Boolean,
        val scrollIndex: Int = 0,
        val scrollOffset: Int = 0
    )

    init {
        loadTopics()
    }

    fun loadTopics(refresh: Boolean = false) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !refresh && it.topics.isEmpty(),
                    isRefreshing = refresh,
                    error = null
                )
            }

            val currentTab = _uiState.value.currentTab
            val username = authRepository.username.first()
            val result = topicRepository.fetchTopics(currentTab.type, page = 0, username = username)

            result.fold(
                onSuccess = { (topics, users) ->
                    // Update cache
                    tabDataCache[currentTab] = TabData(topics, users, 0, topics.size >= 30)
                    
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            topics = topics,
                            users = users,
                            currentPage = 0,
                            hasMore = topics.size >= 30
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = throwable.message ?: "Unknown error"
                        )
                    }
                }
            )
        }
    }

    fun loadMoreTopics() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            val currentTab = _uiState.value.currentTab
            val nextPage = _uiState.value.currentPage + 1
            val username = authRepository.username.first()
            val result = topicRepository.fetchTopics(currentTab.type, page = nextPage, username = username)

            result.fold(
                onSuccess = { (newTopics, newUsers) ->
                    val updatedTopics = _uiState.value.topics + newTopics
                    val updatedUsers = (_uiState.value.users + newUsers).distinctBy { user -> user.id }
                    
                    // Update cache
                    tabDataCache[currentTab] = TabData(updatedTopics, updatedUsers, nextPage, newTopics.size >= 30)

                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            topics = updatedTopics,
                            users = updatedUsers,
                            currentPage = nextPage,
                            hasMore = newTopics.size >= 30
                        )
                    }
                },
                onFailure = { _ ->
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
            )
        }
    }

    fun selectTab(tab: TopicTab) {
        if (_uiState.value.currentTab != tab) {
            // Save current tab state to cache (if needed for scroll position, handled by UI mostly)
            // Restore state from cache if available
            val cachedData = tabDataCache[tab]
            
            if (cachedData != null) {
                _uiState.update {
                    it.copy(
                        currentTab = tab,
                        topics = cachedData.topics,
                        users = cachedData.users,
                        currentPage = cachedData.currentPage,
                        hasMore = cachedData.hasMore,
                        error = null,
                        isLoading = false
                    )
                }
            } else {
                // No cache, load fresh
                _uiState.update {
                    it.copy(
                        currentTab = tab,
                        topics = emptyList(),
                        users = emptyList(),
                        currentPage = 0,
                        hasMore = true,
                        error = null
                    )
                }
                loadTopics()
            }
        }
    }

    fun refresh() {
        loadTopics(refresh = true)
    }
}
