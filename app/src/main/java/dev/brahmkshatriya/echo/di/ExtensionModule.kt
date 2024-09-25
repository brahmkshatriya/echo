package dev.brahmkshatriya.echo.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.brahmkshatriya.echo.EchoDatabase
import dev.brahmkshatriya.echo.common.LyricsExtension
import dev.brahmkshatriya.echo.common.MusicExtension
import dev.brahmkshatriya.echo.common.TrackerExtension
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.db.models.UserEntity
import dev.brahmkshatriya.echo.offline.LocalExtensionRepo
import dev.brahmkshatriya.echo.offline.OfflineExtension
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.plugger.AndroidPluginLoader
import dev.brahmkshatriya.echo.extensions.plugger.ApkPluginSource
import dev.brahmkshatriya.echo.extensions.plugger.FilePluginSource
import dev.brahmkshatriya.echo.extensions.plugger.LazyPluginRepo
import dev.brahmkshatriya.echo.extensions.plugger.LazyRepoComposer
import dev.brahmkshatriya.echo.extensions.LyricsExtensionRepo
import dev.brahmkshatriya.echo.extensions.MusicExtensionRepo
import dev.brahmkshatriya.echo.extensions.plugger.LazyPluginRepoImpl
import dev.brahmkshatriya.echo.extensions.TrackerExtensionRepo
import dev.brahmkshatriya.echo.extensions.plugger.ApkFileManifestParser
import dev.brahmkshatriya.echo.extensions.plugger.ApkManifestParser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
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

    private fun Context.getPluginFileDir() = File(filesDir, "extensions").apply { mkdirs() }
    private fun <T : Any> getComposed(
        context: Context,
        suffix: String,
        vararg repo: LazyPluginRepo<Metadata, T>
    ): LazyPluginRepo<Metadata, T> {
        val loader = AndroidPluginLoader<T>(context)
        val apkFilePluginRepo = LazyPluginRepoImpl(
            FilePluginSource(context.getPluginFileDir(), ".eapk"),
            ApkFileManifestParser(context.packageManager, ApkManifestParser(ImportType.Apk)),
            loader,
        )
        val appPluginRepo = LazyPluginRepoImpl(
            ApkPluginSource(context, "dev.brahmkshatriya.echo.$suffix"),
            ApkManifestParser(ImportType.App),
            loader
        )
        return LazyRepoComposer(appPluginRepo, apkFilePluginRepo, *repo)
    }

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
        val musicExtensionRepo = MusicExtensionRepo(
            context, getComposed(context, "music", LocalExtensionRepo(offlineExtension))
        )
        val trackerExtensionRepo =
            TrackerExtensionRepo(context, getComposed(context, "tracker"))
        val lyricsExtensionRepo =
            LyricsExtensionRepo(context, getComposed(context, "lyrics"))
        ExtensionLoader(
            throwableFlow,
            extensionDao,
            userDao,
            settings,
            refresher,
            userFlow,
            musicExtensionRepo,
            trackerExtensionRepo,
            lyricsExtensionRepo,
            extensionListFlow,
            trackerListFlow,
            lyricsListFlow,
            extensionFlow,
        )
    }
}