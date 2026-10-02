package com.socialdown.app.core.model

data class MediaInfo(
    val id: String,
    val title: String,
    val thumbnailUrl: String?,
    val durationSeconds: Long?,
    val source: String?,
    val sourceUrl: String,
    val videoFormats: List<VideoFormat>,
    val audioFormats: List<AudioFormat>,
)

data class VideoFormat(
    val formatId: String,
    val width: Int?,
    val height: Int?,
    val fps: Double?,
    val codec: String?,
    val container: String?,
    val bitrateKbps: Double?,
    val fileSizeBytes: Long?,
    val hasAudio: Boolean,
    val hdr: Boolean,
    val availableContainers: List<String> = emptyList(),
)

data class AudioFormat(
    val formatId: String,
    val codec: String?,
    val container: String?,
    val bitrateKbps: Double?,
    val sampleRateHz: Int?,
    val channels: Int?,
    val fileSizeBytes: Long?,
)

enum class MediaKind { VIDEO, AUDIO }

enum class AudioOutput(val extension: String, val label: String) {
    ORIGINAL("", "Original"),
    MP3("mp3", "MP3"),
    M4A("m4a", "M4A"),
    FLAC("flac", "FLAC"),
    WAV("wav", "WAV"),
}

enum class DownloadStatus {
    QUEUED,
    ANALYZING,
    DOWNLOADING,
    MERGING,
    CONVERTING,
    COMPLETED,
    FAILED,
    CANCELED,
}

data class DownloadRequest(
    val id: String,
    val mediaId: String,
    val sourceUrl: String,
    val title: String,
    val thumbnailUrl: String?,
    val kind: MediaKind,
    val videoHeight: Int? = null,
    val videoContainer: String? = null,
    val audioOutput: AudioOutput = AudioOutput.ORIGINAL,
    val audioBitrateKbps: Int? = null,
    val estimatedSizeBytes: Long? = null,
)

data class DownloadProgress(
    val id: String,
    val status: DownloadStatus,
    val progressPercent: Int,
    val bytesDownloaded: Long? = null,
    val totalBytes: Long? = null,
    val speedBytesPerSecond: Long? = null,
    val etaSeconds: Long? = null,
)
