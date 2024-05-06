package dev.brahmkshatriya.echo.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.plugger.ExtensionMetadata
import dev.brahmkshatriya.echo.plugger.FileSystemPluginSource
import dev.brahmkshatriya.echo.plugger.LyricsExtension
import dev.brahmkshatriya.echo.plugger.parser.ApkFileManifestParser
import dev.brahmkshatriya.echo.plugger.parser.ApkManifestParser
import kotlinx.coroutines.flow.MutableStateFlow
import tel.jeelpa.plugger.PluginRepo
import tel.jeelpa.plugger.PluginRepoImpl
import tel.jeelpa.plugger.RepoComposer
import tel.jeelpa.plugger.pluginloader.AndroidPluginLoader
import tel.jeelpa.plugger.pluginloader.apk.ApkPluginSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class LyricsModule {
    @Provides
    @Singleton
    fun providesPluginLoader(
        application: Application
    ): PluginRepo<ExtensionMetadata, LyricsClient> {
        val loader = AndroidPluginLoader<ExtensionMetadata, LyricsClient>(application)
        val apkParser = ApkManifestParser()
        val apkFileParser = ApkFileManifestParser(application.packageManager, apkParser)
        val apkFilePluginRepo = PluginRepoImpl(
            FileSystemPluginSource(application.filesDir,". lyrics"),
            apkFileParser,
            loader,
        )
        val apkPluginRepo = PluginRepoImpl(
            ApkPluginSource(application, "dev.brahmkshatriya.echo.lyrics"),
            apkParser,
            loader
        )
        return RepoComposer(
            apkPluginRepo,
            apkFilePluginRepo
        )
    }

}