package com.dmax.nexus.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.nexusDataStore: DataStore<Preferences> by preferencesDataStore(name = "nexus_settings")

data class AppSettings(
    val theme: String = "Emerald",
    val aiEndpoint: String = "",
    val accessToken: String = "",
    val lowDataMode: Boolean = false
)

class AppPreferences(private val context: Context) {
    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val AI_ENDPOINT = stringPreferencesKey("ai_endpoint")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val LOW_DATA_MODE = booleanPreferencesKey("low_data_mode")
    }

    val settings: Flow<AppSettings> = context.nexusDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            AppSettings(
                theme = prefs[Keys.THEME] ?: "Emerald",
                aiEndpoint = prefs[Keys.AI_ENDPOINT] ?: "",
                accessToken = prefs[Keys.ACCESS_TOKEN] ?: "",
                lowDataMode = prefs[Keys.LOW_DATA_MODE] ?: false
            )
        }

    suspend fun setTheme(value: String) {
        context.nexusDataStore.edit { it[Keys.THEME] = value }
    }

    suspend fun setAiEndpoint(value: String) {
        context.nexusDataStore.edit { it[Keys.AI_ENDPOINT] = value.trim() }
    }

    suspend fun setAccessToken(value: String) {
        context.nexusDataStore.edit { it[Keys.ACCESS_TOKEN] = value.trim() }
    }

    suspend fun setLowDataMode(value: Boolean) {
        context.nexusDataStore.edit { it[Keys.LOW_DATA_MODE] = value }
    }
}
