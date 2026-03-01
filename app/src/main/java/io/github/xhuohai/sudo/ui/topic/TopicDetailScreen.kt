package io.github.xhuohai.sudo.ui.topic

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbUpOffAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import io.github.xhuohai.sudo.data.model.Post
import io.github.xhuohai.sudo.ui.components.ErrorView
import io.github.xhuohai.sudo.ui.components.LoadingView
import io.github.xhuohai.sudo.ui.theme.CornerRadius
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern
import io.github.xhuohai.sudo.utils.EmojiUtils
import io.github.xhuohai.sudo.ui.components.RichTextContent
import io.github.xhuohai.sudo.ui.components.UserProfileDialog
import io.github.xhuohai.sudo.ui.components.ImageViewerDialog
import io.github.xhuohai.sudo.ui.components.LoginPromptDialog
import io.github.xhuohai.sudo.ui.components.HtmlContent
import io.github.xhuohai.sudo.ui.components.ReplyEditorDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicDetailScreen(
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit = {},
    onUserProfileClick: (String) -> Unit = {},
    onTopicClick: (Int, String) -> Unit = { _, _ -> },
    viewModel: TopicDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val replyState by viewModel.replyState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    // Bookmark state is managed in uiState
    val listState = rememberLazyListState()
    
    // User profile dialog state
    var showUserProfileDialog by remember { mutableStateOf(false) }
    var selectedUsername by remember { mutableStateOf("") }
    var selectedDisplayName by remember { mutableStateOf<String?>(null) }
    var selectedAvatarUrl by remember { mutableStateOf<String?>(null) }
    
    // Login prompt dialog state
    var showLoginPromptDialog by remember { mutableStateOf(false) }
    
    // Image viewer dialog state
    var showImageViewer by remember { mutableStateOf(false) }
    var selectedImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedImageIndex by remember { mutableStateOf(0) }
    
    // Reply dialog state
    var showReplyDialog by remember { mutableStateOf(false) }
    var replyToPostNumber by remember { mutableStateOf<Int?>(null) }
    var replyToUsername by remember { mutableStateOf<String?>(null) }
    var replyToContent by remember { mutableStateOf<String?>(null) }
    
    // Last read post number for showing divider
    val lastReadPostNumber = uiState.lastReadPostNumber
    
    // Scroll to saved position after loading
    val scrollToPostNumber = uiState.scrollToPostNumber
    LaunchedEffect(scrollToPostNumber, uiState.posts.size, uiState.isRefreshing) {
        // Only scroll after loading completes and for saved positions
        if (scrollToPostNumber > 0 && uiState.posts.isNotEmpty() && !uiState.isRefreshing && !uiState.isLoading) {
            val postIndex = uiState.posts.indexOfFirst { it.postNumber >= scrollToPostNumber }
            if (postIndex >= 0) {
                val listIndex = postIndex + 1
                android.util.Log.d("ScrollPos", "Scrolling to postNumber=$scrollToPostNumber, listIndex=$listIndex")
                listState.scrollToItem(listIndex)
                viewModel.clearScrollToPostNumber()
            }
        }
    }
    
    // Save position and navigate back on back press
    BackHandler {
        val posts = uiState.posts
        val lastVisibleIndex = listState.firstVisibleItemIndex
        android.util.Log.d("ScrollPos", "BackHandler: lastVisibleIndex=$lastVisibleIndex, posts.size=${posts.size}")
        if (posts.isNotEmpty() && lastVisibleIndex >= 0) {
            val postIndex = if (lastVisibleIndex == 0) 0 else (lastVisibleIndex - 1).coerceIn(0, posts.size - 1)
            val lastVisiblePost = posts[postIndex]
            android.util.Log.d("ScrollPos", "BackHandler saving postNumber=${lastVisiblePost.postNumber}")
            viewModel.saveLastReadPostNumber(lastVisiblePost.postNumber)
        }
        onBackClick()
    }
    
    // Auto load more when scrolling near bottom - trigger earlier for seamless experience
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            // Trigger when 10 items from bottom for preloading
            lastVisibleItem >= totalItems - 10 && !uiState.isLoadingMore && uiState.hasMorePages
        }
    }
    
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadMorePosts()
        }
    }
    
    // Observe like result
    val likeResult by viewModel.likeResult.collectAsState()
    LaunchedEffect(likeResult) {
        likeResult?.let { result ->
            when (result) {
                is LikeResult.Success -> snackbarHostState.showSnackbar(result.message)
                is LikeResult.Error -> snackbarHostState.showSnackbar(result.message)
                is LikeResult.LoginRequired -> showLoginPromptDialog = true
            }
            if (result !is LikeResult.LoginRequired) {
                viewModel.clearLikeResult()
            }
        }
    }
    
    // Observe bookmark result
    val bookmarkResult by viewModel.bookmarkResult.collectAsState()
    LaunchedEffect(bookmarkResult) {
        bookmarkResult?.let { result ->
            snackbarHostState.currentSnackbarData?.dismiss() // Dismiss existing snackbar to prevent queuing
            when (result) {
                is BookmarkResult.Added -> snackbarHostState.showSnackbar(message = "已收藏", duration = androidx.compose.material3.SnackbarDuration.Short)
                is BookmarkResult.Removed -> snackbarHostState.showSnackbar(message = "已取消收藏", duration = androidx.compose.material3.SnackbarDuration.Short)
                is BookmarkResult.Error -> snackbarHostState.showSnackbar(message = result.message, duration = androidx.compose.material3.SnackbarDuration.Short)
                is BookmarkResult.LoginRequired -> showLoginPromptDialog = true
            }
            viewModel.clearBookmarkResult()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.topicDetail?.title ?: "帖子详情",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val topicId = uiState.topicDetail?.id ?: return@IconButton
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, uiState.topicDetail?.title ?: "")
                            putExtra(Intent.EXTRA_TEXT, "https://linux.do/t/${uiState.topicDetail?.slug}/$topicId")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "分享帖子"))
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "分享"
                        )
                    }
                    IconButton(onClick = {
                        // Bookmark the first post (OP's post) to bookmark the topic
                        val firstPostId = uiState.posts.firstOrNull()?.id
                        if (firstPostId != null) {
                            viewModel.toggleBookmark(firstPostId)
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("请等待帖子加载完成")
                            }
                        }
                    }) {
                        Icon(
                            imageVector = if (uiState.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "收藏"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    replyToPostNumber = null
                    showReplyDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Reply,
                    contentDescription = "回复"
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingView()
                }
                uiState.error != null && uiState.posts.isEmpty() -> {
                    ErrorView(
                        message = uiState.error ?: "加载失败",
                        onRetry = { viewModel.loadTopicDetail() }
                    )
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = {
                            // Just refresh without position handling - let LazyColumn maintain natural position
                            viewModel.refresh()
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Topic header
                            uiState.topicDetail?.let { detail ->
                                item {
                                    TopicHeader(
                                        title = detail.title,
                                        viewCount = detail.views,
                                        replyCount = (detail.postsCount - 1).coerceAtLeast(0),
                                        likeCount = detail.likeCount
                                    )
                                }
                            }

                            // Posts
                            itemsIndexed(
                                items = uiState.posts,
                                key = { _, post -> post.id }
                            ) { index, post ->
                                // Show "上次浏览到这里" divider before the first unread post
                                if (lastReadPostNumber > 0 && 
                                    index > 0 && 
                                    post.postNumber > lastReadPostNumber &&
                                    (index == 0 || uiState.posts[index - 1].postNumber <= lastReadPostNumber)) {
                                    LastReadDivider()
                                }
                                
                                PostCard(
                                    post = post,
                                    isOriginalPost = post.postNumber == 1,
                                    floorNumber = post.postNumber,
                                    onUserClick = { username ->
                                        // Find post author info
                                        val clickedPost = uiState.posts.find { it.username == username }
                                        selectedUsername = username
                                        selectedDisplayName = clickedPost?.name
                                        selectedAvatarUrl = clickedPost?.getAvatarUrl(120)
                                        showUserProfileDialog = true
                                    },
                                    onLikeClick = { postId ->
                                        viewModel.likePost(postId)
                                    },
                                    onReplyClick = {
                                        // Set reply info for quote
                                        replyToPostNumber = post.postNumber
                                        replyToUsername = post.username
                                        // Extract first 100 chars of plain text for preview
                                        val plainText = cleanHtmlContent(post.cooked)
                                        replyToContent = if (plainText.length > 100) {
                                            plainText.take(100) + "..."
                                        } else {
                                            plainText
                                        }
                                        showReplyDialog = true
                                    },
                                    onImageClick = { images, index ->
                                        selectedImages = images
                                        selectedImageIndex = index
                                        showImageViewer = true
                                    },
                                    onLinkClick = { url ->
                                        val topicPattern = Pattern.compile("https?://(?:www\\.)?linux\\.do/t/(?:[^/]+/)?(\\d+)(?:/.*)?")
                                        val matcher = topicPattern.matcher(url)
                                        if (matcher.matches()) {
                                            val topicId = matcher.group(1)?.toIntOrNull()
                                            if (topicId != null) {
                                                onTopicClick(topicId, "")
                                            } else {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        } else {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                )
                            }
                            
                            // Load more section
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (uiState.hasMorePages) {
                                        // Auto-loading - just show a subtle loading indicator
                                        if (uiState.isLoadingMore) {
                                            androidx.compose.material3.CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    } else {
                                        // No more pages - show end message and refresh button
                                        Text(
                                            text = "— 已加载 ${uiState.posts.size} 楼 —",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    // Always show refresh button to load newer posts
                                    if (!uiState.isLoadingMore) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        androidx.compose.material3.TextButton(
                                            onClick = { viewModel.refresh() }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "刷新",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("刷新查看新回复")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // User Profile Dialog
    if (showUserProfileDialog) {
        UserProfileDialog(
            username = selectedUsername,
            displayName = selectedDisplayName,
            avatarUrl = selectedAvatarUrl,
            onDismiss = { showUserProfileDialog = false },
            onViewProfile = { onUserProfileClick(selectedUsername) }
        )
    }
    
    // Login Prompt Dialog
    if (showLoginPromptDialog) {
        LoginPromptDialog(
            onDismiss = { 
                showLoginPromptDialog = false
                viewModel.clearLikeResult()
            },
            onLoginClick = {
                showLoginPromptDialog = false
                viewModel.clearLikeResult()
                onLoginClick()
            }
        )
    }
    
    // Image Viewer Dialog
    if (showImageViewer && selectedImages.isNotEmpty()) {
        ImageViewerDialog(
            images = selectedImages,
            initialIndex = selectedImageIndex,
            onDismiss = { showImageViewer = false }
        )
    }
    
    // Reply Editor Dialog
    if (showReplyDialog) {
        ReplyEditorDialog(
            topicTitle = uiState.topicDetail?.title ?: "",
            replyToPostNumber = replyToPostNumber,
            replyToUsername = replyToUsername,
            replyToContent = replyToContent,
            isSending = replyState is ReplyState.Sending,
            onDismiss = { 
                showReplyDialog = false
                replyToPostNumber = null
                replyToUsername = null
                replyToContent = null
                viewModel.clearReplyState()
            },
            onSend = { content ->
                viewModel.sendReply(content, replyToPostNumber)
            }
        )
    }
    
    // Handle reply state changes
    LaunchedEffect(replyState) {
        when (val state = replyState) {
            is ReplyState.Success -> {
                showReplyDialog = false
                snackbarHostState.showSnackbar("回复成功！楼层 #${state.postNumber}")
                viewModel.clearReplyState()
            }
            is ReplyState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearReplyState()
            }
            else -> {}
        }
    }
}

@Composable
private fun TopicHeader(
    title: String,
    viewCount: Int,
    replyCount: Int,
    likeCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CornerRadius.Card),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SelectionContainer {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                StatText(label = "浏览", value = viewCount)
                StatText(label = "回复", value = replyCount)
                StatText(label = "点赞", value = likeCount)
            }
        }
    }
}

@Composable
private fun StatText(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = formatNumber(value),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun LastReadDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Text(
            text = "  上次浏览到这里  ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun PostCard(
    post: Post,
    isOriginalPost: Boolean,
    floorNumber: Int,
    onUserClick: (String) -> Unit,
    onLikeClick: (Int) -> Unit,
    onReplyClick: () -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
    onLinkClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CornerRadius.Card),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Author header
            Row(
                modifier = Modifier.clickable { onUserClick(post.username) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = post.getAvatarUrl(90),
                    contentDescription = "头像",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onUserClick(post.username) },
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.name ?: post.username,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        if (isOriginalPost) {
                            UserBadge(text = "楼主", containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                        }
                        
                        if (post.admin) {
                            UserBadge(text = "管理员", containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                        } else if (post.moderator) {
                            UserBadge(text = "版主", containerColor = MaterialTheme.colorScheme.tertiary, contentColor = MaterialTheme.colorScheme.onTertiary)
                        }
                        
                        if (!post.userTitle.isNullOrBlank()) {
                             UserBadge(
                                text = post.userTitle, 
                                containerColor = MaterialTheme.colorScheme.surfaceVariant, 
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = formatDateTime(post.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Reply-to info
                    if (post.replyToPostNumber != null && post.replyToPostNumber > 0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Reply,
                                contentDescription = "回复",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "#${post.replyToPostNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                
                // Floor number
                Text(
                    text = "#${floorNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            

            // Content with inline images - use HtmlContent for proper layout
            SelectionContainer {
                HtmlContent(
                    html = post.cooked,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    textColor = MaterialTheme.colorScheme.onSurface,
                    onImageClick = onImageClick,
                    onLinkClick = onLinkClick
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                PostActionButton(
                    icon = Icons.Default.ThumbUpOffAlt,
                    count = post.actionsSummary?.find { it.id == 2 }?.count ?: 0,
                    label = "点赞",
                    onClick = { onLikeClick(post.id) }
                )
                PostActionButton(
                    icon = Icons.AutoMirrored.Filled.Reply,
                    count = post.replyCount,
                    label = "回复",
                    onClick = onReplyClick
                )
            }
        }
    }
}

/**
 * Clean HTML content to plain text, preserving emoji as unicode text
 */
private fun cleanHtmlContent(html: String): String {
    var text = html
    
    // Remove quote aside blocks entirely (they are displayed separately in QuoteBlock)
    // Format: <aside class="quote" ...>.....</aside>
    text = text.replace(Regex("""<aside[^>]*class="[^"]*quote[^"]*"[^>]*>[\s\S]*?</aside>"""), "")
    
    // First: Replace emoji images with their alt text (emoji unicode)
    // This must happen BEFORE removing other images
    text = text.replace(Regex("""<img[^>]*class="emoji[^"]*"[^>]*alt="([^"]+)"[^>]*>""")) { matchResult ->
        matchResult.groupValues[1]
    }
    text = text.replace(Regex("""<img[^>]*alt="([^"]+)"[^>]*class="emoji[^"]*"[^>]*>""")) { matchResult ->
        matchResult.groupValues[1]
    }
    
    // Remove user avatar images (in mentions/links)
    text = text.replace(Regex("""<img[^>]*class="avatar[^"]*"[^>]*>"""), "")
    
    // Remove lightbox links completely with all their content (image dimension info)
    // Format: <a class="lightbox" ...>100%x100% 1227x662 58.2KB</a>
    text = text.replace(Regex("""<a[^>]*class="[^"]*lightbox[^"]*"[^>]*>[\s\S]*?</a>"""), "")
    
    // Remove image dimension metadata patterns (common Discourse format)
    text = text.replace(Regex("""\d+%?x\d+%?\s*\d+x\d+\s*[\d.]+[KMG]?B""", RegexOption.IGNORE_CASE), "")
    text = text.replace(Regex("""\d+x\d+\s*[\d.]+\s*[KMG]B""", RegexOption.IGNORE_CASE), "")
    
    // Remove other images (they are shown separately)
    text = text.replace(Regex("""<img[^>]*>"""), "")
    
    // Handle user mentions - keep the @username
    text = text.replace(Regex("""<a[^>]*class="mention[^"]*"[^>]*>(@[^<]+)</a>""")) { matchResult ->
        matchResult.groupValues[1]
    }
    
    // Keep links with their text and URL for later processing
    // Format: [linktext](url)
    text = text.replace(Regex("""<a[^>]*href="([^"]+)"[^>]*>([^<]*)</a>""")) { matchResult ->
        val url = matchResult.groupValues[1]
        val linkText = matchResult.groupValues[2]
        if (linkText.isNotBlank() && !url.contains("user-card") && !linkText.contains("@")) {
            "[$linkText]($url)"
        } else {
            linkText
        }
    }
    
    // Convert <br> and </p> to newlines
    text = text.replace(Regex("""<br\s*/?>"""), "\n")
    text = text.replace(Regex("""</p>"""), "\n")
    text = text.replace(Regex("""<p[^>]*>"""), "")
    
    // Handle blockquotes
    text = text.replace(Regex("""<blockquote[^>]*>"""), "\n> ")
    text = text.replace(Regex("""</blockquote>"""), "\n")
    
    // Remove code blocks formatting but keep content
    text = text.replace(Regex("""<pre[^>]*><code[^>]*>"""), "\n```\n")
    text = text.replace(Regex("""</code></pre>"""), "\n```\n")
    text = text.replace(Regex("""<code[^>]*>"""), "`")
    text = text.replace(Regex("""</code>"""), "`")
    
    // Handle lists
    text = text.replace(Regex("""<li[^>]*>"""), "• ")
    text = text.replace(Regex("""</li>"""), "\n")
    text = text.replace(Regex("""<[ou]l[^>]*>"""), "\n")
    text = text.replace(Regex("""</[ou]l>"""), "\n")
    
    // Bold and italic
    text = text.replace(Regex("""<strong[^>]*>"""), "**")
    text = text.replace(Regex("""</strong>"""), "**")
    text = text.replace(Regex("""<em[^>]*>"""), "*")
    text = text.replace(Regex("""</em>"""), "*")
    
    // Remove all other HTML tags
    text = text.replace(Regex("""<[^>]+>"""), "")
    
    // Decode HTML entities
    text = text.replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&#x27;", "'")
        .replace("&hellip;", "…")
    
    // Convert Discourse emoji shortcodes to Unicode
    text = EmojiUtils.convertShortcodes(text)
    
    // Clean up extra whitespace
    text = text.replace(Regex("""\n{3,}"""), "\n\n")
    text = text.replace(Regex("""[ \t]+"""), " ")
    
    return text.trim()
}


private data class ImageInfo(
    val thumbnailUrl: String,
    val originalUrl: String
)

private fun extractImagesFromHtml(html: String): List<ImageInfo> {
    val images = mutableListOf<ImageInfo>()
    
    // Step 1: Build a map of lightbox href URLs (these are original full-size images)
    // Lightbox format: <a class="lightbox" href="originalUrl">...</a>
    val lightboxOriginals = mutableMapOf<String, String>()  // thumbnail -> original
    
    // Extract all lightbox hrefs
    val lightboxHrefPattern = Pattern.compile("""<a[^>]*class="[^"]*lightbox[^"]*"[^>]*href="([^"]+)"[^>]*>""")
    val lightboxHrefMatcher = lightboxHrefPattern.matcher(html)
    val lightboxHrefs = mutableListOf<String>()
    while (lightboxHrefMatcher.find()) {
        lightboxHrefMatcher.group(1)?.let { lightboxHrefs.add(it) }
    }
    
    // Also try alternate attribute order
    val lightboxHrefPattern2 = Pattern.compile("""<a[^>]*href="([^"]+)"[^>]*class="[^"]*lightbox[^"]*"[^>]*>""")
    val lightboxHrefMatcher2 = lightboxHrefPattern2.matcher(html)
    while (lightboxHrefMatcher2.find()) {
        lightboxHrefMatcher2.group(1)?.let { if (it !in lightboxHrefs) lightboxHrefs.add(it) }
    }
    
    // Step 2: Extract standard img tags with src attribute
    val imgPattern = Pattern.compile("""<img[^>]+src="([^"]+)"[^>]*>""")
    val imgMatcher = imgPattern.matcher(html)
    
    var lightboxIndex = 0
    while (imgMatcher.find()) {
        val imgTag = imgMatcher.group(0) ?: continue
        val src = imgMatcher.group(1) ?: continue
        
        // Skip emoji images
        if (imgTag.contains("class=\"emoji") || imgTag.contains("class='emoji")) {
            continue
        }
        
        // Skip avatar images
        if (imgTag.contains("class=\"avatar") || imgTag.contains("class='avatar")) {
            continue
        }
        
        // Skip very small images (likely icons)
        if (imgTag.contains("width=\"20\"") || imgTag.contains("height=\"20\"") ||
            imgTag.contains("width='20'") || imgTag.contains("height='20'")) {
            continue
        }
        
        // Skip placeholder/loading images
        if (src.contains("transparent.png") || src.contains("placeholder")) {
            continue
        }
        
        // Skip emoji URLs
        if (src.contains("/images/emoji/")) {
            continue
        }
        
        // Check if this image has a corresponding lightbox (use index-based matching)
        val originalUrl = if (lightboxIndex < lightboxHrefs.size) {
            lightboxHrefs[lightboxIndex++]
        } else {
            src  // Fallback to src if no lightbox
        }
        
        // Check if already added
        val alreadyAdded = images.any { it.thumbnailUrl == src }
        if (!alreadyAdded) {
            images.add(ImageInfo(thumbnailUrl = src, originalUrl = originalUrl))
        }
    }
    
    return images.distinctBy { it.thumbnailUrl }
}



@Composable
private fun PostActionButton(
    icon: ImageVector,
    count: Int,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (count > 0) count.toString() else label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatNumber(num: Int): String {
    return when {
        num >= 10000 -> String.format("%.1fW", num / 10000.0)
        num >= 1000 -> String.format("%.1fK", num / 1000.0)
        else -> num.toString()
    }
}

private fun formatDateTime(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val outputFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        outputFormat.timeZone = TimeZone.getDefault()
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString.take(16).replace("T", " ")
    } catch (e: Exception) {
        try {
            dateString.take(16).replace("T", " ")
        } catch (e2: Exception) {
            dateString
        }
    }
}

@Composable
fun UserBadge(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Spacer(modifier = Modifier.width(8.dp))
    Box(
        modifier = Modifier
            .background(containerColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}
