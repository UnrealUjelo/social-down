package com.socialdown.app.core.extractor

import com.socialdown.app.core.model.MediaInfo

interface MediaExtractor {
    suspend fun analyze(url: String): Result<MediaInfo>
}
