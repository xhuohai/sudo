package io.github.xhuohai.sudo.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.xhuohai.sudo.data.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import io.github.xhuohai.sudo.data.remote.CreatePostResponse

data class CreatePMUiState(
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null,
    val successTopicId: Int? = null,
    val successTopicSlug: String? = null
)

@HiltViewModel
class CreatePMViewModel @Inject constructor(
    private val topicRepository: TopicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePMUiState())
    val uiState: StateFlow<CreatePMUiState> = _uiState.asStateFlow()

    fun createPrivateMessage(title: String, raw: String, targetRecipients: String) {
        if (title.isBlank() || raw.isBlank() || targetRecipients.isBlank()) {
            _uiState.update { it.copy(error = "标题、内容和收件人不能为空") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, error = null) }
            
            val result = topicRepository.createPrivateMessage(title, raw, targetRecipients)
            
            result.fold(
                onSuccess = { response: CreatePostResponse ->
                    if (response.success == true || response.id != null) {
                        val topicId = response.topicId ?: response.id
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
                            it.copy(isSending = false, error = "发送失败: ${response.errors?.joinToString()}") 
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(isSending = false, error = error.message ?: "发送失败") 
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
