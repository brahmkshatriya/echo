package dev.brahmkshatriya.echo.di

import android.app.Application
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.common.ControllerExtension
import dev.brahmkshatriya.echo.common.LyricsExtension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.TrackerExtension
import dev.brahmkshatriya.echo.db.models.UserEntity
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.offline.OfflineExtension
import dev.brahmkshatriya.echo.viewmodels.SnackBar
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ExtensionModule {

    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideOfflineExtension(context: Application, cache: SimpleCache) =
        OfflineExtension(context, cache)

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
    fun provideControllerListFlow() = MutableStateFlow<List<ControllerExtension>?>(null)

    @Provides
    @Singleton
    fun provideExtensionLoader(
        context: Application,
        throwableFlow: MutableSharedFlow<Throwable>,
        mutableMessageFlow: MutableSharedFlow<SnackBar.Message>,
        database: EchoDatabase,
        settings: SharedPreferences,
        refresher: MutableSharedFlow<Boolean>,
        userFlow: MutableSharedFlow<UserEntity?>,
        offlineExtension: OfflineExtension,
        extensionListFlow: MutableStateFlow<List<MusicExtension>?>,
        trackerListFlow: MutableStateFlow<List<TrackerExtension>?>,
        controllerListFlow: MutableStateFlow<List<ControllerExtension>?>,
        lyricsListFlow: MutableStateFlow<List<LyricsExtension>?>,
        extensionFlow: MutableStateFlow<MusicExtension?>,
    ) = run {
        val extensionDao = database.extensionDao()
        val userDao = database.userDao()
        ExtensionLoader(
            context,
            offlineExtension,
            throwableFlow,
            mutableMessageFlow,
            extensionDao,
            userDao,
            settings,
            refresher,
            userFlow,
            extensionListFlow,
            trackerListFlow,
            controllerListFlow,
            lyricsListFlow,
            extensionFlow,
        )
    }
}