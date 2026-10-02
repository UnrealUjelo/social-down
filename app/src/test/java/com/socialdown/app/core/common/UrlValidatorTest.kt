package com.socialdown.app.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlValidatorTest {
    @Test fun `accepts normal http and https urls`() {
        assertTrue(UrlValidator.isValid("https://example.com/watch?v=123"))
        assertTrue(UrlValidator.isValid("http://media.example.org/file"))
    }

    @Test fun `rejects non web malformed and credential urls`() {
        assertFalse(UrlValidator.isValid("javascript:alert(1)"))
        assertFalse(UrlValidator.isValid("not a url"))
        assertFalse(UrlValidator.isValid("https://user:password@example.com/video"))
    }

    @Test fun `extracts first url from shared text`() {
        assertEquals(
            "https://example.com/v/7",
            UrlValidator.firstUrl("Watch this: https://example.com/v/7 thanks"),
        )
        assertNull(UrlValidator.firstUrl("nothing to see"))
    }
}
