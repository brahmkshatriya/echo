package dev.brahmkshatriya.echo.di

import dev.brahmkshatriya.echo.download.DownloadWorker
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.download.db.DownloadDatabase
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.db.ExtensionDatabase
import dev.brahmkshatriya.echo.playback.PlayerService
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.utils.ContextUtils.getSettings
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

object DI {

    private val baseModule = module {
        single { androidApplication().getSettings() }
        single { App(get(), get()) }
    }

    private val extensionModule = module {
        includes(baseModule)
        singleOf(::ExtensionLoader)
        singleOf(ExtensionDatabase::create)
    }

    private val downloadModule = module {
        includes(extensionModule)
        singleOf(::Downloader)
        workerOf(::DownloadWorker)
        singleOf(DownloadDatabase::create)
    }

    private val playerModule = module {
        includes(extensionModule)
        singleOf(PlayerService::getCache)
        single { PlayerState() }
    }

    private val viewModelModules = module {
        viewModelOf(::PlayerViewModel)
    }

    val appModule = module {
        includes(baseModule)
        includes(extensionModule)
        includes(playerModule)
        includes(downloadModule)
        includes(viewModelModules)
    }
}