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

    fun saveLoginInfo(cookie: String, username: String, avatarUrl: String) {
        val finalAvatarUrl = avatarUrl.replace("{size}", "120")
            .let { if (it.isNotBlank() && it.startsWith("http")) it else if (it.isNotBlank()) "https://linux.do\$it" else null }
        
        viewModelScope.launch {
            authRepository.saveLoginInfo(cookie, username, finalAvatarUrl)
        }
    }
}
