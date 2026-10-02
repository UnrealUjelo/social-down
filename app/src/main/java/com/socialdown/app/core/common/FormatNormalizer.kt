package com.socialdown.app.core.common

import com.socialdown.app.core.model.AudioFormat
import com.socialdown.app.core.model.VideoFormat

object FormatNormalizer {
    private val preferredContainers = listOf("mp4", "webm", "mkv")

    fun video(formats: List<VideoFormat>): List<VideoFormat> = formats
        .filter { (it.height ?: 0) > 0 && !it.codec.equals("none", ignoreCase = true) }
        .groupBy { it.height }
        .mapNotNull { (_, candidates) ->
            val selected = candidates.maxWithOrNull(
                compareBy<VideoFormat> { containerScore(it.container) }
                    .thenBy { if (it.hasAudio) 1 else 0 }
                    .thenBy { it.bitrateKbps ?: 0.0 }
                    .thenBy { it.fileSizeBytes ?: 0L },
            ) ?: return@mapNotNull null
            selected.copy(
                availableContainers = preferredContainers.filter { preferred ->
                    candidates.any { it.container.equals(preferred, ignoreCase = true) }
                },
            )
        }
        .sortedByDescending { it.height }

    fun audio(formats: List<AudioFormat>): List<AudioFormat> = formats
        .filter { !it.codec.equals("none", ignoreCase = true) }
        .distinctBy { Triple(it.codec, it.container, it.bitrateKbps?.toInt()) }
        .sortedByDescending { it.bitrateKbps ?: 0.0 }

    fun defaultVideoHeight(formats: List<VideoFormat>): Int? {
        val heights = formats.mapNotNull { it.height }.distinct().sortedDescending()
        return heights.firstOrNull { it <= 1080 }
    }

    fun availableContainers(formats: List<VideoFormat>, height: Int?): List<String> {
        if (height == null) return emptyList()
        val found = formats
            .filter { it.height == height }
            .flatMap { format ->
                format.availableContainers.ifEmpty {
                    listOfNotNull(format.container?.lowercase())
                }
            }
            .filter { it in preferredContainers }
            .distinct()
        return preferredContainers.filter(found::contains)
            .ifEmpty { listOf("mp4") }
    }

    fun defaultContainer(containers: List<String>): String? =
        containers.firstOrNull { it.equals("mp4", ignoreCase = true) }
            ?: containers.firstOrNull()

    private fun containerScore(container: String?): Int {
        val index = preferredContainers.indexOf(container?.lowercase())
        return if (index == -1) 0 else preferredContainers.size - index
    }
}
