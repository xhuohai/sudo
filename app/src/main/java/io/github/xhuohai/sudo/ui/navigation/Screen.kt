package io.github.xhuohai.sudo.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Home : Screen
    
    @Serializable
    data object Categories : Screen
    
    @Serializable
    data object Messages : Screen
    
    @Serializable
    data object Profile : Screen
    
    @Serializable
    data object MyBookmarks : Screen
    
    @Serializable
    data object History : Screen
    
    @Serializable
    data class TopicDetail(val topicId: Int, val slug: String) : Screen
    
    @Serializable
    data class CategoryTopics(val categoryId: Int, val slug: String) : Screen
    
    @Serializable
    data class UserProfile(val username: String) : Screen
    
    @Serializable
    data object Login : Screen
    
    @Serializable
    data object Settings : Screen
    
    @Serializable
    data object CreateTopic : Screen

    @Serializable
    data object CreatePM : Screen
    
    @Serializable
    data class WebView(val url: String, val title: String = "") : Screen
    
    @Serializable
    data object Search : Screen
}
