package com.socialdown.app.core.common

object DownloadErrorMapper {
    fun userMessage(error: Throwable, diagnosticLines: List<String>): String {
        val details = buildString {
            append(error.message.orEmpty())
            diagnosticLines.forEach {
                append('\n')
                append(it)
            }
        }
        return when {
            details.contains("No space", ignoreCase = true) -> "Not enough storage"
            details.contains("Unsupported URL", ignoreCase = true) -> "Link isn't supported"
            details.contains("HTTP Error 403", ignoreCase = true) ->
                "The site rejected the download (HTTP 403)"
            details.contains("HTTP Error 429", ignoreCase = true) ->
                "The site is rate-limiting requests (HTTP 429)"
            details.contains("Requested format is not available", ignoreCase = true) ->
                "That quality is no longer available"
            details.contains("ffmpeg", ignoreCase = true) &&
                details.contains("not found", ignoreCase = true) ->
                "Media processing isn't available"
            details.contains("Unable to download", ignoreCase = true) ||
                details.contains("Network is unreachable", ignoreCase = true) ||
                details.contains("timed out", ignoreCase = true) ->
                "Network or site error"
            else -> diagnosticLines
                .lastOrNull { it.contains("ERROR:", ignoreCase = true) }
                ?.substringAfter("ERROR:", missingDelimiterValue = "")
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?.take(MAX_USER_MESSAGE_LENGTH)
                ?: "Download failed (${error::class.simpleName ?: "unknown error"})"
        }
    }

    fun safeDiagnostic(line: String): String = line
        .replace(URL_PATTERN, "<url>")
        .replace(CONTROL_CHARACTERS, " ")
        .trim()
        .take(MAX_DIAGNOSTIC_LENGTH)

    private val URL_PATTERN = Regex("https?://\\S+", RegexOption.IGNORE_CASE)
    private val CONTROL_CHARACTERS = Regex("[\\u0000-\\u001F\\u007F]")
    private const val MAX_DIAGNOSTIC_LENGTH = 500
    private const val MAX_USER_MESSAGE_LENGTH = 220
}
