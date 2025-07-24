package dev.brahmkshatriya.echo.ui.media

import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.Artist
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.common.models.Playlist
import dev.brahmkshatriya.echo.databinding.FragmentMediaBinding
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyFabInsets
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyGradient
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyInsets
import dev.brahmkshatriya.echo.ui.feed.viewholders.MediaViewHolder.Companion.icon
import dev.brahmkshatriya.echo.ui.feed.viewholders.MediaViewHolder.Companion.placeHolder
import dev.brahmkshatriya.echo.ui.media.more.MediaMoreBottomSheet
import dev.brahmkshatriya.echo.ui.playlist.delete.DeletePlaylistBottomSheet
import dev.brahmkshatriya.echo.ui.playlist.edit.EditPlaylistFragment
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.Serializer.getSerialized
import dev.brahmkshatriya.echo.utils.Serializer.putSerialized
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadInto
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadWithThumb
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.UiUtils.configureAppBar
import dev.brahmkshatriya.echo.utils.ui.UiUtils.dpToPx
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class MediaFragment : Fragment(R.layout.fragment_media), MediaDetailsFragment.Parent {
    companion object {
        fun getBundle(extensionId: String, item: EchoMediaItem, loaded: Boolean) = Bundle().apply {
            putString("extensionId", extensionId)
            putSerialized("item", item)
            putBoolean("loaded", loaded)
        }
    }

    val args by lazy { requireArguments() }
    val extensionId by lazy { args.getString("extensionId")!! }
    val item by lazy { args.getSerialized<EchoMediaItem>("item")!! }
    val loaded by lazy { args.getBoolean("loaded") }

    override val fromPlayer = false
    override val feedId by lazy { item.id }

    override val viewModel by viewModel<MediaViewModel> {
        parametersOf(true, extensionId, item, loaded, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentMediaBinding.bind(view)
        setupTransition(view)
        applyBackPressCallback()
        binding.appBarLayout.configureAppBar { offset ->
            binding.appbarOutline.alpha = offset
            binding.coverContainer.alpha = 1 - offset
            binding.endIcon.alpha = 1 - offset
        }
        binding.toolBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.toolBar.setOnMenuItemClickListener {
            val item = viewModel.itemResultFlow.value?.getOrNull()?.item ?: item
            MediaMoreBottomSheet.newInstance(
                id, extensionId, item, !viewModel.isRefreshing
            ).show(childFragmentManager, null)
            true
        }
        applyInsets {
            binding.fabContainer.applyFabInsets(it, systemInsets.value)
        }

        observe(viewModel.itemResultFlow) { result ->
            val item = result?.getOrNull()?.item ?: item
            binding.toolBar.title = item.title.trim()
            binding.endIcon.setImageResource(item.icon)
            if (item is Artist) binding.coverContainer.run {
                val maxWidth = 240.dpToPx(context)
                radius = maxWidth.toFloat()
                updateLayoutParams<ConstraintLayout.LayoutParams> {
                    matchConstraintMaxWidth = maxWidth
                }
            }
            item.cover.loadInto(binding.cover, null, item.placeHolder)
            item.background.loadWithThumb(view) { applyGradient(view, it) }
            val isEditable = (result?.getOrNull()?.item as? Playlist)?.isEditable ?: false
            binding.fabEditPlaylist.isVisible = isEditable
            binding.fabEditPlaylist.setOnClickListener {
                val playlist = item as? Playlist ?: return@setOnClickListener
                openFragment<EditPlaylistFragment>(
                    it, EditPlaylistFragment.getBundle(extensionId, playlist, loaded)
                )
            }
        }
        parentFragmentManager.setFragmentResultListener("reload", this) { _, data ->
            if (data.getString("id") == item.id) viewModel.refresh()
        }
        parentFragmentManager.setFragmentResultListener("delete", this) { _, data ->
            val playlist = item as? Playlist ?: return@setFragmentResultListener
            DeletePlaylistBottomSheet.show(
                this, extensionId, playlist, !viewModel.isRefreshing
            )
        }
        parentFragmentManager.setFragmentResultListener("deleted", this) { _, data ->
            if (data.getString("id") == item.id) parentFragmentManager.popBackStack()
        }
    }
}