package dev.brahmkshatriya.echo.di

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.brahmkshatriya.echo.plugger.ExtensionMetadata
import dev.brahmkshatriya.echo.plugger.FileSystemPluginSource
import dev.brahmkshatriya.echo.plugger.ImportType
import dev.brahmkshatriya.echo.plugger.LocalExtensionRepo
import dev.brahmkshatriya.echo.plugger.LyricsExtension
import dev.brahmkshatriya.echo.plugger.LyricsExtensionRepo
import dev.brahmkshatriya.echo.plugger.MusicExtension
import dev.brahmkshatriya.echo.plugger.MusicExtensionRepo
import dev.brahmkshatriya.echo.plugger.TrackerExtension
import dev.brahmkshatriya.echo.plugger.TrackerExtensionRepo
import dev.brahmkshatriya.echo.plugger.parser.ApkFileManifestParser
import dev.brahmkshatriya.echo.plugger.parser.ApkManifestParser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import tel.jeelpa.plugger.PluginRepo
import tel.jeelpa.plugger.PluginRepoImpl
import tel.jeelpa.plugger.RepoComposer
import tel.jeelpa.plugger.pluginloader.AndroidPluginLoader
import tel.jeelpa.plugger.pluginloader.apk.ApkPluginSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ExtensionModule {


    @Provides
    @Singleton
    fun providesRefresher(): MutableSharedFlow<Boolean> = MutableStateFlow(false)

    private fun <T> getComposed(
        context: Context,
        suffix: String,
        vararg repo: PluginRepo<ExtensionMetadata, T>
    ): RepoComposer<ExtensionMetadata, T> {
        val loader = AndroidPluginLoader<ExtensionMetadata, T>(context)
        val apkFilePluginRepo = PluginRepoImpl(
            FileSystemPluginSource(context.filesDir, ".apk"),
            ApkFileManifestParser(context.packageManager, ApkManifestParser(ImportType.Apk)),
            loader,
        )
        val appPluginRepo = PluginRepoImpl(
            ApkPluginSource(context, "dev.brahmkshatriya.echo.$suffix"),
            ApkManifestParser(ImportType.App),
            loader
        )
        return RepoComposer(appPluginRepo, apkFilePluginRepo, *repo)
    }

    @Provides
    @Singleton
    fun providesMusicLoader(context: Application) =
        MusicExtensionRepo(context, getComposed(context, "music", LocalExtensionRepo(context)))

    @Provides
    @Singleton
    fun providesTrackerLoader(context: Application) =
        TrackerExtensionRepo(context, getComposed(context, "tracker"))

    @Provides
    @Singleton
    fun providesLyricsLoader(context: Application) =
        LyricsExtensionRepo(context, getComposed(context, "lyrics"))


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
}