package com.socialdown.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialdown.app.core.common.FormatNormalizer
import com.socialdown.app.core.common.UrlValidator
import com.socialdown.app.core.downloader.MediaDownloader
import com.socialdown.app.core.extractor.MediaExtractor
import com.socialdown.app.core.model.AudioOutput
import com.socialdown.app.core.model.DownloadRequest
import com.socialdown.app.core.model.MediaInfo
import com.socialdown.app.core.model.MediaKind
import com.socialdown.app.core.preferences.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface AnalysisState {
    data object Idle : AnalysisState
    data object Loading : AnalysisState
    data class Ready(val media: MediaInfo) : AnalysisState
    data class Error(val message: String) : AnalysisState
}

data class HomeUiState(
    val url: String = "",
    val analysis: AnalysisState = AnalysisState.Idle,
    val selectedKind: MediaKind = MediaKind.VIDEO,
    val selectedHeight: Int? = null,
    val selectedContainer: String? = null,
    val selectedAudioOutput: AudioOutput = AudioOutput.ORIGINAL,
    val selectedBitrate: Int = 192,
    val queuedMessage: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val extractor: MediaExtractor,
    private val downloader: MediaDownloader,
    private val preferences: PreferencesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun setUrl(value: String) {
        _state.update { current ->
            current.copy(
                url = value,
                analysis = if (current.analysis is AnalysisState.Error) AnalysisState.Idle else current.analysis,
            )
        }
    }

    fun acceptSharedUrl(value: String) {
        setUrl(value)
    }

    fun analyze() {
        val url = state.value.url.trim()
        if (!UrlValidator.isValid(url)) {
            _state.update { it.copy(analysis = AnalysisState.Error("Invalid link")) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(analysis = AnalysisState.Loading, queuedMessage = null) }
            extractor.analyze(url)
                .onSuccess { media -> applyMedia(media) }
                .onFailure { error ->
                    val message = if (error.message == "No downloadable media found") {
                        "No downloadable media found"
                    } else {
                        "Couldn't analyze this link"
                    }
                    _state.update { it.copy(analysis = AnalysisState.Error(message)) }
                }
        }
    }

    private suspend fun applyMedia(media: MediaInfo) {
        val userPreferences = preferences.preferences.first()
        val availableHeights = media.videoFormats.mapNotNull { it.height }
        val preferred = userPreferences.defaultVideoHeight.takeIf(availableHeights::contains)
            ?: FormatNormalizer.defaultVideoHeight(media.videoFormats)
        val containers = FormatNormalizer.availableContainers(media.videoFormats, preferred)
        val defaultKind = if (media.videoFormats.isNotEmpty()) MediaKind.VIDEO else MediaKind.AUDIO
        val defaultAudio = AudioOutput.entries.firstOrNull {
            it.label.equals(userPreferences.defaultAudioFormat, ignoreCase = true)
        } ?: AudioOutput.ORIGINAL
        _state.update {
            it.copy(
                analysis = AnalysisState.Ready(media),
                selectedKind = defaultKind,
                selectedHeight = preferred,
                selectedContainer = FormatNormalizer.defaultContainer(containers),
                selectedAudioOutput = defaultAudio,
            )
        }
    }

    fun selectKind(kind: MediaKind) = _state.update { it.copy(selectedKind = kind) }

    fun selectHeight(height: Int) {
        val media = (state.value.analysis as? AnalysisState.Ready)?.media ?: return
        val containers = FormatNormalizer.availableContainers(media.videoFormats, height)
        _state.update {
            it.copy(
                selectedHeight = height,
                selectedContainer = FormatNormalizer.defaultContainer(containers),
            )
        }
    }

    fun selectContainer(container: String) =
        _state.update { it.copy(selectedContainer = container) }

    fun selectAudioOutput(output: AudioOutput) =
        _state.update { it.copy(selectedAudioOutput = output) }

    fun selectBitrate(bitrate: Int) = _state.update { it.copy(selectedBitrate = bitrate) }

    fun enqueueDownload() {
        val current = state.value
        val media = (current.analysis as? AnalysisState.Ready)?.media ?: return
        viewModelScope.launch {
            val selectedVideo = media.videoFormats.firstOrNull { it.height == current.selectedHeight }
            downloader.enqueue(
                DownloadRequest(
                    id = UUID.randomUUID().toString(),
                    mediaId = media.id,
                    sourceUrl = media.sourceUrl,
                    title = media.title,
                    thumbnailUrl = media.thumbnailUrl,
                    kind = current.selectedKind,
                    videoHeight = current.selectedHeight,
                    videoContainer = current.selectedContainer,
                    audioOutput = current.selectedAudioOutput,
                    audioBitrateKbps = current.selectedBitrate,
                    estimatedSizeBytes = selectedVideo?.fileSizeBytes,
                ),
            )
            _state.update { it.copy(queuedMessage = "Added to downloads") }
        }
    }

    fun consumeQueuedMessage() = _state.update { it.copy(queuedMessage = null) }
}
