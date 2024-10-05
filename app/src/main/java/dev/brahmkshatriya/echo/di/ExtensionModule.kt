package dev.brahmkshatriya.echo.di

import android.app.Application
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.common.LyricsExtension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.TrackerExtension
import dev.brahmkshatriya.echo.db.models.UserEntity
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.offline.OfflineExtension
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ExtensionModule {

    @Provides
    @Singleton
    fun provideOfflineExtension(context: Application) = OfflineExtension(context)

    @Provides
    @Singleton
    fun providesRefresher(): MutableSharedFlow<Boolean> = MutableStateFlow(false)

    @Provides
    @Singleton
    fun providesCurrentFlow() = MutableStateFlow<MusicExtension?>(null)

    @Provides
    @Singleton
    fun provideMusicListFlow() = MutableStateFlow<List<MusicExtension>?>(null)

    @Provides
    @Singleton
    fun providesLyricsList() = MutableStateFlow<List<LyricsExtension>?>(null)

    @Provides
    @Singleton
    fun provideTrackerListFlow() = MutableStateFlow<List<TrackerExtension>?>(null)

    @Provides
    @Singleton
    fun provideExtensionLoader(
        context: Application,
        throwableFlow: MutableSharedFlow<Throwable>,
        database: EchoDatabase,
        settings: SharedPreferences,
        refresher: MutableSharedFlow<Boolean>,
        userFlow: MutableSharedFlow<UserEntity?>,
        offlineExtension: OfflineExtension,
        extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
        trackerListFlow: MutableStateFlow<List<TrackerExtension>?>,
        lyricsListFlow: MutableStateFlow<List<LyricsExtension>?>,
        extensionFlow: MutableStateFlow<MusicExtension?>,
    ) = run {
        val extensionDao = database.extensionDao()
        val userDao = database.userDao()
        ExtensionLoader(
            context,
            offlineExtension,
            throwableFlow,
            extensionDao,
            userDao,
            settings,
            refresher,
            userFlow,
            extensionListFlow,
            trackerListFlow,
            lyricsListFlow,
            extensionFlow,
        )
    }
}