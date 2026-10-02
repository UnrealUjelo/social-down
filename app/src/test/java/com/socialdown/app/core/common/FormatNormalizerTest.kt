package com.socialdown.app.core.common

import com.socialdown.app.core.model.VideoFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatNormalizerTest {
    @Test fun `deduplicates video qualities and sorts highest first`() {
        val normalized = FormatNormalizer.video(
            listOf(
                format("web-1080", 1080, "webm", false, 2_000.0),
                format("mp4-1080", 1080, "mp4", true, 1_800.0),
                format("mp4-720", 720, "mp4", true, 900.0),
                format("audio", null, "m4a", true, 128.0, codec = "none"),
            ),
        )

        assertEquals(listOf(1080, 720), normalized.map { it.height })
        assertEquals("mp4-1080", normalized.first().formatId)
    }

    @Test fun `defaults to 1080 or best sensible lower quality`() {
        assertEquals(1080, FormatNormalizer.defaultVideoHeight(listOf(format("a", 2160), format("b", 1080))))
        assertEquals(720, FormatNormalizer.defaultVideoHeight(listOf(format("a", 2160), format("b", 720))))
        assertEquals(null, FormatNormalizer.defaultVideoHeight(listOf(format("a", 2160))))
    }

    @Test fun `returns only valid ordered containers for selected quality`() {
        val containers = FormatNormalizer.availableContainers(
            listOf(
                format("a", 1080, "webm"),
                format("b", 1080, "mp4"),
                format("c", 720, "mkv"),
            ),
            1080,
        )
        assertEquals(listOf("mp4", "webm"), FormatNormalizer.availableContainers(FormatNormalizer.video(listOf(
            format("a", 1080, "webm"),
            format("b", 1080, "mp4"),
        )), 1080))
        assertEquals(listOf("mp4", "webm"), containers)
        assertEquals("mp4", FormatNormalizer.defaultContainer(containers))
    }

    @Test fun `falls back to compatible mp4 container`() {
        assertEquals(listOf("mp4"), FormatNormalizer.availableContainers(listOf(format("a", 360, "3gp")), 360))
    }

    private fun format(
        id: String,
        height: Int?,
        container: String = "mp4",
        hasAudio: Boolean = false,
        bitrate: Double = 1_000.0,
        codec: String = "avc1",
    ) = VideoFormat(
        formatId = id,
        width = height?.let { it * 16 / 9 },
        height = height,
        fps = 30.0,
        codec = codec,
        container = container,
        bitrateKbps = bitrate,
        fileSizeBytes = 1_000_000,
        hasAudio = hasAudio,
        hdr = false,
    )
}
