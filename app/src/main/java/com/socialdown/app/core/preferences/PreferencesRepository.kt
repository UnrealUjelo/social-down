package com.socialdown.app.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultVideoHeight: Int = 1080,
    val defaultAudioFormat: String = "Original",
)

interface PreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun setTheme(mode: ThemeMode)
    suspend fun setDefaultVideoHeight(height: Int)
    suspend fun setDefaultAudioFormat(format: String)
}

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class DataStorePreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : PreferencesRepository {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val videoHeight = intPreferencesKey("default_video_height")
        val audioFormat = stringPreferencesKey("default_audio_format")
    }

    override val preferences: Flow<UserPreferences> = context.dataStore.data.map { values ->
        UserPreferences(
            themeMode = values[Keys.theme]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            defaultVideoHeight = values[Keys.videoHeight] ?: 1080,
            defaultAudioFormat = values[Keys.audioFormat] ?: "Original",
        )
    }

    override suspend fun setTheme(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.theme] = mode.name }
    }

    override suspend fun setDefaultVideoHeight(height: Int) {
        context.dataStore.edit { it[Keys.videoHeight] = height }
    }

    override suspend fun setDefaultAudioFormat(format: String) {
        context.dataStore.edit { it[Keys.audioFormat] = format }
    }
}
