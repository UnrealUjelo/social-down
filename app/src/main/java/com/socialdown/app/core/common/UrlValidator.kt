package com.socialdown.app.core.common

import java.net.URI

object UrlValidator {
    fun isValid(value: String): Boolean = runCatching {
        val uri = URI(value.trim())
        uri.scheme?.lowercase() in setOf("http", "https") &&
            !uri.host.isNullOrBlank() &&
            uri.userInfo == null
    }.getOrDefault(false)

    fun firstUrl(text: String?): String? = text
        ?.split(Regex("\\s+"))
        ?.map { it.trim().trimEnd('.', ',', ')', ']', '}') }
        ?.firstOrNull(::isValid)
}
