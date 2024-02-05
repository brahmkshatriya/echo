package dev.brahmkshatriya.echo.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.brahmkshatriya.echo.data.clients.HomeFeedClient
import dev.brahmkshatriya.echo.data.clients.SearchClient
import dev.brahmkshatriya.echo.data.clients.TrackClient
import dev.brahmkshatriya.echo.data.extensions.OfflineExtension
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class PluginModule {

    private var offline : OfflineExtension? = null
    private fun getOfflineExtension(app: Application) = offline ?: OfflineExtension(app).also {
        offline = it
    }

    @Provides
    @Singleton
    fun provideSearchClient(app: Application) : SearchClient = getOfflineExtension(app)

    @Provides
    @Singleton
    fun provideHomeClient(app: Application) : HomeFeedClient = getOfflineExtension(app)

    @Provides
    @Singleton
    fun provideTrackClient(app: Application) : TrackClient = getOfflineExtension(app)
}