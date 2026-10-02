package com.socialdown.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun get(id: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DownloadEntity)

    @Query(
        """UPDATE downloads SET status = :status, progressPercent = :progress,
            etaSeconds = :etaSeconds, updatedAt = :updatedAt WHERE id = :id""",
    )
    suspend fun updateProgress(
        id: String,
        status: String,
        progress: Int,
        etaSeconds: Long?,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query(
        """UPDATE downloads SET status = :status, progressPercent = 100,
            outputUri = :outputUri, errorMessage = NULL, updatedAt = :updatedAt WHERE id = :id""",
    )
    suspend fun markCompleted(
        id: String,
        status: String,
        outputUri: String,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query(
        """UPDATE downloads SET status = :status, errorMessage = :message,
            updatedAt = :updatedAt WHERE id = :id""",
    )
    suspend fun markFailed(
        id: String,
        status: String,
        message: String,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: String)
}
