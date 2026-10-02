package com.socialdown.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.socialdown.app.core.model.DownloadStatus
import com.socialdown.app.core.model.MediaKind

@Database(entities = [DownloadEntity::class], version = 1, exportSchema = true)
@TypeConverters(DatabaseConverters::class)
abstract class SocialDownDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}

class DatabaseConverters {
    @TypeConverter fun toDownloadStatus(value: String): DownloadStatus = DownloadStatus.valueOf(value)
    @TypeConverter fun fromDownloadStatus(value: DownloadStatus): String = value.name
    @TypeConverter fun toMediaKind(value: String): MediaKind = MediaKind.valueOf(value)
    @TypeConverter fun fromMediaKind(value: MediaKind): String = value.name
}
