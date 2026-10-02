package com.socialdown.app.core.extractor

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpRuntime @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val initializationMutex = Mutex()
    @Volatile private var initialized = false

    suspend fun ensureInitialized() {
        if (initialized) return
        initializationMutex.withLock {
            if (initialized) return
            withContext(Dispatchers.IO) {
                FFmpeg.getInstance().init(context)
                YoutubeDL.getInstance().init(context)
                updateYtDlpIfNeeded()
            }
            initialized = true
        }
    }

    private fun updateYtDlpIfNeeded() {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastAttempt = preferences.getLong(KEY_LAST_UPDATE_ATTEMPT, 0L)
        if (now - lastAttempt < UPDATE_RETRY_INTERVAL_MS) return

        preferences.edit { putLong(KEY_LAST_UPDATE_ATTEMPT, now) }
        runCatching {
            YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)
        }.onSuccess { status ->
            preferences.edit { putLong(KEY_LAST_SUCCESSFUL_UPDATE, now) }
            Log.i(
                TAG,
                "yt-dlp update status: $status; version=${YoutubeDL.getInstance().versionName(context)}",
            )
        }.onFailure { error ->
            // Keep the bundled executable available when the device is offline or GitHub is unavailable.
            Log.w(TAG, "Couldn't refresh yt-dlp; continuing with bundled version", error)
        }
    }

    private companion object {
        const val TAG = "YtDlpRuntime"
        const val PREFERENCES_NAME = "yt_dlp_runtime"
        const val KEY_LAST_UPDATE_ATTEMPT = "last_update_attempt"
        const val KEY_LAST_SUCCESSFUL_UPDATE = "last_successful_update"
        const val UPDATE_RETRY_INTERVAL_MS = 6 * 60 * 60 * 1_000L
    }
}
