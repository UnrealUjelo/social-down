package com.socialdown.app.core.extractor

import com.socialdown.app.core.common.FormatNormalizer
import com.socialdown.app.core.model.AudioFormat
import com.socialdown.app.core.model.MediaInfo
import com.socialdown.app.core.model.VideoFormat
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import com.yausername.youtubedl_android.mapper.VideoFormat as RawFormat

@Singleton
class YtDlpMediaExtractor @Inject constructor(
    private val runtime: YtDlpRuntime,
) : MediaExtractor {
    override suspend fun analyze(url: String): Result<MediaInfo> = withContext(Dispatchers.IO) {
        runCatching {
            runtime.ensureInitialized()
            val request = YoutubeDLRequest(url)
                .addOption("--no-playlist")
                .addOption("--no-warnings")
            val raw = YoutubeDL.getInstance().getInfo(request)
            val formats = raw.formats.orEmpty()
            val videoFormats = FormatNormalizer.video(
                formats.filter { it.hasVideo() }.map { it.toVideoFormat() },
            )
            val audioFormats = FormatNormalizer.audio(
                formats.filter { it.hasAudio() }.map { it.toAudioFormat() },
            )
            require(videoFormats.isNotEmpty() || audioFormats.isNotEmpty()) {
                "No downloadable media found"
            }
            MediaInfo(
                id = raw.id?.takeIf(String::isNotBlank) ?: UUID.randomUUID().toString(),
                title = raw.title?.takeIf(String::isNotBlank) ?: "Untitled media",
                thumbnailUrl = raw.thumbnail,
                durationSeconds = raw.duration.toLong().takeIf { it > 0 },
                source = raw.uploader?.takeIf(String::isNotBlank) ?: raw.extractor,
                sourceUrl = raw.webpageUrl ?: url,
                videoFormats = videoFormats,
                audioFormats = audioFormats,
            )
        }
    }

    private fun RawFormat.hasVideo() = height > 0 && !vcodec.equals("none", ignoreCase = true)

    private fun RawFormat.hasAudio() = !acodec.isNullOrBlank() &&
        !acodec.equals("none", ignoreCase = true)

    private fun RawFormat.toVideoFormat() = VideoFormat(
        formatId = formatId.orEmpty(),
        width = width.takeIf { it > 0 },
        height = height.takeIf { it > 0 },
        fps = fps.toDouble().takeIf { it > 0 },
        codec = vcodec,
        container = ext,
        bitrateKbps = tbr.toDouble().takeIf { it > 0 },
        fileSizeBytes = bestFileSize(),
        hasAudio = hasAudio(),
        hdr = formatNote?.contains("HDR", ignoreCase = true) == true,
    )

    private fun RawFormat.toAudioFormat() = AudioFormat(
        formatId = formatId.orEmpty(),
        codec = acodec,
        container = ext,
        bitrateKbps = abr.toDouble().takeIf { it > 0 },
        sampleRateHz = asr.takeIf { it > 0 },
        channels = null,
        fileSizeBytes = bestFileSize(),
    )

    private fun RawFormat.bestFileSize(): Long? = when {
        fileSize > 0 -> fileSize
        fileSizeApproximate > 0 -> fileSizeApproximate
        else -> null
    }
}
