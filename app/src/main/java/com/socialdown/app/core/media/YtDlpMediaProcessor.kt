package com.socialdown.app.core.media

import com.socialdown.app.core.model.AudioOutput
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpMediaProcessor @Inject constructor() : MediaProcessor {
    override fun videoPlan(height: Int, container: String): MediaProcessingPlan {
        val safeHeight = height.coerceAtLeast(144)
        val safeContainer = container.lowercase().takeIf { it in SUPPORTED_CONTAINERS } ?: "mp4"
        val selector = when (safeContainer) {
            "mp4" -> "bestvideo[height=$safeHeight][ext=mp4]+bestaudio[ext=m4a]/best[height=$safeHeight][ext=mp4]/bestvideo[height<=$safeHeight]+bestaudio/best[height<=$safeHeight]"
            "webm" -> "bestvideo[height=$safeHeight][ext=webm]+bestaudio[ext=webm]/best[height=$safeHeight][ext=webm]/bestvideo[height<=$safeHeight]+bestaudio/best[height<=$safeHeight]"
            else -> "bestvideo[height=$safeHeight]+bestaudio/best[height=$safeHeight]/bestvideo[height<=$safeHeight]+bestaudio/best[height<=$safeHeight]"
        }
        return MediaProcessingPlan(
            formatSelector = selector,
            mergeContainer = safeContainer,
        )
    }

    override fun audioPlan(output: AudioOutput, bitrateKbps: Int?): MediaProcessingPlan {
        if (output == AudioOutput.ORIGINAL) {
            return MediaProcessingPlan(formatSelector = "bestaudio/best")
        }
        val quality = if (output in setOf(AudioOutput.MP3, AudioOutput.M4A)) {
            val safeBitrate = bitrateKbps?.takeIf { it in SUPPORTED_BITRATES } ?: 192
            "${safeBitrate}K"
        } else {
            null
        }
        return MediaProcessingPlan(
            formatSelector = "bestaudio/best",
            extractAudio = true,
            audioFormat = output.extension,
            audioQuality = quality,
        )
    }

    private companion object {
        val SUPPORTED_CONTAINERS = setOf("mp4", "webm", "mkv")
        val SUPPORTED_BITRATES = setOf(128, 192, 256, 320)
    }
}
