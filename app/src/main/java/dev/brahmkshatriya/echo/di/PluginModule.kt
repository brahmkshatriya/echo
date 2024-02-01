package dev.brahmkshatriya.echo.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.brahmkshatriya.echo.data.extensions.OfflineExtension
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class PluginModule {

    @Provides
    @Singleton
    fun providesOfflineExtension(app: Application) =
        OfflineExtension(app)

}