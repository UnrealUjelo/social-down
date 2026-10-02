package com.socialdown.app.core.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.socialdown.app.core.database.DownloadDao
import com.socialdown.app.core.database.SocialDownDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SocialDownDatabase =
        Room.databaseBuilder(context, SocialDownDatabase::class.java, "social_down.db")
            .build()

    @Provides
    fun provideDownloadDao(database: SocialDownDatabase): DownloadDao = database.downloadDao()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

    @Provides
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver
}
