package com.socialdown.app.core.downloader

import android.content.ContentResolver
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.core.net.toUri
import com.socialdown.app.core.database.DownloadDao
import com.socialdown.app.core.database.DownloadEntity
import com.socialdown.app.core.model.DownloadRequest
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerMediaDownloader @Inject constructor(
    private val workManager: WorkManager,
    private val downloadDao: DownloadDao,
    private val contentResolver: ContentResolver,
) : MediaDownloader {
    override fun observeDownloads(): Flow<List<DownloadEntity>> = downloadDao.observeAll()

    override suspend fun enqueue(request: DownloadRequest) {
        downloadDao.upsert(DownloadEntity.queued(request))
        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(request.toWorkData())
            .addTag(tag(request.id))
            .build()
        workManager.enqueueUniqueWork(workName(request.id), ExistingWorkPolicy.KEEP, workRequest)
    }

    override suspend fun cancel(id: String) {
        workManager.cancelUniqueWork(workName(id))
        YoutubeDL.getInstance().destroyProcessById(id)
        downloadDao.get(id)?.let {
            downloadDao.upsert(
                it.copy(
                    status = com.socialdown.app.core.model.DownloadStatus.CANCELED,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    override suspend fun delete(id: String) {
        val entity = downloadDao.get(id) ?: return
        entity.outputUri?.let { uri -> runCatching { contentResolver.delete(uri.toUri(), null, null) } }
        downloadDao.delete(id)
    }

    private fun DownloadRequest.toWorkData(): Data = Data.Builder()
        .putString(DownloadWorker.KEY_ID, id)
        .putString(DownloadWorker.KEY_MEDIA_ID, mediaId)
        .putString(DownloadWorker.KEY_URL, sourceUrl)
        .putString(DownloadWorker.KEY_TITLE, title)
        .putString(DownloadWorker.KEY_THUMBNAIL, thumbnailUrl)
        .putString(DownloadWorker.KEY_KIND, kind.name)
        .putInt(DownloadWorker.KEY_VIDEO_HEIGHT, videoHeight ?: 0)
        .putString(DownloadWorker.KEY_VIDEO_CONTAINER, videoContainer)
        .putString(DownloadWorker.KEY_AUDIO_OUTPUT, audioOutput.name)
        .putInt(DownloadWorker.KEY_AUDIO_BITRATE, audioBitrateKbps ?: 0)
        .build()

    private fun tag(id: String) = "download:$id"
    private fun workName(id: String) = "social-down-download:$id"
}
