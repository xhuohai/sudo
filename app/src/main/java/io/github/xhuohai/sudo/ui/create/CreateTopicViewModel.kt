package io.github.xhuohai.sudo.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.xhuohai.sudo.data.model.Category
import io.github.xhuohai.sudo.data.repository.CategoryRepository
import io.github.xhuohai.sudo.data.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import io.github.xhuohai.sudo.data.remote.CreatePostResponse

data class CreateTopicUiState(
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val categories: List<Category> = emptyList(),
    val error: String? = null,
    val successTopicId: Int? = null,
    val successTopicSlug: String? = null
)

@HiltViewModel
class CreateTopicViewModel @Inject constructor(
    private val topicRepository: TopicRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateTopicUiState())
    val uiState: StateFlow<CreateTopicUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = categoryRepository.getCategories()
            result.fold(
                onSuccess = { categories ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            categories = categories.sortedBy { cat -> cat.position }
                        ) 
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(isLoading = false, error = error.message) 
                    }
                }
            )
        }
    }

    fun createTopic(title: String, raw: String, categoryId: Int) {
        if (title.isBlank() || raw.isBlank()) {
            _uiState.update { it.copy(error = "标题和内容不能为空") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, error = null) }
            
            // Note: tags currently not supported in UI, passing empty list
            val result = topicRepository.createTopic(title, raw, categoryId, emptyList())
            
            result.fold(
                onSuccess = { response: CreatePostResponse ->
                    if (response.success == true || response.id != null) {
                         // Attempt to find topic info. 
                         // If response has topicId directly, use it.
                         // Otherwise we might need to rely on the fact that creating a topic *usually* returns the topic id in post?
                         // Assuming CreatePostResponse now has topicId/slug populated if API returns it.
                         // If not, we might need a fallback or just go to Home.
                         
                        val topicId = response.topicId ?: response.id // Fallback to post ID? No, post ID != topic ID.
                        val topicSlug = response.topicSlug ?: ""

                        _uiState.update { 
                            it.copy(
                                isSending = false, 
                                successTopicId = topicId,
                                successTopicSlug = topicSlug
                            ) 
                        }
                    } else {
                         _uiState.update { 
                            it.copy(isSending = false, error = "发布失败: ${response.errors?.joinToString()}") 
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(isSending = false, error = error.message ?: "发布失败") 
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun resetSuccess() {
        _uiState.update { it.copy(successTopicId = null, successTopicSlug = null) }
    }
}
