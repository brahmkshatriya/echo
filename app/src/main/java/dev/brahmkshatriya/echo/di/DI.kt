package dev.brahmkshatriya.echo.di

import dev.brahmkshatriya.echo.common.models.Shelf
import dev.brahmkshatriya.echo.download.DownloadWorker
import dev.brahmkshatriya.echo.download.Downloader
import dev.brahmkshatriya.echo.download.db.DownloadDatabase
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.playback.PlayerService
import dev.brahmkshatriya.echo.playback.PlayerState
import dev.brahmkshatriya.echo.ui.common.UiViewModel
import dev.brahmkshatriya.echo.ui.common.SnackBarHandler
import dev.brahmkshatriya.echo.ui.download.DownloadViewModel
import dev.brahmkshatriya.echo.ui.extensions.ExtensionsViewModel
import dev.brahmkshatriya.echo.ui.extensions.add.AddViewModel
import dev.brahmkshatriya.echo.ui.extensions.login.LoginUserListViewModel
import dev.brahmkshatriya.echo.ui.extensions.login.LoginViewModel
import dev.brahmkshatriya.echo.ui.main.home.HomeFeedViewModel
import dev.brahmkshatriya.echo.ui.main.library.LibraryViewModel
import dev.brahmkshatriya.echo.ui.main.search.SearchViewModel
import dev.brahmkshatriya.echo.ui.media.MediaViewModel
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.ui.player.info.TrackInfoViewModel
import dev.brahmkshatriya.echo.ui.player.lyrics.LyricsViewModel
import dev.brahmkshatriya.echo.ui.playlist.edit.EditPlaylistViewModel
import dev.brahmkshatriya.echo.ui.playlist.save.SaveToPlaylistViewModel
import dev.brahmkshatriya.echo.ui.shelf.ShelfViewModel
import dev.brahmkshatriya.echo.utils.ContextUtils.getSettings
import kotlinx.coroutines.flow.MutableStateFlow
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
        single { MutableStateFlow<List<Shelf>>(listOf()) }
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
        viewModelOf(::UiViewModel)

        viewModelOf(::PlayerViewModel)
        viewModelOf(::LyricsViewModel)
        viewModelOf(::TrackInfoViewModel)

        viewModelOf(::ExtensionsViewModel)
        viewModelOf(::LoginUserListViewModel)
        viewModelOf(::AddViewModel)
        viewModelOf(::LoginViewModel)

        viewModelOf(::HomeFeedViewModel)
        viewModelOf(::LibraryViewModel)
        viewModelOf(::SearchViewModel)

        viewModelOf(::MediaViewModel)
        viewModelOf(::ShelfViewModel)

        viewModelOf(::SaveToPlaylistViewModel)
        viewModelOf(::EditPlaylistViewModel)

        viewModelOf(::DownloadViewModel)
    }

    val appModule = module {
        includes(baseModule)
        includes(extensionModule)
        includes(playerModule)
        includes(downloadModule)
        includes(uiModules)
    }
}