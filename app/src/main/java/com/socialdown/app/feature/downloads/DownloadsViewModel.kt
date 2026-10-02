package com.socialdown.app.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialdown.app.core.database.DownloadEntity
import com.socialdown.app.core.downloader.MediaDownloader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloader: MediaDownloader,
) : ViewModel() {
    val downloads: StateFlow<List<DownloadEntity>> = downloader.observeDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun cancel(id: String) = viewModelScope.launch { downloader.cancel(id) }
    fun delete(id: String) = viewModelScope.launch { downloader.delete(id) }
}
