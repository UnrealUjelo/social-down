package com.socialdown.app.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DownloadErrorMapperTest {
    @Test fun `maps common site and storage failures to actionable messages`() {
        assertEquals(
            "The site rejected the download (HTTP 403)",
            DownloadErrorMapper.userMessage(Exception(), listOf("ERROR: HTTP Error 403: Forbidden")),
        )
        assertEquals(
            "Not enough storage",
            DownloadErrorMapper.userMessage(Exception("No space left on device"), emptyList()),
        )
    }

    @Test fun `uses a bounded extractor error when no common mapping exists`() {
        assertEquals(
            "example extractor failure",
            DownloadErrorMapper.userMessage(
                Exception("process exited with 1"),
                listOf("ERROR: example extractor failure"),
            ),
        )
    }

    @Test fun `redacts urls from diagnostics`() {
        val safe = DownloadErrorMapper.safeDiagnostic(
            "ERROR: failed at https://example.test/video?token=secret",
        )
        assertFalse(safe.contains("secret"))
        assertEquals("ERROR: failed at <url>", safe)
    }
}
