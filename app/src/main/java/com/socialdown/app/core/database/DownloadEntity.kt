package com.socialdown.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.socialdown.app.core.model.DownloadRequest
import com.socialdown.app.core.model.DownloadStatus
import com.socialdown.app.core.model.MediaKind

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val mediaId: String,
    val sourceUrl: String,
    val title: String,
    val thumbnailUrl: String?,
    val kind: MediaKind,
    val qualityLabel: String,
    val formatLabel: String,
    val status: DownloadStatus,
    val progressPercent: Int,
    val bytesDownloaded: Long?,
    val totalBytes: Long?,
    val speedBytesPerSecond: Long?,
    val etaSeconds: Long?,
    val outputUri: String?,
    val errorMessage: String?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    companion object {
        fun queued(request: DownloadRequest, now: Long = System.currentTimeMillis()) = DownloadEntity(
            id = request.id,
            mediaId = request.mediaId,
            sourceUrl = request.sourceUrl,
            title = request.title,
            thumbnailUrl = request.thumbnailUrl,
            kind = request.kind,
            qualityLabel = request.videoHeight?.let { "${it}p" }.orEmpty(),
            formatLabel = when (request.kind) {
                MediaKind.VIDEO -> request.videoContainer?.uppercase() ?: "MP4"
                MediaKind.AUDIO -> request.audioOutput.label
            },
            status = DownloadStatus.QUEUED,
            progressPercent = 0,
            bytesDownloaded = 0,
            totalBytes = request.estimatedSizeBytes,
            speedBytesPerSecond = null,
            etaSeconds = null,
            outputUri = null,
            errorMessage = null,
            createdAt = now,
            updatedAt = now,
        )
    }
}
