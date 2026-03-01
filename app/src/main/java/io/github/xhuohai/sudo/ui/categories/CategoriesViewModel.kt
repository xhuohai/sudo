package io.github.xhuohai.sudo.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.xhuohai.sudo.data.model.Category
import io.github.xhuohai.sudo.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val categories: List<Category> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories(refresh: Boolean = false) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !refresh && it.categories.isEmpty(),
                    isRefreshing = refresh,
                    error = null
                )
            }

            categoryRepository.getCategories(forceRefresh = refresh).fold(
                onSuccess = { categories ->
                    // Filter out subcategories for top-level display
                    val topLevelCategories = categories.filter { it.parentCategoryId == null }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            categories = topLevelCategories
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

    fun refresh() {
        loadCategories(refresh = true)
    }
}
