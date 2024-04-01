package dev.brahmkshatriya.echo.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.brahmkshatriya.echo.player.Queue
import dev.brahmkshatriya.echo.viewmodels.SnackBarViewModel
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
    fun provideMessageFlow() = MutableSharedFlow<SnackBarViewModel.Message>()

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
}