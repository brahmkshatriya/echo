package dev.brahmkshatriya.echo.ui.common

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.ui.MainFragment
import dev.brahmkshatriya.echo.ui.item.ItemFragment
import dev.brahmkshatriya.echo.utils.ui.sharedElementTransitions
import dev.brahmkshatriya.echo.viewmodels.SnackBar
import dev.brahmkshatriya.echo.viewmodels.UiViewModel

fun Fragment.openFragment(newFragment: Fragment, view: View? = null) {
    parentFragmentManager.commit {
        val oldFragment = this@openFragment
        if (view?.sharedElementTransitions == true) {
            runCatching {
                addSharedElement(view, view.transitionName)
                newFragment.run {
                    if (arguments == null) arguments = Bundle()
                    arguments!!.putString("transitionName", view.transitionName)
                }
            }.getOrElse { it.printStackTrace() }
        }
        add(oldFragment.id, newFragment)
        hide(oldFragment)
        setReorderingAllowed(true)
        addToBackStack(null)
    }
    val uiViewModel by activityViewModels<UiViewModel>()
    uiViewModel.isMainFragment.value = newFragment is MainFragment
}

fun FragmentActivity.openFragment(newFragment: Fragment, view: View? = null) {
    val oldFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment)!!
    oldFragment.openFragment(newFragment, view)
}

fun FragmentActivity.openItemFragmentFromUri(uri: Uri) {
    fun createSnack(id: Int) {
        val snackbar by viewModels<SnackBar>()
        val message = getString(id)
        snackbar.create(Message(message))
    }

    val extensionType = uri.host
    when (extensionType) {
        "music" -> {
            val extensionId = uri.pathSegments.firstOrNull()
            if (extensionId == null) {
                createSnack(R.string.error_no_client)
                return
            }
            val type = uri.pathSegments.getOrNull(1)
            val id = uri.pathSegments.getOrNull(2)
            if (id == null) {
                createSnack(R.string.error_no_id)
                return
            }
            val name = uri.getQueryParameter("name").orEmpty()
            val item: EchoMediaItem? = when (type) {
                "user" -> User(id, name).toMediaItem()
                "artist" -> Artist(id, name).toMediaItem()
                "track" -> Track(id, name).toMediaItem()
                "album" -> Album(id, name).toMediaItem()
                "playlist" -> Playlist(id, name, false).toMediaItem()
                else -> null
            }
            if (item == null) {
                createSnack(R.string.error_invalid_type)
                return
            }
            openFragment(ItemFragment.newInstance(extensionId, item))
        }

        else -> {
            createSnack(R.string.invalid_extension_host)
        }
    }
}
