package com.socialdown.app.core.media

import com.socialdown.app.core.model.AudioOutput

data class MediaProcessingPlan(
    val formatSelector: String,
    val mergeContainer: String? = null,
    val extractAudio: Boolean = false,
    val audioFormat: String? = null,
    val audioQuality: String? = null,
)

interface MediaProcessor {
    fun videoPlan(height: Int, container: String): MediaProcessingPlan
    fun audioPlan(output: AudioOutput, bitrateKbps: Int?): MediaProcessingPlan
}
