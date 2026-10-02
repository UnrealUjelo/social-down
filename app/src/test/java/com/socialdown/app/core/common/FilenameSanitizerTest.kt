package com.socialdown.app.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class FilenameSanitizerTest {
    @Test fun `removes traversal separators and invalid characters`() {
        val name = FilenameSanitizer.sanitize("../../A: video? <today>")
        assertFalse(name.contains(".."))
        assertFalse(name.contains('/'))
        assertFalse(name.contains(':'))
        assertEquals("A video today", name)
    }

    @Test fun `uses a safe fallback and protects reserved names`() {
        assertEquals("download", FilenameSanitizer.sanitize("<>:\\/?*"))
        assertEquals("_CON", FilenameSanitizer.sanitize("CON"))
    }

    @Test fun `resolves duplicate output names without overwriting`() {
        val directory = Files.createTempDirectory("social-down-test").toFile()
        try {
            directory.resolve("Clip.mp4").createNewFile()
            directory.resolve("Clip (1).mp4").createNewFile()
            assertEquals(
                "Clip (2).mp4",
                FilenameSanitizer.resolveCollision(directory, "Clip", "mp4").name,
            )
        } finally {
            directory.deleteRecursively()
        }
    }
}
