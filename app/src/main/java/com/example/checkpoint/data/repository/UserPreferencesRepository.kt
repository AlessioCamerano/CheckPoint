package com.example.checkpoint.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * Interfaccia per la gestione delle preferenze utente.
 */
interface UserPreferencesRepository {
    val isLoggedIn: Flow<Boolean>
    val userEmail: Flow<String?>
    val isDarkTheme: Flow<Boolean?>
    val searchHistory: Flow<List<String>>
    suspend fun saveSession(email: String)
    suspend fun setTheme(isDark: Boolean)
    suspend fun saveSearchQuery(query: String)
    suspend fun clearSearchHistory()
    suspend fun clearSession()
}

/**
 * Implementazione delle preferenze utente con DataStore.
 */
class UserPreferencesRepositoryImpl(private val context: Context?) : UserPreferencesRepository {

    private object PreferencesKeys {
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val SEARCH_HISTORY = stringSetPreferencesKey("search_history")
    }

    override val isLoggedIn: Flow<Boolean> = context?.dataStore?.data?.map { preferences ->
        preferences[PreferencesKeys.IS_LOGGED_IN] ?: false
    } ?: emptyFlow()

    override val userEmail: Flow<String?> = context?.dataStore?.data?.map { preferences ->
        preferences[PreferencesKeys.USER_EMAIL]
    } ?: emptyFlow()

    override val isDarkTheme: Flow<Boolean?> = context?.dataStore?.data?.map { preferences ->
        preferences[PreferencesKeys.IS_DARK_THEME]
    } ?: emptyFlow()

    override val searchHistory: Flow<List<String>> = context?.dataStore?.data?.map { preferences ->
        preferences[PreferencesKeys.SEARCH_HISTORY]?.toList()?.sorted() ?: emptyList()
    } ?: emptyFlow()

    override suspend fun saveSession(email: String) {
        context?.dataStore?.edit { preferences ->
            preferences[PreferencesKeys.IS_LOGGED_IN] = true
            preferences[PreferencesKeys.USER_EMAIL] = email
        }
    }

    override suspend fun setTheme(isDark: Boolean) {
        context?.dataStore?.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_THEME] = isDark
        }
    }

    override suspend fun saveSearchQuery(query: String) {
        context?.dataStore?.edit { preferences ->
            val currentHistory = preferences[PreferencesKeys.SEARCH_HISTORY] ?: emptySet()
            val newHistory = currentHistory.toMutableSet()
            if (newHistory.contains(query)) {
                newHistory.remove(query)
            }
            newHistory.add(query)
            // Limit to 5 items
            if (newHistory.size > 5) {
                val oldest = newHistory.first()
                newHistory.remove(oldest)
            }
            preferences[PreferencesKeys.SEARCH_HISTORY] = newHistory
        }
    }

    override suspend fun clearSearchHistory() {
        context?.dataStore?.edit { preferences ->
            preferences.remove(PreferencesKeys.SEARCH_HISTORY)
        }
    }

    override suspend fun clearSession() {
        context?.dataStore?.edit { preferences ->
            preferences[PreferencesKeys.IS_LOGGED_IN] = false
            preferences[PreferencesKeys.USER_EMAIL] = ""
        }
    }
}
