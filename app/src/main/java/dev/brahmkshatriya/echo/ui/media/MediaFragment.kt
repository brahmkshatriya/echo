package dev.brahmkshatriya.echo.ui.media

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ConcatAdapter
import com.google.android.material.color.MaterialColors
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.databinding.FragmentMediaBinding
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyFabInsets
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyInsets
import dev.brahmkshatriya.echo.ui.media.more.MediaMoreBottomSheet
import dev.brahmkshatriya.echo.ui.player.PlayerColors
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.ui.shelf.adapter.MediaItemViewHolder.Companion.icon
import dev.brahmkshatriya.echo.ui.shelf.adapter.MediaItemViewHolder.Companion.placeHolder
import dev.brahmkshatriya.echo.ui.shelf.adapter.ShelfAdapter.Companion.getShelfAdapter
import dev.brahmkshatriya.echo.ui.shelf.adapter.ShelfClickListener
import dev.brahmkshatriya.echo.utils.ContextUtils.getSettings
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.Serializer.getSerialized
import dev.brahmkshatriya.echo.utils.Serializer.putSerialized
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadWithThumb
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import dev.brahmkshatriya.echo.utils.ui.FastScrollerHelper
import dev.brahmkshatriya.echo.utils.ui.GradientDrawable
import dev.brahmkshatriya.echo.utils.ui.GradientDrawable.BACKGROUND_GRADIENT
import dev.brahmkshatriya.echo.utils.ui.UiUtils.dpToPx
import dev.brahmkshatriya.echo.utils.ui.UiUtils.onAppBarChangeListener
import kotlinx.coroutines.flow.combine
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

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

    private var binding by autoCleared<FragmentMediaBinding>()
    private val playerViewModel by activityViewModel<PlayerViewModel>()
    private val vm by viewModel<MediaViewModel> { parametersOf(extensionId, item, loaded, true) }

    private val headerListener by lazy { MediaHeaderAdapter.getListener(this) }
    private val headerAdapter by lazy { MediaHeaderAdapter(headerListener) }

    private val listener by lazy { ShelfClickListener(requireActivity().supportFragmentManager) }
    private val shelfAdapter by lazy { getShelfAdapter(listener) }

    private val listAdapter by lazy { TrackAdapter(listener) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentMediaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val settings = requireContext().getSettings()
        setupTransition(view)
        applyInsets {
            binding.recyclerView.applyContentInsets(it)
            binding.fabContainer.applyFabInsets(it, systemInsets.value)
        }
        applyBackPressCallback()
        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.appbarOutline.alpha = offset
            binding.coverContainer.alpha = 1 - offset
            binding.endIcon.alpha = 1 - offset
        }

        binding.toolBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.toolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_more -> {
                    MediaMoreBottomSheet.newInstance(
                        id, extensionId, vm.itemFlow.value, vm.isLoading, false
                    ).show(parentFragmentManager, null)
                    true
                }

                else -> false
            }
        }

        FastScrollerHelper.applyTo(binding.recyclerView)
        observe(playerViewModel.playerState.current) {
            listAdapter.onCurrentChanged(it)
            shelfAdapter.onCurrentChanged(it)
        }
        observe(vm.itemFlow) { item ->
            if (item is EchoMediaItem.Profile) binding.coverContainer.run {
                val maxWidth = 240.dpToPx(context)
                radius = maxWidth.toFloat()
                updateLayoutParams<ConstraintLayout.LayoutParams> {
                    matchConstraintMaxWidth = maxWidth
                }
            }
            item.cover.loadWithThumb(view, null, item.placeHolder) {
                binding.cover.setImageDrawable(it)
                val color = PlayerColors.getDominantColor(it?.toBitmap().takeIf {
                    settings.getBoolean(BACKGROUND_GRADIENT, true)
                })
                val (appBar, collapsing) = if (color != null) Color.TRANSPARENT to Color.TRANSPARENT
                else Pair(
                    MaterialColors.getColor(view, R.attr.echoBackground),
                    MaterialColors.getColor(view, R.attr.navBackground)
                )
                binding.appBarLayout.setBackgroundColor(appBar)
                binding.collapsingToolbar.setContentScrimColor(collapsing)
                view.background = GradientDrawable.createBg(
                    view, color ?: MaterialColors.getColor(view, R.attr.echoBackground)
                )
            }
            binding.toolBar.title = item.title.trim()
            binding.endIcon.setImageResource(item.icon)
        }

        binding.swipeRefresh.setOnRefreshListener { vm.refresh(true) }
        binding.swipeRefresh.isRefreshing = vm.isLoading
        observe(vm.loadingFlow) {
            binding.swipeRefresh.isRefreshing = it
        }

        observe(vm.run { itemFlow.combine(extensionFlow) { a, b -> a to b } }) { (item, ext) ->
            headerAdapter.submit(ext?.id, item, ext?.instance?.value()?.getOrNull())
        }

        observe(vm.tracks) { (extension, data, tracks) ->
            listAdapter.submit(extension?.id, vm.itemFlow.value, data, tracks)
        }

        observe(vm.feed) { (extension, shelf, feed) ->
            shelfAdapter.submit(extension?.id, shelf, feed)
        }

        binding.recyclerView.adapter = ConcatAdapter(
            headerAdapter,
            listAdapter.withHeaders(this),
            shelfAdapter.withHeaders(this)
        )
    }
}