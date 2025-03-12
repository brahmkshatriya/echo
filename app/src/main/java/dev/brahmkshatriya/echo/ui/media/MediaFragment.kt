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
import com.google.android.material.color.MaterialColors
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.models.EchoMediaItem
import dev.brahmkshatriya.echo.databinding.FragmentMediaBinding
import dev.brahmkshatriya.echo.ui.UiViewModel
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyFabInsets
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyInsets
import dev.brahmkshatriya.echo.ui.media.more.MediaMoreBottomSheet
import dev.brahmkshatriya.echo.ui.player.PlayerColors
import dev.brahmkshatriya.echo.ui.player.PlayerViewModel
import dev.brahmkshatriya.echo.ui.shelf.adapter.MediaItemViewHolder.Companion.icon
import dev.brahmkshatriya.echo.ui.shelf.adapter.MediaItemViewHolder.Companion.placeHolder
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
    private val uiViewModel by activityViewModel<UiViewModel>()
    private val vm by viewModel<MediaViewModel> { parametersOf(extensionId, item, loaded) }

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
                        id, extensionId, vm.itemFlow.value, vm.isLoaded, false
                    ).show(parentFragmentManager, null)
                    true
                }

                else -> false
            }
        }

        FastScrollerHelper.applyTo(binding.recyclerView)
        observe(vm.itemFlow) { item ->
            if (item is EchoMediaItem.Profile) binding.coverContainer.run {
                val maxWidth = 240.dpToPx(context)
                radius = maxWidth.toFloat()
                updateLayoutParams<ConstraintLayout.LayoutParams> {
                    matchConstraintMaxWidth = maxWidth
                }
            }
            item.cover.loadWithThumb(binding.cover, null, item.placeHolder) {
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
            binding.toolBar.title = item.title
            binding.endIcon.setImageResource(item.icon)
        }
    }
}