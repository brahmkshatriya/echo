package dev.brahmkshatriya.echo.ui.media

import android.os.Bundle
import androidx.fragment.app.Fragment
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.utils.Serializer.getSerialized
import dev.brahmkshatriya.echo.utils.Serializer.putSerialized

class MediaFragment : Fragment() {
    companion object {
        fun getBundle(extension: String, item: EchoMediaItem, loaded: Boolean) = Bundle().apply {
            putString("extensionId", extension)
            putSerialized("item", item)
            putBoolean("loaded", loaded)
        }
    }

    private val args by lazy { requireArguments() }
    private val extensionId by lazy { args.getString("extensionId")!! }
    private val item by lazy { args.getSerialized<EchoMediaItem>("item")!! }
    private val loaded by lazy { args.getBoolean("loaded") }
}