package dev.brahmkshatriya.echo.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.brahmkshatriya.echo.common.clients.TrackerClient
import dev.brahmkshatriya.echo.plugger.ApkManifestParser
import kotlinx.coroutines.flow.MutableStateFlow
import tel.jeelpa.plugger.PluginRepo
import tel.jeelpa.plugger.RepoComposer
import tel.jeelpa.plugger.models.PluginConfiguration
import tel.jeelpa.plugger.pluginloader.AndroidPluginLoader
import tel.jeelpa.plugger.pluginloader.apk.ApkPluginLoader
import tel.jeelpa.plugger.pluginloader.file.FilePluginConfig
import tel.jeelpa.plugger.pluginloader.file.FileSystemPluginLoader
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class TrackerModule {
    @Provides
    @Singleton
    fun providesTrackerLoader(
        application: Application
    ): PluginRepo<TrackerClient> {
        val loader = AndroidPluginLoader(application)
        val filePluginConfig = FilePluginConfig(application.filesDir.absolutePath, ".tracker")
        val apkPluginConfig = PluginConfiguration("dev.brahmkshatriya.echo.tracker")
        return RepoComposer(
            FileSystemPluginLoader(application, filePluginConfig, loader),
            ApkPluginLoader(application, apkPluginConfig, loader, ApkManifestParser()),
        )
    }

    data class TrackerListFlow(val flow: MutableStateFlow<List<TrackerClient>?>) {
        val list = flow.value
    }

    private val trackerListFlow = TrackerListFlow(MutableStateFlow(null))

    @Provides
    @Singleton
    fun providesTrackerList() = trackerListFlow
}