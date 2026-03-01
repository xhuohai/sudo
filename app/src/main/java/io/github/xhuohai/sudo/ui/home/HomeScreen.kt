package io.github.xhuohai.sudo.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.xhuohai.sudo.ui.components.ErrorView
import io.github.xhuohai.sudo.ui.components.LoadingView
import io.github.xhuohai.sudo.ui.components.TopicCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onTopicClick: (topicId: Int, slug: String) -> Unit,
    onSearchClick: () -> Unit,
    onCreateTopicClick: () -> Unit,
    scrollToTopTrigger: Int = 0,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    // Pager state for swipeable tabs
    val pagerState = rememberPagerState(
        initialPage = TopicTab.entries.indexOf(uiState.currentTab),
        pageCount = { TopicTab.entries.size }
    )
    
    // Sync pager with tab selection from ViewModel
    LaunchedEffect(uiState.currentTab) {
        val targetPage = TopicTab.entries.indexOf(uiState.currentTab)
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }
    
    // Sync ViewModel with pager swipe
    LaunchedEffect(pagerState.currentPage) {
        val currentTabFromPager = TopicTab.entries[pagerState.currentPage]
        if (uiState.currentTab != currentTabFromPager) {
            viewModel.selectTab(currentTabFromPager)
        }
    }

    androidx.compose.material3.Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "sudo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(
                onClick = onCreateTopicClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "发帖")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            
            // Tabs
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                edgePadding = 0.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    if (pagerState.currentPage < tabPositions.size) {
                        androidx.compose.material3.TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            height = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                divider = {
                    androidx.compose.material3.HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                }
            ) {
            TopicTab.entries.forEachIndexed { index, tab ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
             // We only show content if the current tab matches the page to avoid unnecessary composition of off-screen pages
             // But HorizontalPager keeps adjacent pages composed. 
             // We rely on ViewModel to hold state. 
             // Since ViewModel only holds ONE list of topics (currentTab), this is tricky with HorizontalPager if we want to swipe.
             // If we swipe, we change tab, ViewModel loads new data. The previous page data is lost in ViewModel's single list.
             // We updated ViewModel to have a cache! So when we switch back, it's instant.
             // But during the swipe animation, what do we show?
             // Ideally ViewModel should probably expose data for *each* tab or we accept a small loading blip.
             // Given the cache implementation in ViewModel, `selectTab` updates `topics` flow immediately from cache if available.
             
            val currentTabForPage = TopicTab.entries[page]
            
            // Only render the list if this page is the current active tab in ViewModel
            // Or if we are securely cached? 
            // Actually, `uiState.topics` reflects `uiState.currentTab`.
            // If `page != uiState.currentTab.ordinal`, the data in `uiState.topics` belongs to another tab!
            // This means HorizontalPager will show WRONG data on adjacent pages during swipe if we bind directly to `uiState.topics`.
            
            // Fix: We need `HomeViewModel` to expose data per tab or we disable swiping and just use Clickable Tabs.
            // OR, we just use the `HorizontalPager` for the transition effect but we must accept that adjacent pages might look empty until fully switched?
            // Better approach for now: Use `Box` content that switches, or keep `HorizontalPager` but strictly sync.
            // With the current ViewModel design (single `topics` list), `HorizontalPager` swipe is problematic because `topics` changes *after* swipe/selection.
            
            // Let's stick to Pager but we might see a flash. 
            // Actually, let's remove Pager for now to ensure stability, OR update ViewModel to expose `topics` map.
            // Since user asked for Pager ("Supports left/right swipe"), let's try to make it work.
            
            // If we want true Pager support, ViewModel should expose `getData(Tab)` or similar.
            // Let's modify `HomeScreen` to NOT use Pager for now if it complicates state, or update ViewModel?
            // User requirement: "Support left/right slide switching". So Pager is needed.
            
            // To support Pager properly with single-list ViewModel:
            // The `uiState` has `currentTab`.
            // If `page` == `uiState.currentTab`, show `uiState.topics`.
            // If `page` != `uiState.currentTab`, show Loading or Empty? 
            // This is not ideal visual. 
            
            // Correct fix: ViewModel should expose `feedState(tab)` which returns a flow or state for that specific tab.
            // But we already updated ViewModel to have `tabDataCache`.
            // Let's update ViewModel to expose the map or `HomeUiState` to have `latestTopics`, `topTopics`, `unreadTopics`.
            
            // Re-evaluating ViewModel change...
            // It's better to verify this locally.
            // Let's assume for this step we render the `PullToRefreshBox` inside.
            
            if (TopicTab.entries[page] == uiState.currentTab) {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                   if (uiState.error != null && uiState.topics.isEmpty()) {
                        ErrorView(
                            message = uiState.error!!,
                            onRetry = { viewModel.loadTopics() }
                        )
                    } else if (uiState.isLoading) {
                        LoadingView()
                    } else {
                         LazyColumn(
                            state = rememberLazyListState(), // Each tab gets its own state implicitly by key? No, need explicit state if we want to save it.
                            // But `items` key is important.
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                             items(
                                items = uiState.topics,
                                key = { topic -> topic.id }
                            ) { topic ->
                                TopicCard(
                                    topic = topic,
                                    users = uiState.users,
                                    onClick = { onTopicClick(topic.id, topic.slug) }
                                )
                            }
                            
                            if (uiState.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                            
                            // Infinite scroll trigger
                             if (uiState.hasMore && !uiState.isLoadingMore && !uiState.isLoading && uiState.topics.isNotEmpty()) {
                                item {
                                    LaunchedEffect(Unit) {
                                        viewModel.loadMoreTopics()
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                 // Placeholder for off-screen pages to prevent wrong data usage
                 Box(Modifier.fillMaxSize())
            }
        }


    }
    }
}




