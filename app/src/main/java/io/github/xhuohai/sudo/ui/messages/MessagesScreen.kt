package io.github.xhuohai.sudo.ui.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import io.github.xhuohai.sudo.data.model.Topic
import io.github.xhuohai.sudo.ui.components.EmptyListView
import io.github.xhuohai.sudo.ui.theme.CornerRadius
import io.github.xhuohai.sudo.utils.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    onTopicClick: (Int, String) -> Unit,
    onCreatePMClick: () -> Unit,
    viewModel: MessagesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "消息",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },

                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreatePMClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "发私信")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Private Messages
                if (!uiState.isLoggedIn) {
                    EmptyListView(
                        icon = Icons.Default.NotificationsNone,
                        title = "登录后查看私信",
                        description = "登录后可以查看私信列表"
                    )
                } else if (uiState.isLoading && uiState.topics.isEmpty()) {
                    EmptyListView(
                        icon = Icons.Default.Notifications,
                        title = "加载中...",
                        description = "正在获取私信"
                    )
                } else if (uiState.topics.isEmpty()) {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        EmptyListView(
                            icon = Icons.Default.NotificationsNone,
                            title = "暂无私信",
                            description = "您还没有收到任何私信"
                        )
                    }
                } else {
                    val listState = rememberLazyListState()
                    val isAtBottom by remember {
                        derivedStateOf {
                            val layoutInfo = listState.layoutInfo
                            val visibleItemsInfo = layoutInfo.visibleItemsInfo
                            if (layoutInfo.totalItemsCount == 0) {
                                false
                            } else {
                                val lastVisibleItem = visibleItemsInfo.last()
                                val viewportHeight = layoutInfo.viewportEndOffset + layoutInfo.viewportStartOffset
                                (lastVisibleItem.index + 1 == layoutInfo.totalItemsCount) &&
                                        (lastVisibleItem.offset + lastVisibleItem.size <= viewportHeight)
                            }
                        }
                    }

                    LaunchedEffect(isAtBottom) {
                        if (isAtBottom) {
                            viewModel.loadMore()
                        }
                    }

                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.topics) { topic ->
                                PrivateMessageCard(topic = topic, onClick = { onTopicClick(topic.id, topic.slug) })
                            }
                            
                            if (uiState.isLoading && uiState.topics.isNotEmpty()) {
                                item { 
                                     Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalArrangement = Arrangement.Center
                                     ) {
                                         Text("加载更多...", style = MaterialTheme.typography.bodySmall)
                                     }
                                }
                            }
                        }
                    }
                }
        }
    }
}

@Composable
fun PrivateMessageCard(topic: Topic, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(CornerRadius.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Participants Avatars (Show up to 3)
            val participants = topic.participants.take(3)
            if (participants.isNotEmpty()) {
               Box(modifier = Modifier.width(50.dp)) {
                   participants.forEachIndexed { index, participant ->
                       AsyncImage(
                           model = participant.getAvatarUrl(80),
                           contentDescription = participant.username,
                           modifier = Modifier
                               .size(32.dp)
                               .padding(start = (index * 15).dp)
                               .clip(CircleShape)
                               .align(Alignment.TopStart)
                       )
                   }
               }
            } else {
                // Fallback avatar if no participants field (shouldn't happen for PMs usually)
                 AsyncImage(
                    model = "https://linux.do/user_avatar/linux.do/${topic.lastPosterUsername ?: "system"}/120/1.png", // Simplified
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = topic.participants.joinToString(", ") { it.username },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = TimeUtils.getTimeAgo(topic.lastPostedAt ?: topic.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = topic.excerpt ?: "无预览",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
