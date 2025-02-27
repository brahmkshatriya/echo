package dev.brahmkshatriya.echo.di

import dev.brahmkshatriya.echo.download.DownloadWorker
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.download.db.DownloadDatabase
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.db.ExtensionDatabase
import dev.brahmkshatriya.echo.playback.PlayerService
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.ui.UiViewModel
import dev.brahmkshatriya.echo.ui.common.SnackBarHandler
import dev.brahmkshatriya.echo.ui.extensions.ExtensionsViewModel
import dev.brahmkshatriya.echo.ui.extensions.login.LoginUserListViewModel
import dev.brahmkshatriya.echo.ui.extensions.login.LoginViewModel
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.ui.player.lyrics.LyricsViewModel
import dev.brahmkshatriya.echo.utils.ContextUtils.getSettings
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

object DI {

    private val baseModule = module {
        single { androidApplication().getSettings() }
        singleOf(::App)
    }

    private val extensionModule = module {
        includes(baseModule)
        singleOf(ExtensionDatabase::create)
        singleOf(::ExtensionLoader)
    }

    private val downloadModule = module {
        includes(extensionModule)
        singleOf(DownloadDatabase::create)
        singleOf(::Downloader)
        workerOf(::DownloadWorker)
    }

    private val playerModule = module {
        includes(extensionModule)
        singleOf(PlayerService::getCache)
        single { PlayerState() }
    }

    private val uiModules = module {
        singleOf(::SnackBarHandler)
        viewModelOf(::PlayerViewModel)
        viewModelOf(::LyricsViewModel)
        viewModelOf(::UiViewModel)
        viewModelOf(::ExtensionsViewModel)
        viewModelOf(::LoginUserListViewModel)
        viewModelOf(::LoginViewModel)
    }

    val appModule = module {
        includes(baseModule)
        includes(extensionModule)
        includes(playerModule)
        includes(downloadModule)
        includes(uiModules)
    }
}