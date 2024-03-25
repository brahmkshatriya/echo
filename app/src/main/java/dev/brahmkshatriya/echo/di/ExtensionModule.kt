package dev.brahmkshatriya.echo.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.data.utils.ApkManifestParser
import dev.brahmkshatriya.echo.data.utils.LocalExtensionRepo
import dev.brahmkshatriya.echo.data.utils.RepoWithPreferences
import kotlinx.coroutines.flow.Flow
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
class ExtensionModule {

    @Provides
    @Singleton
    fun providesPluginLoader(
        application: Application
    ): PluginRepo<ExtensionClient> {

        val loader = AndroidPluginLoader(application)
        val filePluginConfig = FilePluginConfig(application.filesDir.absolutePath, ".echo")
        val apkPluginConfig = PluginConfiguration("dev.brahmkshatriya.echo")
        val repo = RepoComposer(
            FileSystemPluginLoader(application, filePluginConfig, loader),
            ApkPluginLoader(application, apkPluginConfig, loader, ApkManifestParser()),
            LocalExtensionRepo(application)
        )
        return RepoWithPreferences(application, repo)
    }

    @Provides
    @Singleton
    fun provideMutableExtensionFlow() = mutableExtensionFlow

    @Provides
    @Singleton
    fun providesExtensionClient() = ExtensionFlow(extensionFlow)

    @Provides
    @Singleton
    fun provideExtensionListFlow() = mutableExtensionListFlow

    // Dagger cannot directly infer Foo<Bar>, if Bar is an interface
    // That means the Flow<Clients?> cannot be directly injected,
    // So, we need to wrap it in a data class and inject that instead
    data class MutableFlow(val flow: MutableStateFlow<ExtensionClient?>)
    data class ExtensionFlow(val flow: Flow<ExtensionClient?>)
    data class ExtensionListFlow(val flow: MutableStateFlow<List<ExtensionClient>?>)

    private val mutableExtensionFlow = MutableFlow(MutableStateFlow(null))
    private val mutableExtensionListFlow = ExtensionListFlow(MutableStateFlow(null))
    private val extensionFlow = mutableExtensionFlow.flow.asStateFlow()
}