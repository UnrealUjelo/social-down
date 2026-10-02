package com.socialdown.app.core.common

import com.socialdown.app.core.model.DownloadStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadStateMachineTest {
    @Test fun `allows normal download lifecycle`() {
        assertTrue(DownloadStateMachine.canTransition(DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING))
        assertTrue(DownloadStateMachine.canTransition(DownloadStatus.DOWNLOADING, DownloadStatus.MERGING))
        assertTrue(DownloadStateMachine.canTransition(DownloadStatus.MERGING, DownloadStatus.COMPLETED))
    }

    @Test fun `prevents terminal states from restarting`() {
        assertFalse(DownloadStateMachine.canTransition(DownloadStatus.COMPLETED, DownloadStatus.DOWNLOADING))
        assertFalse(DownloadStateMachine.canTransition(DownloadStatus.CANCELED, DownloadStatus.QUEUED))
        assertTrue(DownloadStateMachine.canTransition(DownloadStatus.FAILED, DownloadStatus.FAILED))
    }
}
