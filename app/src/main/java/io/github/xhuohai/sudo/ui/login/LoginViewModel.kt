package io.github.xhuohai.sudo.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.xhuohai.sudo.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

import io.github.xhuohai.sudo.data.remote.DiscourseApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val api: DiscourseApi
) : ViewModel() {

    fun saveLoginInfo(cookie: String, fallbackUsername: String, unusedAvatar: String) {
        viewModelScope.launch {
            val (finalUsername, avatarUrl) = withContext(Dispatchers.IO) {
                try {
                    val response = api.getCurrentSession(cookie)
                    if (response.isSuccessful) {
                        val user = response.body()?.currentUser
                        if (user != null) {
                            val finalAvatarUrl = user.avatarTemplate?.replace("{size}", "120")
                                ?.let { if (it.startsWith("http")) it else "https://linux.do\$it" }
                            return@withContext Pair(user.username, finalAvatarUrl)
                        }
                    }
                    Pair(fallbackUsername, null)
                } catch (e: Exception) {
                    Pair(fallbackUsername, null)
                }
            }
            
            authRepository.saveLoginInfo(cookie, finalUsername, avatarUrl)
        }
    }
}
