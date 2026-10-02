package com.socialdown.app.core.common

import com.socialdown.app.core.model.DownloadStatus

object DownloadStateMachine {
    private val transitions = mapOf(
        DownloadStatus.QUEUED to setOf(
            DownloadStatus.ANALYZING,
            DownloadStatus.DOWNLOADING,
            DownloadStatus.CANCELED,
            DownloadStatus.FAILED,
        ),
        DownloadStatus.ANALYZING to setOf(
            DownloadStatus.DOWNLOADING,
            DownloadStatus.CANCELED,
            DownloadStatus.FAILED,
        ),
        DownloadStatus.DOWNLOADING to setOf(
            DownloadStatus.MERGING,
            DownloadStatus.CONVERTING,
            DownloadStatus.COMPLETED,
            DownloadStatus.CANCELED,
            DownloadStatus.FAILED,
        ),
        DownloadStatus.MERGING to setOf(
            DownloadStatus.COMPLETED,
            DownloadStatus.CANCELED,
            DownloadStatus.FAILED,
        ),
        DownloadStatus.CONVERTING to setOf(
            DownloadStatus.COMPLETED,
            DownloadStatus.CANCELED,
            DownloadStatus.FAILED,
        ),
        DownloadStatus.COMPLETED to emptySet(),
        DownloadStatus.FAILED to emptySet(),
        DownloadStatus.CANCELED to emptySet(),
    )

    fun canTransition(from: DownloadStatus, to: DownloadStatus): Boolean =
        from == to || transitions.getValue(from).contains(to)
}
