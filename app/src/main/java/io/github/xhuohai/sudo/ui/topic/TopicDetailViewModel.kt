package io.github.xhuohai.sudo.ui.topic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.xhuohai.sudo.data.model.Post
import io.github.xhuohai.sudo.data.model.TopicDetailResponse
import io.github.xhuohai.sudo.data.remote.CreatePostResponse
import io.github.xhuohai.sudo.data.repository.TopicRepository
import io.github.xhuohai.sudo.data.repository.ScrollPositionRepository
import io.github.xhuohai.sudo.data.repository.HistoryRepository
import io.github.xhuohai.sudo.data.model.Topic
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TopicDetailUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val topicDetail: TopicDetailResponse? = null,
    val posts: List<Post> = emptyList(),
    val error: String? = null,
    val currentPage: Int = 1,
    val hasMorePages: Boolean = true,
    val lastReadPostNumber: Int = 0,  // 0 means not read before
    val scrollToPostNumber: Int = 0,  // Post to scroll to after loading (0 = don't scroll)
    val isBookmarked: Boolean = false,
    val bookmarkId: Int? = null
)

@HiltViewModel
class TopicDetailViewModel @Inject constructor(
    private val topicRepository: TopicRepository,
    private val scrollPositionRepository: ScrollPositionRepository,
    private val historyRepository: HistoryRepository,
    private val authRepository: io.github.xhuohai.sudo.data.repository.AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val topicId: Int = savedStateHandle["topicId"] ?: 0
    private val slug: String = savedStateHandle["slug"] ?: ""

    private val _uiState = MutableStateFlow(TopicDetailUiState())
    val uiState: StateFlow<TopicDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // First load the last read position
            val lastRead = scrollPositionRepository.getScrollPosition(topicId)
            _uiState.update { it.copy(lastReadPostNumber = lastRead) }
            android.util.Log.d("ScrollPos", "Init: lastRead=$lastRead")
            
            _uiState.update { it.copy(isLoading = true) }
            
            // Load the topic - this gives us initial posts + stream of all post IDs
            val result = topicRepository.getTopicDetail(slug, topicId, page = 1)
            result.fold(
                onSuccess = { detail ->
                    val initialPosts = detail.postStream?.posts?.toMutableList() ?: mutableListOf()
                    val stream = detail.postStream?.stream ?: emptyList()
                    val totalPosts = detail.postsCount ?: 0
                    
                    android.util.Log.d("ScrollPos", "Initial load: ${initialPosts.size} posts, stream has ${stream.size} IDs, lastRead=$lastRead")
                    
                    // Check if we need more posts for scrolling to saved position
                    val maxPostNumber = initialPosts.maxOfOrNull { it.postNumber } ?: 0
                    val minPostNumber = initialPosts.minOfOrNull { it.postNumber } ?: 1
                    val needsEarlierPosts = minPostNumber > 1
                    val needsLaterPosts = lastRead > 0 && maxPostNumber < lastRead
                    
                    if ((needsEarlierPosts || needsLaterPosts) && stream.isNotEmpty()) {
                        android.util.Log.d("ScrollPos", "Need more posts: earlier=$needsEarlierPosts (min=$minPostNumber), later=$needsLaterPosts (max=$maxPostNumber, target=$lastRead)")
                        
                        val loadedIds = initialPosts.map { it.id }.toSet()
                        
                        // Find post IDs we need to load:
                        // 1. All earlier posts (for scrolling up)
                        // 2. Posts up to lastRead + 20 (for scrolling down after position restore)
                        val targetIndex = if (lastRead > 0 && lastRead <= stream.size) {
                            (lastRead + 20).coerceAtMost(stream.size)  // Load 20 extra for scrolling down
                        } else {
                            stream.size
                        }
                        val neededIds = stream.take(targetIndex).filter { it !in loadedIds }
                        
                        android.util.Log.d("ScrollPos", "Need to load ${neededIds.size} posts to reach target position")
                        
                        // Load needed posts in chunks of 20
                        for (chunk in neededIds.chunked(20)) {
                            android.util.Log.d("ScrollPos", "Loading ${chunk.size} posts by ID")
                            topicRepository.getPostsByIds(topicId, chunk).fold(
                                onSuccess = { posts ->
                                    posts.forEach { post ->
                                        if (initialPosts.none { it.id == post.id }) {
                                            initialPosts.add(post)
                                        }
                                    }
                                    android.util.Log.d("ScrollPos", "Loaded ${posts.size} posts, total now ${initialPosts.size}")
                                },
                                onFailure = { error ->
                                    android.util.Log.e("ScrollPos", "Failed to load chunk: ${error.message}")
                                }
                            )
                        }
                    }

                    // Record History
                    val topicForHistory = Topic(
                        id = detail.id,
                        title = detail.title,
                        slug = detail.slug,
                        categoryId = detail.categoryId,
                        postsCount = detail.postsCount,
                        replyCount = detail.replyCount,
                        views = detail.views,
                        likeCount = detail.likeCount,
                        createdAt = detail.createdAt,
                        // Other fields are optional or not available in detail response directly
                        lastPosterUsername = detail.postStream.posts.lastOrNull()?.username
                    )
                    viewModelScope.launch {
                        historyRepository.addHistory(topicForHistory)
                    }


                    
                    // Sort by post_number
                    initialPosts.sortBy { it.postNumber }
                    
                    // Extract bookmark state properly during initial load
                    val firstPost = initialPosts.asSequence().filter { it.postNumber == 1 }.firstOrNull()
                    val bookmarked = detail.bookmarked || (detail.details?.bookmarked == true) || (firstPost?.bookmarked == true) || (firstPost?.actionsSummary?.any { it.id == 3 && it.acted } == true)
                    val bookmarkId = detail.details?.bookmarks?.firstOrNull()?.id ?: detail.bookmarks.firstOrNull()?.id ?: firstPost?.bookmarkId
                    
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            topicDetail = detail,
                            posts = initialPosts.toList(),
                            currentPage = 1,
                            hasMorePages = initialPosts.isNotEmpty() && (initialPosts.lastOrNull()?.postNumber ?: 0) < totalPosts,
                            scrollToPostNumber = if (lastRead > 0) lastRead else 0,
                            isBookmarked = bookmarked,
                            bookmarkId = bookmarkId
                        )
                    }
                    android.util.Log.d("Bookmark", "Init parsed Topic bookmarked=$bookmarked, bookmarkId=$bookmarkId")
                    android.util.Log.d("ScrollPos", "Init complete: ${initialPosts.size} posts, scrollTo=$lastRead")
                },
                onFailure = { throwable ->
                    android.util.Log.e("ScrollPos", "Failed to load topic", throwable)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "加载失败"
                        )
                    }
                }
            )
        }
    }
    
    private fun loadLastReadPostNumber() {
        viewModelScope.launch {
            val lastRead = scrollPositionRepository.getScrollPosition(topicId)
            _uiState.update { it.copy(lastReadPostNumber = lastRead) }
        }
    }
    
    fun clearScrollToPostNumber() {
        _uiState.update { it.copy(scrollToPostNumber = 0) }
    }
    
    fun saveLastReadPostNumber(postNumber: Int) {
        viewModelScope.launch {
            android.util.Log.d("ScrollPos", "Saving postNumber=$postNumber")
            scrollPositionRepository.saveScrollPosition(topicId, postNumber)
        }
    }

    fun loadTopicDetail(refresh: Boolean = false) {
        if (refresh) {
            // On refresh, reload all pages up to current page and keep position
            val currentPage = _uiState.value.currentPage.coerceAtLeast(1)
            reloadAllPages(currentPage)
        } else {
            loadTopicDetailFromPage(1, refresh = false, clearScrollTo = false)
        }
    }
    
    // Called from Screen to set scroll position before refresh
    fun setScrollToPostNumber(postNumber: Int) {
        _uiState.update { it.copy(scrollToPostNumber = postNumber) }
    }
    
    private fun reloadAllPages(upToPage: Int) {
        if (_uiState.value.isRefreshing) return
        
        // Save current scroll position to restore after refresh
        val scrollToAfterRefresh = _uiState.value.scrollToPostNumber
        
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            
            val allPosts = mutableListOf<Post>()
            var detail: TopicDetailResponse? = null
            var totalPosts = 0
            
            for (page in 1..upToPage) {
                android.util.Log.d("ScrollPos", "Refresh loading page $page")
                val result = topicRepository.getTopicDetail(slug, topicId, page = page)
                result.fold(
                    onSuccess = { pageDetail ->
                        if (detail == null) {
                            detail = pageDetail
                            totalPosts = pageDetail.postsCount ?: 0
                        }
                        val pagePosts = pageDetail.postStream?.posts ?: emptyList()
                        pagePosts.forEach { post ->
                            if (allPosts.none { it.id == post.id }) {
                                allPosts.add(post)
                            }
                        }
                        android.util.Log.d("ScrollPos", "Refresh page $page: ${pagePosts.size} posts, total ${allPosts.size}")
                    },
                    onFailure = { throwable ->
                        _uiState.update {
                            it.copy(isRefreshing = false, error = throwable.message ?: "刷新失败")
                        }
                        return@launch
                    }
                )
            }
            
            allPosts.sortBy { it.postNumber }
            
            // Extract bookmark state properly during refresh
            val firstPost = allPosts.asSequence().filter { it.postNumber == 1 }.firstOrNull()
            val bookmarked = detail?.bookmarked == true || (detail?.details?.bookmarked == true) || (firstPost?.bookmarked == true) || (firstPost?.actionsSummary?.any { it.id == 3 && it.acted } == true)
            val bookmarkId = detail?.details?.bookmarks?.firstOrNull()?.id ?: detail?.bookmarks?.firstOrNull()?.id ?: firstPost?.bookmarkId
            
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    topicDetail = detail,
                    posts = allPosts.toList(),
                    currentPage = upToPage,
                    hasMorePages = allPosts.isNotEmpty() && (allPosts.lastOrNull()?.postNumber ?: 0) < totalPosts,
                    scrollToPostNumber = scrollToAfterRefresh, // Restore scroll position
                    isBookmarked = bookmarked,
                    bookmarkId = bookmarkId
                )
            }
            android.util.Log.d("Bookmark", "Refresh parsed Topic bookmarked=$bookmarked, bookmarkId=$bookmarkId")
            android.util.Log.d("ScrollPos", "Refresh complete: ${allPosts.size} posts, restoreTo=$scrollToAfterRefresh")
        }
    }
    
    private fun loadTopicDetailFromPage(page: Int, refresh: Boolean = false, clearScrollTo: Boolean = false) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !refresh && it.topicDetail == null,
                    isRefreshing = refresh,
                    error = null,
                    currentPage = page,
                    scrollToPostNumber = if (clearScrollTo) 0 else it.scrollToPostNumber
                )
            }

            topicRepository.getTopicDetail(slug, topicId, page = page).fold(
                onSuccess = { detail ->
                    val posts = detail.postStream?.posts ?: emptyList()
                    val totalPosts = detail.postsCount ?: posts.size
                    
                    // Discourse represents a bookmark on the whole topic as a bookmark on the FIRST post.
                    val firstPost = posts.asSequence().filter { it.postNumber == 1 }.firstOrNull()
                    
                    // A topic is considered bookmarked if the user bookmarked the topic object itself OR the first post
                    val bookmarked = detail.bookmarked || (detail.details?.bookmarked == true) || (firstPost?.bookmarked == true) || (firstPost?.actionsSummary?.any { it.id == 3 && it.acted } == true)
                    
                    // Bookmark ID can be found in a few places in Discourse payloads
                    val bookmarkId = detail.details?.bookmarks?.firstOrNull()?.id 
                        ?: detail.bookmarks.firstOrNull()?.id 
                        ?: firstPost?.bookmarkId
                    
                    // Clear the bookmark result because we just loaded fresh truth
                    if (!refresh) {
                        _bookmarkResult.value = null
                    }
                    
                    android.util.Log.d("Bookmark", "Topic bookmarked=$bookmarked, bookmarkId=$bookmarkId")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            topicDetail = detail,
                            posts = posts,
                            currentPage = page,
                            hasMorePages = posts.isNotEmpty() && (posts.lastOrNull()?.postNumber ?: 0) < totalPosts,
                            isBookmarked = bookmarked,
                            bookmarkId = bookmarkId
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = throwable.message ?: "加载失败"
                        )
                    }
                }
            )
        }
    }

    fun loadMorePosts() {
        val state = _uiState.value
        // Guard against race condition: don't load more if still loading initial posts
        if (state.isLoadingMore || !state.hasMorePages || state.isLoading || state.posts.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            val nextPage = state.currentPage + 1
            topicRepository.getTopicDetail(slug, topicId, page = nextPage).fold(
                onSuccess = { detail ->
                    val newPosts = detail.postStream?.posts ?: emptyList()
                    val totalPosts = detail.postsCount ?: (state.posts.size + newPosts.size)
                    val allPosts = state.posts + newPosts.filter { new -> 
                        state.posts.none { existing -> existing.id == new.id }
                    }
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            posts = allPosts,
                            currentPage = nextPage,
                            hasMorePages = allPosts.size < totalPosts && newPosts.isNotEmpty()
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            error = throwable.message ?: "加载更多失败"
                        )
                    }
                }
            )
        }
    }

    fun refresh() {
        // Calculate page based on loaded posts (20 posts per page)
        val loadedPostCount = _uiState.value.posts.size
        val pageFromPosts = ((loadedPostCount + 19) / 20).coerceAtLeast(1)
        // Also ensure we load at least one more page to get new posts
        val targetPage = (pageFromPosts + 1).coerceAtLeast(_uiState.value.currentPage + 1)
        reloadAllPages(targetPage)
    }
    
    fun refreshWithPosition(scrollToPostNumber: Int) {
        // Set position first, then refresh
        _uiState.update { it.copy(scrollToPostNumber = scrollToPostNumber) }
        android.util.Log.d("ScrollPos", "refreshWithPosition: setting scrollTo=$scrollToPostNumber")
        loadTopicDetail(refresh = true)
    }
    
    // Like result for UI feedback
    private val _likeResult = MutableStateFlow<LikeResult?>(null)
    val likeResult: StateFlow<LikeResult?> = _likeResult.asStateFlow()
    
    fun likePost(postId: Int) {
        // Optimistic update
        val currentPosts = _uiState.value.posts
        val postIndex = currentPosts.indexOfFirst { it.id == postId }
        if (postIndex == -1) return
        
        val post = currentPosts[postIndex]
        val isLiked = post.isLiked
        val currentCount = post.likeCount
        
        // Calculate new state
        val newIsLiked = !isLiked
        val newCount = if (newIsLiked) currentCount + 1 else (currentCount - 1).coerceAtLeast(0)
        
        // Update local state immediately
        updatePostLikeState(postId, newIsLiked, newCount)
        
        viewModelScope.launch {
            val result = if (newIsLiked) {
                topicRepository.likePost(postId)
            } else {
                topicRepository.unlikePost(postId)
            }
            
            result.fold(
                onSuccess = {
                    // Success - state already updated, just show brief feedback
                    // _likeResult.value = LikeResult.Success(if (newIsLiked) "点赞成功 ❤️" else "取消点赞")
                    // kotlinx.coroutines.delay(500)
                    // _likeResult.value = null
                },
                onFailure = { e ->
                    // Revert state on failure
                    updatePostLikeState(postId, isLiked, currentCount)
                    
                    val message = e.message ?: "操作失败"
                    if (message.contains("登录")) {
                        _likeResult.value = LikeResult.LoginRequired
                    } else {
                        _likeResult.value = LikeResult.Error(message)
                        kotlinx.coroutines.delay(1000)
                        _likeResult.value = null
                    }
                }
            )
        }
    }
    
    private fun updatePostLikeState(postId: Int, isLiked: Boolean, count: Int) {
        _uiState.update { state ->
            val updatedPosts = state.posts.map { post ->
                if (post.id == postId) {
                    // We need to modify actionsSummary to reflect the new state
                    // logic: find action id=2 (like) and update it, or add it if missing
                    val newActions = post.actionsSummary.toMutableList()
                    val actionIndex = newActions.indexOfFirst { it.id == 2 }
                    
                    if (actionIndex >= 0) {
                        newActions[actionIndex] = newActions[actionIndex].copy(
                            acted = isLiked,
                            count = count,
                            canAct = !isLiked // usually if liked, canAct becomes false for "like" action depending on API, but for toggle usually we toggle acted
                        )
                    } else {
                        // Action not present, add it
                        // This case is rare for posts that allow liking but have 0 likes
                         // We'd need ActionSummary definition here. assuming it's available
                        // import io.github.xhuohai.sudo.data.model.ActionSummary
                        // For now, if missing and we are liking, we might not be able to add it easily without imports 
                        // But usually actionsSummary is present.
                    }
                    
                    post.copy(actionsSummary = newActions)
                } else {
                    post
                }
            }
            state.copy(posts = updatedPosts)
        }
    }
    
    fun clearLikeResult() {
        _likeResult.value = null
    }
    
    // Reply functionality
    private val _replyState = MutableStateFlow<ReplyState>(ReplyState.Idle)
    val replyState: StateFlow<ReplyState> = _replyState.asStateFlow()
    
    fun sendReply(content: String, replyToPostNumber: Int? = null) {
        val topicId = this.topicId
        
        if (content.isBlank()) {
            _replyState.value = ReplyState.Error("回复内容不能为空")
            return
        }
        
        if (content.length < 5) {
            _replyState.value = ReplyState.Error("回复内容太短，至少需要5个字符")
            return
        }
        
        _replyState.value = ReplyState.Sending
        
        viewModelScope.launch {
            val result = topicRepository.createPost(
                topicId = topicId,
                content = content,
                replyToPostNumber = replyToPostNumber
            )
            
            result.fold(
                onSuccess = { response ->
                    val postNumber = response.postNumber ?: 0
                    val postId = response.id
                    
                    if (postId != null) {
                        // Optimistically fetch the new post to update UI immediately
                        val fetchResult = topicRepository.getPostsByIds(topicId, listOf(postId))
                        
                        fetchResult.fold(
                            onSuccess = { posts ->
                                val newPost = posts.firstOrNull()
                                if (newPost != null) {
                                    _uiState.update { state ->
                                        // Check if post already exists to avoid duplicates
                                        if (state.posts.any { it.id == newPost.id }) {
                                            state
                                        } else {
                                            val updatedPosts = (state.posts + newPost).sortedBy { it.postNumber }
                                            state.copy(
                                                posts = updatedPosts,
                                                scrollToPostNumber = newPost.postNumber, // Scroll to new post
                                                topicDetail = state.topicDetail?.let { detail ->
                                                    detail.copy(
                                                        postsCount = (detail.postsCount ?: 0) + 1,
                                                        replyCount = (detail.replyCount ?: 0) + 1
                                                    )
                                                }
                                            )
                                        }
                                    }
                                    _replyState.value = ReplyState.Success(postNumber)
                                } else {
                                    _replyState.value = ReplyState.Success(postNumber)
                                    refresh()
                                }
                            },
                            onFailure = {
                                // Fallback to full refresh if fetch fails
                                _replyState.value = ReplyState.Success(postNumber)
                                refresh()
                            }
                        )
                    } else {
                        _replyState.value = ReplyState.Success(postNumber)
                        refresh()
                    }
                },
                onFailure = { exception ->
                    _replyState.value = ReplyState.Error(exception.message ?: "发送失败")
                }
            )
        }
    }
    
    fun clearReplyState() {
        _replyState.value = ReplyState.Idle
    }
    
    // Bookmark state
    private val _bookmarkResult = MutableStateFlow<BookmarkResult?>(null)
    val bookmarkResult: StateFlow<BookmarkResult?> = _bookmarkResult.asStateFlow()
    
    fun toggleBookmark(postId: Int) {
        val currentState = _uiState.value
        viewModelScope.launch {
            if (currentState.isBookmarked && currentState.bookmarkId != null) {
                // Unbookmark
                topicRepository.unbookmarkPost(currentState.bookmarkId).fold(
                    onSuccess = {
                        _uiState.update { it.copy(isBookmarked = false, bookmarkId = null) }
                        _bookmarkResult.value = BookmarkResult.Removed
                    },
                    onFailure = { exception ->
                        _bookmarkResult.value = BookmarkResult.Error(exception.message ?: "取消收藏失败")
                    }
                )
            } else if (!currentState.isBookmarked) {
                // Bookmark
                topicRepository.bookmarkPost(postId).fold(
                    onSuccess = { bookmarkId ->
                        _uiState.update { it.copy(isBookmarked = true, bookmarkId = bookmarkId) }
                        _bookmarkResult.value = BookmarkResult.Added
                    },
                    onFailure = { exception ->
                        val msg = exception.message ?: ""
                        if (msg.contains("登录") == true) {
                            _bookmarkResult.value = BookmarkResult.LoginRequired
                        } else if (msg.contains("already") || msg.contains("exists") || msg.contains("重复") || msg.contains("400") || msg.contains("422")) {
                            // API says already exists (400/422).
                            // The easiest way to recover is to just visually set it to Added,
                            // and reload the topic to fetch the true bookmark ID natively.
                            _bookmarkResult.value = null // clear any error
                            _uiState.update { it.copy(isBookmarked = true) } 
                            loadTopicDetail(refresh = true)
                        } else {
                            _bookmarkResult.value = BookmarkResult.Error(msg)
                        }
                    }
                )
            }
        }
    }
    fun clearBookmarkResult() {
        _bookmarkResult.value = null
    }
}

sealed class LikeResult {
    data class Success(val message: String) : LikeResult()
    data class Error(val message: String) : LikeResult()
    object LoginRequired : LikeResult()
}

sealed class ReplyState {
    object Idle : ReplyState()
    object Sending : ReplyState()
    data class Success(val postNumber: Int) : ReplyState()
    data class Error(val message: String) : ReplyState()
}

sealed class BookmarkResult {
    object Added : BookmarkResult()
    object Removed : BookmarkResult()
    object LoginRequired : BookmarkResult()
    data class Error(val message: String) : BookmarkResult()
}


