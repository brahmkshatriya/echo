package dev.brahmkshatriya.echo.ui.common

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.add
import androidx.fragment.app.commit
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Album
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.EchoMediaItem.Companion.toMediaItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.common.models.Track
import dev.brahmkshatriya.echo.common.models.User
import dev.brahmkshatriya.echo.ui.common.SnackBarHandler.Companion.createSnack
import dev.brahmkshatriya.echo.ui.media.MediaFragment

object FragmentUtils {
    inline fun <reified T : Fragment> Fragment.openFragment(
        view: View? = null, bundle: Bundle? = null,
    ) {
        parentFragmentManager.commit {
            add<T>(id, args = bundle)
            hide(this@openFragment)
            addToBackStack(null)
        }
    }

    inline fun <reified T : Fragment> FragmentActivity.openFragment(
        view: View? = null, bundle: Bundle? = null, cont: Int = R.id.navHostFragment
    ) {
        val oldFragment = supportFragmentManager.findFragmentById(cont)!!
        oldFragment.openFragment<T>(view, bundle)
    }

    inline fun <reified F : Fragment> Fragment.addIfNull(
        id: Int, tag: String, args: Bundle? = null
    ) {
        childFragmentManager.run {
            if (findFragmentByTag(tag) == null) commit {
                add<F>(id, tag, args)
            }
        }
    }

    fun FragmentActivity.openItemFragmentFromUri(uri: Uri) {
        val extensionType = uri.host
        when (extensionType) {
            "music" -> {
                val extensionId = uri.pathSegments.firstOrNull()
                if (extensionId == null) {
                    createSnack("No extension id found")
                    return
                }
                val type = uri.pathSegments.getOrNull(1)
                val id = uri.pathSegments.getOrNull(2)
                if (id == null) {
                    createSnack("No id found")
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
                    createSnack("Invalid item type")
                    return
                }
                openFragment<MediaFragment>(null, MediaFragment.getBundle(extensionId, item, false))
            }

            else -> {
                createSnack("Invalid extension host")
            }
        }
    }
}

