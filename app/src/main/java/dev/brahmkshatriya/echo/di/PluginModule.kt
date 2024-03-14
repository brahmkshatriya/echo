package dev.brahmkshatriya.echo.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.data.extensions.LocalExtensionRepo
import dev.brahmkshatriya.echo.player.Queue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

        return RepoComposer(
            FileSystemPluginLoader(application, filePluginConfig, loader),
            ApkPluginLoader(application, apkPluginConfig, loader, ApkManifestParser()),
            LocalExtensionRepo(application)
        )
    }

    private val mutableExtensionFlow = MutableExtensionFlow(MutableStateFlow(null))
    private val extensionFlow = mutableExtensionFlow.flow.asStateFlow()

    @Provides
    @Singleton
    fun provideMutableExtensionFlow() = mutableExtensionFlow

    @Provides
    @Singleton
    fun providesExtensionClient() =
        ExtensionFlow(extensionFlow)

    @Provides
    @Singleton
    fun provideGlobalQueue() = Queue()

    @Provides
    @Singleton
    fun provideExceptionFlow() = MutableSharedFlow<Exception>()

    @Provides
    @Singleton
    fun provideSettingsPreferences(application: Application): SharedPreferences =
        application.getSharedPreferences(application.packageName, Context.MODE_PRIVATE)

}