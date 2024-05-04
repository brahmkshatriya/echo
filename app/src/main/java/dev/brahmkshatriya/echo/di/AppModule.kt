package dev.brahmkshatriya.echo.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.room.Room
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.models.UserEntity
import dev.brahmkshatriya.echo.playback.PlayerListener
import dev.brahmkshatriya.echo.playback.Queue
import dev.brahmkshatriya.echo.viewmodels.SnackBar
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.File
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun provideGlobalQueue() = Queue()

    @Provides
    @Singleton
    fun provideThrowableFlow() = MutableSharedFlow<Throwable>()

    @Provides
    @Singleton
    fun provideMessageFlow() = MutableSharedFlow<SnackBar.Message>()

    @Provides
    @Singleton
    fun provideSettingsPreferences(application: Application): SharedPreferences =
        application.getSharedPreferences(application.packageName, Context.MODE_PRIVATE)

    @Provides
    @Singleton
    @UnstableApi
    fun provideCache(application: Application): SimpleCache {
        val databaseProvider = StandaloneDatabaseProvider(application)
        return SimpleCache(
            File(application.cacheDir, "exoplayer"),
            LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024L),
            databaseProvider
        )
    }

    @Provides
    fun provideInitialPlayerState(global: Queue) =
        if (global.queue.isEmpty()) STATE_HIDDEN else STATE_COLLAPSED

    @Provides
    @Singleton
    fun provideDatabase(application: Application) = Room.databaseBuilder(
        application, EchoDatabase::class.java, "echo-database"
    ).build()

    @Provides
    @Singleton
    fun provideLoginUserFlow() = MutableSharedFlow<UserEntity?>()

    @Provides
    @Singleton
    fun providePlayerListener(
        application: Application,
        extensionList: ExtensionModule.ExtensionListFlow,
        trackerListFlow: TrackerModule.TrackerListFlow,
        global: Queue,
        settings: SharedPreferences,
        throwableFlow: MutableSharedFlow<Throwable>,
        messageFlow: MutableSharedFlow<SnackBar.Message>,
    ) = PlayerListener(
        application, extensionList, trackerListFlow, global, settings, throwableFlow, messageFlow
    )
}