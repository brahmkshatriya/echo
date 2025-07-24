package dev.brahmkshatriya.echo.ui.common

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import dev.brahmkshatriya.echo.MainActivity
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.ui.common.SnackBarHandler.Companion.createSnack
import dev.brahmkshatriya.echo.ui.download.DownloadFragment
import dev.brahmkshatriya.echo.ui.extensions.ExtensionsViewModel
import dev.brahmkshatriya.echo.ui.extensions.WebViewUtils.onWebViewIntent
import dev.brahmkshatriya.echo.ui.media.MediaFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

object FragmentUtils {
    inline fun <reified T : Fragment> Fragment.openFragment(
        view: View? = null, bundle: Bundle? = null,
    ) {
        val viewModel by activityViewModels<UiViewModel>()
        openFragment<T>(id, parentFragmentManager, viewModel, view, bundle)
    }

    inline fun <reified T : Fragment> openFragment(
        cont: Int,
        manager: FragmentManager,
        viewModel: UiViewModel,
        view: View? = null,
        bundle: Bundle? = null,
    ) {
        viewModel.collapsePlayer()
        manager.commit {
            setReorderingAllowed(true)
            addToBackStack(null)
            val fragment = createFragment<T>(bundle)
            val old = manager.findFragmentById(cont)
            if (old != null) hide(old)
            add(cont, fragment)
            setPrimaryNavigationFragment(fragment)
        }
    }

    inline fun <reified T : Fragment> createFragment(
        bundle: Bundle? = null
    ): T = T::class.java.getDeclaredConstructor().newInstance().apply { arguments = bundle }

    inline fun <reified T : Fragment> FragmentActivity.openFragment(
        view: View? = null, bundle: Bundle? = null, cont: Int = R.id.navHostFragment
    ) {
        val oldFragment = supportFragmentManager.findFragmentById(cont)
        if (oldFragment == null) {
            val viewModel by viewModel<UiViewModel>()
            openFragment<T>(cont, supportFragmentManager, viewModel, view, bundle)
        } else oldFragment.openFragment<T>(view, bundle)
    }

    inline fun <reified F : Fragment> Fragment.addIfNull(
        id: Int, tag: String, args: Bundle? = null
    ) {
        childFragmentManager.run {
            if (findFragmentByTag(tag) == null) commit {
                val fragment = createFragment<F>(args)
                add(id, fragment, tag)
            }
        }
    }

    fun MainActivity.setupIntents(
        uiViewModel: UiViewModel,
    ) {
        addOnNewIntentListener { onIntent(uiViewModel, it) }
        onIntent(uiViewModel, intent)
    }

    private fun FragmentActivity.onIntent(uiViewModel: UiViewModel, intent: Intent?) {
        this.intent = null
        intent ?: return
        val fromNotif = intent.hasExtra("fromNotification")
        if (fromNotif) uiViewModel.run {
            if (playerSheetState.value == STATE_HIDDEN) return@run
            changePlayerState(STATE_EXPANDED)
            changeMoreState(STATE_COLLAPSED)
            return
        }
        val fromDownload = intent.hasExtra("fromDownload")
        if (fromDownload) {
            uiViewModel.selectedSettingsTab.value = 0
            openFragment<DownloadFragment>()
            return
        }
        val webViewRequest = intent.hasExtra("webViewRequest")
        if (webViewRequest) {
            onWebViewIntent(intent)
            return
        }
        val uri = intent.data
        when (uri?.scheme) {
            "echo" -> runCatching { openItemFragmentFromUri(uri) }
            "file" -> {
                val viewModel by viewModel<ExtensionsViewModel>()
                viewModel.installWithPrompt(listOf(uri.toFile()))
            }
        }
    }

    private fun FragmentActivity.openItemFragmentFromUri(uri: Uri) {
        when (val extensionType = uri.host) {
            "music" -> {
                val extensionId = uri.pathSegments.firstOrNull()
                if (extensionId == null) {
                    createSnack("No extension id found")
                    return
                }
                val type = uri.pathSegments.getOrNull(1)
                val id = uri.pathSegments.getOrNull(2)
                if (id == null) {
                    val vm by viewModel<ExtensionsViewModel>()
                    vm.changeExtension(extensionId)
                    return
                }
                val name = uri.getQueryParameter("name").orEmpty()
                val item: EchoMediaItem? = when (type) {
                    "artist" -> Artist(id, name)
                    "track" -> Track(id, name)
                    "album" -> Album(id, name)
                    "playlist" -> Playlist(id, name, false)
                    else -> null
                }
                if (item == null) {
                    createSnack("Invalid item type")
                    return
                }
                openFragment<MediaFragment>(null, MediaFragment.getBundle(extensionId, item, false))
            }

            else -> {
                createSnack("Opening $extensionType extension is not possible")
            }
        }
    }
}

