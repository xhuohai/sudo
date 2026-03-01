package io.github.xhuohai.sudo.ui.settings

import androidx.lifecycle.ViewModel
import io.github.xhuohai.sudo.data.repository.AuthRepository
import io.github.xhuohai.sudo.data.repository.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    val themeMode: Flow<ThemeMode> = authRepository.themeMode
    
    suspend fun setThemeMode(mode: ThemeMode) {
        authRepository.setThemeMode(mode)
    }
}
