package com.socialdown.app.core.di

import com.socialdown.app.core.downloader.MediaDownloader
import com.socialdown.app.core.downloader.WorkManagerMediaDownloader
import com.socialdown.app.core.extractor.MediaExtractor
import com.socialdown.app.core.extractor.YtDlpMediaExtractor
import com.socialdown.app.core.preferences.DataStorePreferencesRepository
import com.socialdown.app.core.preferences.PreferencesRepository
import com.socialdown.app.core.media.MediaProcessor
import com.socialdown.app.core.media.YtDlpMediaProcessor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds
    @Singleton
    abstract fun bindMediaExtractor(implementation: YtDlpMediaExtractor): MediaExtractor

    @Binds
    @Singleton
    abstract fun bindMediaDownloader(implementation: WorkManagerMediaDownloader): MediaDownloader

    @Binds
    @Singleton
    abstract fun bindPreferences(
        implementation: DataStorePreferencesRepository,
    ): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindMediaProcessor(implementation: YtDlpMediaProcessor): MediaProcessor
}
