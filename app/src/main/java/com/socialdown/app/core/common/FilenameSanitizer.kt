package com.socialdown.app.core.common

import java.io.File

object FilenameSanitizer {
    private val invalidCharacters = Regex("[\\x00-\\x1F\\x7F<>:\"/\\\\|?*]")
    private val traversal = Regex("\\.{2,}")
    private val whitespace = Regex("\\s+")
    private val reservedNames = Regex(
        "^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?$",
        RegexOption.IGNORE_CASE,
    )

    fun sanitize(title: String, fallback: String = "download", maxLength: Int = 120): String {
        val safe = title
            .replace(invalidCharacters, " ")
            .replace(traversal, ".")
            .replace(whitespace, " ")
            .trim(' ', '.')
            .take(maxLength)
            .trimEnd(' ', '.')
        return when {
            safe.isBlank() -> fallback
            reservedNames.matches(safe) -> "_$safe"
            else -> safe
        }
    }

    fun resolveCollision(directory: File, baseName: String, extension: String): File {
        val cleanExtension = extension.trimStart('.').lowercase()
        val suffix = if (cleanExtension.isBlank()) "" else ".$cleanExtension"
        var candidate = File(directory, "$baseName$suffix")
        var index = 1
        while (candidate.exists()) {
            candidate = File(directory, "$baseName ($index)$suffix")
            index += 1
        }
        return candidate
    }
}
