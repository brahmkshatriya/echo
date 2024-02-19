package dev.brahmkshatriya.echo.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.HomeFeedClient
import dev.brahmkshatriya.echo.common.clients.SearchClient
import dev.brahmkshatriya.echo.common.clients.TrackClient
import dev.brahmkshatriya.echo.data.extensions.LocalExtensionRepo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
class PluginModule {

    @Provides
    @Singleton
    fun providesPluginLoader(
        application: Application
    ): PluginRepo<ExtensionClient> {

        val loader = AndroidPluginLoader(application)
        val filePluginConfig = FilePluginConfig(application.filesDir.absolutePath, ".echo")
        val apkPluginConfig = PluginConfiguration("dev.brahmkshatriya.echo")

        val composer = RepoComposer(
            FileSystemPluginLoader(application, filePluginConfig, loader),
            ApkPluginLoader(application, apkPluginConfig, loader),
            LocalExtensionRepo()
        )

        return ContextProviderForRepo(application, composer)
    }

    @Provides
    @Singleton
    fun getExtensionClients(pluginLoader: PluginRepo<ExtensionClient>): List<ExtensionClient> {
        val clients = runBlocking { pluginLoader.getAllPlugins().first() }
        return clients
    }

    @Provides
    @Singleton
    fun provideExtension(pluginLoader: PluginRepo<ExtensionClient>): ExtensionClient =
        getExtensionClients(pluginLoader).first()

    @Provides
    @Singleton
    fun provideSearchClient(pluginLoader: PluginRepo<ExtensionClient>): SearchClient =
        provideExtension(pluginLoader) as SearchClient

    @Provides
    @Singleton
    fun provideHomeClient(pluginLoader: PluginRepo<ExtensionClient>): HomeFeedClient =
        provideExtension(pluginLoader) as HomeFeedClient

    @Provides
    @Singleton
    fun provideTrackClient(pluginLoader: PluginRepo<ExtensionClient>): TrackClient =
        provideExtension(pluginLoader) as TrackClient

}