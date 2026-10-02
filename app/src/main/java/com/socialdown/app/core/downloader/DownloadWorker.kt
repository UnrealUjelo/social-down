package com.socialdown.app.core.downloader

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.socialdown.app.MainActivity
import com.socialdown.app.R
import com.socialdown.app.core.common.DownloadErrorMapper
import com.socialdown.app.core.common.FilenameSanitizer
import com.socialdown.app.core.database.DownloadDao
import com.socialdown.app.core.model.AudioOutput
import com.socialdown.app.core.model.DownloadStatus
import com.socialdown.app.core.model.MediaKind
import com.socialdown.app.core.media.MediaProcessingPlan
import com.socialdown.app.core.media.MediaProcessor
import com.socialdown.app.core.extractor.YtDlpRuntime
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val downloadDao: DownloadDao,
    private val runtime: YtDlpRuntime,
    private val mediaProcessor: MediaProcessor,
) : CoroutineWorker(appContext, params) {
    private val downloadId = inputData.getString(KEY_ID).orEmpty()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val title = inputData.getString(KEY_TITLE).orEmpty()
        var workDirectory: File? = null
        val recentOutput = ArrayDeque<String>(MAX_DIAGNOSTIC_LINES)
        try {
            require(downloadId.isNotBlank())
            runtime.ensureInitialized()
            setForeground(foregroundInfo(title, 0, "Queued"))

            workDirectory = secureWorkDirectory(downloadId).apply {
                deleteRecursively()
                mkdirs()
            }
            val outputTemplate = File(
                workDirectory,
                "${FilenameSanitizer.sanitize(title)}.%(ext)s",
            ).absolutePath
            val request = buildYtDlpRequest(outputTemplate)
            var lastProgress = -1
            var lastStatus = DownloadStatus.QUEUED

            executeCancellable(request) { progress, eta, line ->
                val diagnostic = DownloadErrorMapper.safeDiagnostic(line)
                if (diagnostic.isNotBlank()) {
                    synchronized(recentOutput) {
                        if (recentOutput.size == MAX_DIAGNOSTIC_LINES) recentOutput.removeFirst()
                        recentOutput.addLast(diagnostic)
                    }
                }
                val status = statusFromLine(line)
                val normalizedProgress = progress.roundToInt().coerceIn(0, 99)
                if (normalizedProgress != lastProgress || status != lastStatus) {
                    lastProgress = normalizedProgress
                    lastStatus = status
                    val safeEta = eta.takeIf { it >= 0 }
                    runCatching {
                        kotlinx.coroutines.runBlocking {
                            downloadDao.updateProgress(
                                id = downloadId,
                                status = status.name,
                                progress = normalizedProgress,
                                etaSeconds = safeEta,
                            )
                            setForeground(
                                foregroundInfo(
                                    title,
                                    normalizedProgress,
                                    status.userLabel(),
                                ),
                            )
                        }
                    }
                }
            }

            val resultFile = workDirectory.listFiles()
                ?.filter { it.isFile && !it.name.endsWith(".part") }
                ?.maxByOrNull { it.length() }
                ?: error("Download produced no media file")
            val uri = publishToMediaStore(resultFile, title)
            downloadDao.markCompleted(
                id = downloadId,
                status = DownloadStatus.COMPLETED.name,
                outputUri = uri.toString(),
            )
            showCompletionNotification(title)
            Result.success()
        } catch (cancelled: CancellationException) {
            markCanceled()
            throw cancelled
        } catch (cancelled: YoutubeDL.CanceledException) {
            markCanceled()
            Result.failure()
        } catch (interrupted: InterruptedException) {
            markCanceled()
            Result.failure()
        } catch (error: Throwable) {
            val diagnostics = synchronized(recentOutput) { recentOutput.toList() }
            val message = DownloadErrorMapper.userMessage(error, diagnostics)
            Log.e(
                TAG,
                "Download $downloadId failed (${error::class.simpleName}): " +
                    diagnostics.joinToString(separator = " | "),
            )
            downloadDao.markFailed(downloadId, DownloadStatus.FAILED.name, message)
            Result.failure()
        } finally {
            workDirectory?.takeIf(::isSafeWorkDirectory)?.deleteRecursively()
        }
    }

    private suspend fun executeCancellable(
        request: YoutubeDLRequest,
        callback: (Float, Long, String) -> Unit,
    ) = suspendCancellableCoroutine { continuation ->
        val processThread = thread(
            start = false,
            isDaemon = true,
            name = "social-down-$downloadId",
        ) {
            try {
                YoutubeDL.getInstance().execute(request, downloadId, true, callback)
                if (continuation.isActive) continuation.resume(Unit)
            } catch (error: Throwable) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }
        continuation.invokeOnCancellation {
            YoutubeDL.getInstance().destroyProcessById(downloadId)
            processThread.interrupt()
        }
        processThread.start()
    }

    private fun buildYtDlpRequest(outputTemplate: String): YoutubeDLRequest {
        val url = inputData.getString(KEY_URL).orEmpty()
        val kind = MediaKind.valueOf(inputData.getString(KEY_KIND) ?: MediaKind.VIDEO.name)
        return YoutubeDLRequest(url)
            .addOption("--no-playlist")
            .addOption("--newline")
            .addOption("--no-mtime")
            .addOption("--output", outputTemplate)
            .apply {
                when (kind) {
                    MediaKind.VIDEO -> configureVideo()
                    MediaKind.AUDIO -> configureAudio()
                }
            }
    }

    private fun YoutubeDLRequest.configureVideo() {
        val height = inputData.getInt(KEY_VIDEO_HEIGHT, 1080).coerceAtLeast(144)
        val container = inputData.getString(KEY_VIDEO_CONTAINER)
            ?: "mp4"
        applyProcessingPlan(mediaProcessor.videoPlan(height, container))
    }

    private fun YoutubeDLRequest.configureAudio() {
        val output = AudioOutput.valueOf(
            inputData.getString(KEY_AUDIO_OUTPUT) ?: AudioOutput.ORIGINAL.name,
        )
        val bitrate = inputData.getInt(KEY_AUDIO_BITRATE, 0).takeIf { it > 0 }
        applyProcessingPlan(mediaProcessor.audioPlan(output, bitrate))
    }

    private fun YoutubeDLRequest.applyProcessingPlan(plan: MediaProcessingPlan) {
        addOption("--format", plan.formatSelector)
        plan.mergeContainer?.let { addOption("--merge-output-format", it) }
        if (plan.extractAudio) addOption("--extract-audio")
        plan.audioFormat?.let { addOption("--audio-format", it) }
        plan.audioQuality?.let { addOption("--audio-quality", it) }
    }

    private suspend fun publishToMediaStore(file: File, title: String): Uri {
        val extension = file.extension.lowercase().ifBlank { "bin" }
        val displayName = uniqueDisplayName(FilenameSanitizer.sanitize(title), extension)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            val kind = MediaKind.valueOf(
                inputData.getString(KEY_KIND) ?: MediaKind.VIDEO.name,
            )
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType(extension, kind))
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/Social Down")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = appContext.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val uri = resolver.insert(collection, values) ?: error("Couldn't create output file")
        try {
            resolver.openOutputStream(uri, "w")?.use { output ->
                file.inputStream().buffered().use { input -> input.copyTo(output) }
            } ?: error("Couldn't open output file")
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                null,
                null,
            )
            return uri
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    private fun uniqueDisplayName(baseName: String, extension: String): String {
        val resolver = appContext.contentResolver
        val relativePath = "Download/Social Down/"
        var index = 0
        while (true) {
            val suffix = if (index == 0) "" else " ($index)"
            val candidate = "$baseName$suffix.$extension"
            resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?",
                arrayOf(candidate, relativePath),
                null,
            )?.use { if (!it.moveToFirst()) return candidate }
            index += 1
        }
    }

    private fun secureWorkDirectory(id: String): File {
        val root = File(appContext.filesDir, "download-work").canonicalFile
        root.mkdirs()
        val directory = File(root, id).canonicalFile
        require(directory.path.startsWith(root.path + File.separator))
        return directory
    }

    private fun isSafeWorkDirectory(file: File): Boolean = runCatching {
        val root = File(appContext.filesDir, "download-work").canonicalFile
        file.canonicalPath.startsWith(root.path + File.separator)
    }.getOrDefault(false)

    private fun foregroundInfo(title: String, progress: Int, status: String): ForegroundInfo {
        ensureNotificationChannel()
        val openIntent = PendingIntent.getActivity(
            appContext,
            downloadId.hashCode(),
            Intent(appContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_download)
            .setContentTitle(status)
            .setContentText(title)
            .setContentIntent(openIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, progress, progress <= 0)
            .build()
        return ForegroundInfo(
            NOTIFICATION_ID_BASE + downloadId.hashCode().and(0x0FFF),
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private fun showCompletionNotification(title: String) {
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_download)
            .setContentTitle("Download complete")
            .setContentText(title)
            .setAutoCancel(true)
            .build()
        val canNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        if (canNotify) runCatching {
            NotificationManagerCompat.from(appContext).notify(
                COMPLETED_ID_BASE + downloadId.hashCode().and(0x0FFF),
                notification,
            )
        }
    }

    private fun ensureNotificationChannel() {
        val manager = appContext.getSystemService<NotificationManager>() ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                appContext.getString(R.string.download_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = appContext.getString(R.string.download_channel_description)
            },
        )
    }

    private suspend fun markCanceled() {
        downloadDao.get(downloadId)?.let {
            downloadDao.upsert(
                it.copy(status = DownloadStatus.CANCELED, updatedAt = System.currentTimeMillis()),
            )
        }
    }

    private fun statusFromLine(line: String): DownloadStatus = when {
        line.contains("[Merger]", ignoreCase = true) -> DownloadStatus.MERGING
        line.contains("[ExtractAudio]", ignoreCase = true) ||
            line.contains("[AudioConvertor]", ignoreCase = true) -> DownloadStatus.CONVERTING
        else -> DownloadStatus.DOWNLOADING
    }

    private fun DownloadStatus.userLabel(): String = when (this) {
        DownloadStatus.MERGING -> "Merging"
        DownloadStatus.CONVERTING -> "Converting"
        else -> "Downloading"
    }

    private fun mimeType(extension: String, kind: MediaKind): String = when (extension) {
        "mp4", "m4v" -> if (kind == MediaKind.AUDIO) "audio/mp4" else "video/mp4"
        "webm" -> if (kind == MediaKind.AUDIO) "audio/webm" else "video/webm"
        "mkv" -> "video/x-matroska"
        "mp3" -> "audio/mpeg"
        "m4a", "aac" -> "audio/mp4"
        "opus" -> "audio/opus"
        "ogg" -> "audio/ogg"
        "flac" -> "audio/flac"
        "wav" -> "audio/wav"
        else -> "application/octet-stream"
    }

    companion object {
        const val KEY_ID = "id"
        const val KEY_MEDIA_ID = "media_id"
        const val KEY_URL = "url"
        const val KEY_TITLE = "title"
        const val KEY_THUMBNAIL = "thumbnail"
        const val KEY_KIND = "kind"
        const val KEY_VIDEO_HEIGHT = "video_height"
        const val KEY_VIDEO_CONTAINER = "video_container"
        const val KEY_AUDIO_OUTPUT = "audio_output"
        const val KEY_AUDIO_BITRATE = "audio_bitrate"
        private const val CHANNEL_ID = "downloads"
        private const val NOTIFICATION_ID_BASE = 1000
        private const val COMPLETED_ID_BASE = 5000
        private const val MAX_DIAGNOSTIC_LINES = 12
        private const val TAG = "DownloadWorker"
    }
}
