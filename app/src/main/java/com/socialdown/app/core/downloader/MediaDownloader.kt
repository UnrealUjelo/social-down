package com.socialdown.app.core.downloader

import com.socialdown.app.core.database.DownloadEntity
import com.socialdown.app.core.model.DownloadRequest
import kotlinx.coroutines.flow.Flow

interface MediaDownloader {
    fun observeDownloads(): Flow<List<DownloadEntity>>
    suspend fun enqueue(request: DownloadRequest)
    suspend fun cancel(id: String)
    suspend fun delete(id: String)
}
