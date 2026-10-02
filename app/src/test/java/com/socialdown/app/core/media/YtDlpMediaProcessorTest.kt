package com.socialdown.app.core.media

import com.socialdown.app.core.model.AudioOutput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YtDlpMediaProcessorTest {
    private val processor = YtDlpMediaProcessor()

    @Test fun `mp4 video plan requests compatible audio and merge container`() {
        val plan = processor.videoPlan(1080, "mp4")
        assertTrue(plan.formatSelector.contains("bestvideo[height=1080][ext=mp4]"))
        assertTrue(plan.formatSelector.contains("bestaudio[ext=m4a]"))
        assertEquals("mp4", plan.mergeContainer)
    }

    @Test fun `unsupported video container safely falls back to mp4`() {
        assertEquals("mp4", processor.videoPlan(720, "avi").mergeContainer)
    }

    @Test fun `original audio avoids lossy conversion`() {
        val plan = processor.audioPlan(AudioOutput.ORIGINAL, 320)
        assertFalse(plan.extractAudio)
        assertNull(plan.audioFormat)
        assertNull(plan.audioQuality)
    }

    @Test fun `compressed and lossless outputs receive correct quality options`() {
        assertEquals("256K", processor.audioPlan(AudioOutput.MP3, 256).audioQuality)
        assertEquals("192K", processor.audioPlan(AudioOutput.M4A, 999).audioQuality)
        assertNull(processor.audioPlan(AudioOutput.WAV, 320).audioQuality)
    }
}
