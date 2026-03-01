package io.github.xhuohai.sudo.data.repository

import io.github.xhuohai.sudo.data.model.Category
import io.github.xhuohai.sudo.data.remote.DiscourseApi
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val api: DiscourseApi,
    private val authRepository: AuthRepository
) {
    // Simple in-memory cache
    private var cachedCategories: List<Category>? = null
    
    // Timestamp of last fetch could be added for invalidation, but for now simple null check is fine
    // Categories rarely change structure often enough to warrant complex cache in session.

    private suspend fun getCookie(): String? = authRepository.cookie.first()

    suspend fun getCategories(forceRefresh: Boolean = false): Result<List<Category>> {
        if (!forceRefresh && cachedCategories != null) {
            return Result.success(cachedCategories!!)
        }

        return try {
            val response = api.getCategories(cookie = getCookie())
            val categories = response.categoryList.categories
            cachedCategories = categories
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCategory(id: Int): Category? {
        if (cachedCategories == null) {
            val result = getCategories()
            if (result.isFailure) return null
        }
        return cachedCategories?.find { it.id == id }
    }
    
    suspend fun getCategoryBySlug(slug: String): Category? {
        if (cachedCategories == null) {
            val result = getCategories()
            if (result.isFailure) return null
        }
        return cachedCategories?.find { it.slug == slug }
    }
}
