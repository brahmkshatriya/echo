package dev.brahmkshatriya.echo.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.db.models.UserEntity
import dev.brahmkshatriya.echo.playback.Current
import dev.brahmkshatriya.echo.playback.listeners.Radio
import dev.brahmkshatriya.echo.playback.render.FFTAudioProcessor
import dev.brahmkshatriya.echo.ui.settings.AudioFragment.AudioPreference.Companion.CACHE_SIZE
import dev.brahmkshatriya.echo.viewmodels.SnackBar
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
class AppModule {

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
    fun provideDatabase(application: Application) = Room.databaseBuilder(
        application, EchoDatabase::class.java, "echo-database"
    ).fallbackToDestructiveMigration().build()

    @Provides
    @Singleton
    fun provideLoginUserFlow() = MutableSharedFlow<UserEntity?>()

    @Provides
    @Singleton
    @UnstableApi
    fun provideCache(application: Application, settings: SharedPreferences): SimpleCache {
        val databaseProvider = StandaloneDatabaseProvider(application)
        val cacheSize = settings.getInt(CACHE_SIZE, 200)
        return SimpleCache(
            File(application.cacheDir, "exoplayer"),
            LeastRecentlyUsedCacheEvictor(cacheSize * 1024 * 1024L),
            databaseProvider
        )
    }

    @Provides
    @Singleton
    fun currentMediaItemFlow() = MutableStateFlow<Current?>(null)

    @Provides
    @Singleton
    fun provideExtensionListFlow() = MutableStateFlow<Radio.State>(Radio.State.Empty)

    @Provides
    @Singleton
    fun providesAudioProcessor() = FFTAudioProcessor()
}