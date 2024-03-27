package dev.brahmkshatriya.echo.newui

import androidx.recyclerview.widget.RecyclerView
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.ui.adapters.ClientLoadingAdapter
import dev.brahmkshatriya.echo.ui.adapters.ClientNotSupportedAdapter

inline fun <reified T> getAdapterForExtension(
    it: ExtensionClient?,
    name: Int,
    adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>,
    block: ((T?) -> Unit) = {}
): RecyclerView.Adapter<out RecyclerView.ViewHolder> {
    return if (it != null) {
        if (it is T) {
            block(it)
            adapter
        } else {
            block(null)
            ClientNotSupportedAdapter(name)
        }
    } else {
        block(null)
        ClientLoadingAdapter()
    }
}