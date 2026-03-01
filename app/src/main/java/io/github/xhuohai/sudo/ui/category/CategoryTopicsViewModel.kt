package io.github.xhuohai.sudo.ui.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.xhuohai.sudo.data.model.Category
import io.github.xhuohai.sudo.data.model.Topic
import io.github.xhuohai.sudo.data.model.User
import io.github.xhuohai.sudo.data.repository.CategoryRepository
import io.github.xhuohai.sudo.data.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryTopicsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val categoryName: String = "",
    val category: Category? = null,
    val topics: List<Topic> = emptyList(),
    val users: List<User> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class CategoryTopicsViewModel @Inject constructor(
    private val topicRepository: TopicRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val categoryId: Int = savedStateHandle["categoryId"] ?: 0
    private val categorySlug: String = savedStateHandle["slug"] ?: ""

    private val _uiState = MutableStateFlow(CategoryTopicsUiState(categoryName = categorySlug))
    val uiState: StateFlow<CategoryTopicsUiState> = _uiState.asStateFlow()

    init {
        loadCategoryTopics()
        loadCategoryDetails()
    }

    private fun loadCategoryDetails() {
        viewModelScope.launch {
            val category = if (categoryId > 0) {
                categoryRepository.getCategory(categoryId)
            } else {
                categoryRepository.getCategoryBySlug(categorySlug)
            }
            
            if (category != null) {
                _uiState.update { 
                    it.copy(
                        category = category,
                        categoryName = category.name
                    ) 
                }
            }
        }
    }

    fun loadCategoryTopics(refresh: Boolean = false) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !refresh && it.topics.isEmpty(),
                    isRefreshing = refresh,
                    error = null
                )
            }

            topicRepository.getCategoryTopics(categorySlug, categoryId).fold(
                onSuccess = { (topics, users) ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            topics = topics,
                            users = users
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

    fun refresh() {
        loadCategoryTopics(refresh = true)
    }
}
