package com.socialdown.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialdown.app.core.preferences.PreferencesRepository
import com.socialdown.app.core.preferences.ThemeMode
import com.socialdown.app.core.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: PreferencesRepository,
) : ViewModel() {
    val preferences: StateFlow<UserPreferences> = repository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { repository.setTheme(mode) }
    fun setVideoHeight(height: Int) = viewModelScope.launch {
        repository.setDefaultVideoHeight(height)
    }
    fun setAudioFormat(format: String) = viewModelScope.launch {
        repository.setDefaultAudioFormat(format)
    }
}
