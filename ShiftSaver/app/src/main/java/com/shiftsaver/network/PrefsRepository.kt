package com.shiftsaver.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shiftsaver_prefs")

class PrefsRepository(private val context: Context) {

    companion object {
        val KEY_HOST = stringPreferencesKey("server_host")
        val KEY_PORT = intPreferencesKey("server_port")
        val KEY_THEME = stringPreferencesKey("theme_choice")   // "md3" or "miuix"
        val KEY_DARK_MODE = stringPreferencesKey("dark_mode")  // "system", "light", "dark"

        const val DEFAULT_HOST = "192.168.1.100"
        const val DEFAULT_PORT = 5050
        const val DEFAULT_THEME = "md3"
        const val DEFAULT_DARK = "system"
    }

    val host: Flow<String> = context.dataStore.data.map { it[KEY_HOST] ?: DEFAULT_HOST }
    val port: Flow<Int> = context.dataStore.data.map { it[KEY_PORT] ?: DEFAULT_PORT }
    val theme: Flow<String> = context.dataStore.data.map { it[KEY_THEME] ?: DEFAULT_THEME }
    val darkMode: Flow<String> = context.dataStore.data.map { it[KEY_DARK_MODE] ?: DEFAULT_DARK }

    suspend fun saveServer(host: String, port: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HOST] = host
            prefs[KEY_PORT] = port
        }
    }

    suspend fun saveTheme(theme: String) {
        context.dataStore.edit { it[KEY_THEME] = theme }
    }

    suspend fun saveDarkMode(mode: String) {
        context.dataStore.edit { it[KEY_DARK_MODE] = mode }
    }
}
