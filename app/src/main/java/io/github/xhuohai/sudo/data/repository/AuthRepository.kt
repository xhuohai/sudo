package io.github.xhuohai.sudo.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferenceKeys {
        val COOKIE = stringPreferencesKey("auth_cookie")
        val USERNAME = stringPreferencesKey("username")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val AVATAR_URL = stringPreferencesKey("avatar_url")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferenceKeys.IS_LOGGED_IN] ?: false
        }

    val cookie: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferenceKeys.COOKIE]
        }

    val username: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferenceKeys.USERNAME]
        }

    val avatarUrl: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferenceKeys.AVATAR_URL]
        }
    
    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map { preferences ->
            val value = preferences[PreferenceKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            ThemeMode.valueOf(value)
        }

    suspend fun saveLoginInfo(cookie: String, username: String, avatarUrl: String?) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.COOKIE] = cookie
            preferences[PreferenceKeys.USERNAME] = username
            preferences[PreferenceKeys.IS_LOGGED_IN] = true
            avatarUrl?.let { preferences[PreferenceKeys.AVATAR_URL] = it }
        }
    }

    suspend fun logout() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferenceKeys.COOKIE)
            preferences.remove(PreferenceKeys.USERNAME)
            preferences.remove(PreferenceKeys.AVATAR_URL)
            preferences[PreferenceKeys.IS_LOGGED_IN] = false
        }
    }
    
    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun getCookieSync(): String? {
        var result: String? = null
        context.dataStore.data.collect { preferences ->
            result = preferences[PreferenceKeys.COOKIE]
        }
        return result
    }
}
