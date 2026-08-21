package com.example.checkpoint.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * Repository dedicato alla gestione delle preferenze utente tramite DataStore.
 */
open class UserPreferencesRepository(private val context: Context?) {

    private object PreferencesKeys {
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
    }

    open val isLoggedIn: Flow<Boolean> = context?.dataStore?.data?.map { preferences ->
        preferences[PreferencesKeys.IS_LOGGED_IN] ?: false
    } ?: emptyFlow()

    open val userEmail: Flow<String?> = context?.dataStore?.data?.map { preferences ->
        preferences[PreferencesKeys.USER_EMAIL]
    } ?: emptyFlow()

    open val isDarkTheme: Flow<Boolean?> = context?.dataStore?.data?.map { preferences ->
        preferences[PreferencesKeys.IS_DARK_THEME]
    } ?: emptyFlow()

    open suspend fun saveSession(email: String) {
        context?.dataStore?.edit { preferences ->
            preferences[PreferencesKeys.IS_LOGGED_IN] = true
            preferences[PreferencesKeys.USER_EMAIL] = email
        }
    }

    open suspend fun setTheme(isDark: Boolean) {
        context?.dataStore?.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_THEME] = isDark
        }
    }

    open suspend fun clearSession() {
        context?.dataStore?.edit { preferences ->
            preferences[PreferencesKeys.IS_LOGGED_IN] = false
            preferences[PreferencesKeys.USER_EMAIL] = ""
        }
    }
}
