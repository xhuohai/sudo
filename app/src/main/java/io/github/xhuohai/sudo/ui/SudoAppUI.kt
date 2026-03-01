package io.github.xhuohai.sudo.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.xhuohai.sudo.ui.categories.CategoriesScreen
import io.github.xhuohai.sudo.ui.category.CategoryTopicsScreen
import io.github.xhuohai.sudo.ui.create.CreateTopicScreen
import io.github.xhuohai.sudo.ui.home.HomeScreen
import io.github.xhuohai.sudo.ui.login.LoginScreen
import io.github.xhuohai.sudo.ui.messages.MessagesScreen
import io.github.xhuohai.sudo.ui.navigation.BottomNavItem
import io.github.xhuohai.sudo.ui.navigation.Screen
import io.github.xhuohai.sudo.ui.profile.ProfileScreen
import io.github.xhuohai.sudo.ui.profile.ProfileScreen
import io.github.xhuohai.sudo.ui.profile.bookmarks.MyBookmarksScreen
import io.github.xhuohai.sudo.ui.settings.SettingsScreen
import io.github.xhuohai.sudo.ui.topic.TopicDetailScreen
import io.github.xhuohai.sudo.ui.userprofile.UserProfileScreen
import io.github.xhuohai.sudo.ui.history.HistoryScreen
import androidx.navigation.toRoute

@Composable
fun SudoAppUI() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Determine if we should show bottom nav
    val showBottomBar = remember(currentDestination) {
        BottomNavItem.entries.any { item ->
            currentDestination?.hasRoute(item.route::class) == true
        }
    }
    
    // Double-tap scroll-to-top state
    var lastClickTime by remember { mutableLongStateOf(0L) }
    var lastClickedRoute by remember { mutableIntStateOf(-1) }
    var scrollToTopTrigger by remember { mutableIntStateOf(0) }  // Increment to trigger scroll

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BottomNavItem.entries.forEachIndexed { index, item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.hasRoute(item.route::class)
                        } == true

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                val currentTime = System.currentTimeMillis()
                                
                                // Check for double-tap on the same already-selected tab
                                if (selected && lastClickedRoute == index && 
                                    currentTime - lastClickTime < 300) {
                                    // Double-tap detected - trigger scroll to top
                                    scrollToTopTrigger++
                                } else if (!selected) {
                                    // Navigate to new tab
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                
                                lastClickTime = currentTime
                                lastClickedRoute = index
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable<Screen.Home> {
                HomeScreen(
                    onTopicClick = { topicId, slug ->
                        navController.navigate(Screen.TopicDetail(topicId, slug))
                    },
                    onSearchClick = { navController.navigate(Screen.Search) },
                    onCreateTopicClick = { navController.navigate(Screen.CreateTopic) },
                    scrollToTopTrigger = scrollToTopTrigger
                )
            }

            composable<Screen.Categories> {
                CategoriesScreen(
                    onCategoryClick = { categoryId, slug ->
                        navController.navigate(Screen.CategoryTopics(categoryId, slug))
                    }
                )
            }

            composable<Screen.Messages> {
                MessagesScreen(
                    onTopicClick = { topicId, slug ->
                        navController.navigate(Screen.TopicDetail(topicId, slug))
                    },
                    onCreatePMClick = {
                        navController.navigate(Screen.CreatePM)
                    }
                )
            }
            
            composable<Screen.CreatePM> {
                io.github.xhuohai.sudo.ui.create.CreatePMScreen(
                    onBackClick = { navController.popBackStack() },
                    onTopicCreated = { topicId: Int, slug: String ->
                        navController.popBackStack()
                        navController.navigate(Screen.TopicDetail(topicId, slug))
                    }
                )
            }

            composable<Screen.Profile> {
                ProfileScreen(
                    onLoginClick = {
                        navController.navigate(Screen.Login)
                    },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings)
                    },
                    onWebViewClick = { url, title ->
                        navController.navigate(Screen.WebView(url, title))
                    },
                    onMyBookmarksClick = {
                        navController.navigate(Screen.MyBookmarks)
                    },
                    onHistoryClick = {
                        navController.navigate(Screen.History)
                    },
                    onMessagesClick = {
                        navController.navigate(Screen.Messages) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable<Screen.History> {
                HistoryScreen(
                    onBackClick = { navController.popBackStack() },
                    onTopicClick = { topicId, slug ->
                        navController.navigate(Screen.TopicDetail(topicId, slug))
                    }
                )
            }

            composable<Screen.MyBookmarks> {
                MyBookmarksScreen(
                    onBackClick = { navController.popBackStack() },
                    onTopicClick = { topicId, slug ->
                        navController.navigate(Screen.TopicDetail(topicId, slug))
                    }
                )
            }

            composable<Screen.TopicDetail> {
                TopicDetailScreen(
                    onBackClick = { navController.popBackStack() },
                    onLoginClick = { navController.navigate(Screen.Login) },
                    onUserProfileClick = { username -> 
                        navController.navigate(Screen.UserProfile(username))
                    },
                    onTopicClick = { topicId, slug ->
                        navController.navigate(Screen.TopicDetail(topicId, slug))
                    }
                )
            }

            composable<Screen.CategoryTopics> {
                CategoryTopicsScreen(
                    onBackClick = { navController.popBackStack() },
                    onTopicClick = { topicId, slug ->
                        navController.navigate(Screen.TopicDetail(topicId, slug))
                    }
                )
            }

            composable<Screen.Login> {
                LoginScreen(
                    onLoginSuccess = {
                        navController.popBackStack()
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable<Screen.CreateTopic> {
                CreateTopicScreen(
                    onBackClick = { navController.popBackStack() },
                    onTopicCreated = { topicId: Int, slug: String ->
                        navController.popBackStack() // Remove CreateTopic
                        navController.navigate(Screen.TopicDetail(topicId, slug))
                    }
                )
            }

            composable<Screen.Settings> {
                SettingsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
            
            composable<Screen.UserProfile> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.UserProfile>()
                UserProfileScreen(
                    username = route.username,
                    onBackClick = { navController.popBackStack() }
                )
            }
            
            composable<Screen.WebView> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.WebView>()
                io.github.xhuohai.sudo.ui.webview.WebViewScreen(
                    url = route.url,
                    title = route.title,
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable<Screen.Search> {
                io.github.xhuohai.sudo.ui.search.SearchScreen(
                    onBackClick = { navController.popBackStack() },
                    onTopicClick = { topic ->
                        navController.navigate(Screen.TopicDetail(topic.id, topic.slug))
                    }
                )
            }
        }
    }
}

