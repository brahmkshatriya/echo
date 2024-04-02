package dev.brahmkshatriya.echo.viewmodels

import android.content.Context
import android.view.View
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_DRAGGING
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_SETTLING
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.utils.Animator.animatePeekHeight
import dev.brahmkshatriya.echo.utils.dpToPx
import dev.brahmkshatriya.echo.utils.emit
import dev.brahmkshatriya.echo.utils.observe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlin.math.max

class UiViewModel : ViewModel() {
    data class Insets(
        val top: Int = 0,
        val bottom: Int = 0,
        val start: Int = 0,
        val end: Int = 0
    ) {
        fun add(vararg insets: Insets) = insets.fold(this) { acc, it ->
            Insets(
                acc.top + it.top,
                acc.bottom + it.bottom,
                acc.start + it.start,
                acc.end + it.end
            )
        }
    }

    private val navViewInsets = MutableStateFlow(Insets())
    private val playerNavViewInsets = MutableStateFlow(Insets())
    private val playerInsets = MutableStateFlow(Insets())
    private val systemInsets = MutableStateFlow(Insets())

    val combined = systemInsets.combine(navViewInsets) { system, nav ->
        system.add(nav)
    }.combine(playerInsets) { system, player ->
        system.add(player)
    }

    fun setPlayerNavViewInsets(context: Context, isNavVisible: Boolean, isRail: Boolean): Insets {
        val insets = context.resources.run {
            if (isNavVisible) {
                if (isRail) {
                    val width = getDimensionPixelSize(R.dimen.nav_width)
                    if (context.isRTL()) Insets(end = width)
                    else Insets(start = width)
                } else Insets(bottom = getDimensionPixelSize(R.dimen.nav_height))
            } else Insets()
        }
        playerNavViewInsets.value = insets
        return insets
    }

    fun setNavInsets(insets: Insets) {
        navViewInsets.value = insets
    }

    fun setSystemInsets(context: Context, insets: WindowInsetsCompat) {
        val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val inset = system.run {
            if (context.isRTL()) Insets(top, bottom, right, left)
            else Insets(top, bottom, left, right)
        }
        systemInsets.value = inset
    }

    fun setPlayerInsets(context: Context, isVisible: Boolean) {
        val insets = if (isVisible) {
            val height = context.resources.getDimensionPixelSize(R.dimen.collapsed_cover_size)
            Insets(bottom = height)
        } else Insets()
        playerInsets.value = insets
    }

    val fromNotification: MutableSharedFlow<Boolean> = MutableSharedFlow()

    val playerSheetState = MutableStateFlow(STATE_COLLAPSED)
    val infoSheetState = MutableStateFlow(STATE_COLLAPSED)

    val playerSheetOffset = MutableStateFlow(0f)
    val infoSheetOffset = MutableStateFlow(0f)

    val startBackProgress = MutableSharedFlow<BackEventCompat>()
    val updateBackProgress = MutableSharedFlow<BackEventCompat>()
    val handleBackInvoked = MutableSharedFlow<Unit>()
    val cancelBackProgress = MutableSharedFlow<Unit>()

    companion object {
        fun Context.isRTL() =
            resources.configuration.layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL

        fun Fragment.applyInsets(block: UiViewModel.(Insets) -> Unit) {
            val uiViewModel by activityViewModels<UiViewModel>()
            observe(uiViewModel.combined) { uiViewModel.block(it) }
        }

        fun Fragment.applyInsetsMain(
            appBar: View,
            child: View,
            block: UiViewModel.(Insets) -> Unit = {}
        ) {
            val uiViewModel by activityViewModels<UiViewModel>()
            observe(uiViewModel.combined) { insets ->
                child.applyContentInsets(insets)
                appBar.updatePaddingRelative(
                    start = insets.start,
                    end = insets.end
                )
                uiViewModel.block(insets)
            }
        }

        fun View.applyContentInsets(insets: Insets) {
            val verticalPadding = 8.dpToPx(context)
            updatePaddingRelative(
                top = verticalPadding,
                bottom = insets.bottom + verticalPadding,
                start = insets.start,
                end = insets.end
            )
        }

        private fun LifecycleOwner.backPress(viewModel: UiViewModel) =
            object : OnBackPressedCallback(false) {
                override fun handleOnBackStarted(backEvent: BackEventCompat) =
                    emit(viewModel.startBackProgress) { backEvent }

                override fun handleOnBackProgressed(backEvent: BackEventCompat) =
                    emit(viewModel.updateBackProgress) { backEvent }

                override fun handleOnBackPressed() =
                    emit(viewModel.handleBackInvoked) {}

                override fun handleOnBackCancelled() =
                    emit(viewModel.cancelBackProgress) {}
            }

        fun Fragment.applyBackPressCallback(callback: ((Int) -> Unit)? = null) {
            val activity = requireActivity()
            val viewModel by activity.viewModels<UiViewModel>()
            val backPress = backPress(viewModel)
            observe(viewModel.playerSheetState) {
                backPress.isEnabled = it == STATE_EXPANDED
                callback?.invoke(it)
            }
            activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPress)
        }

        private fun LifecycleOwner.observeBack(
            behavior: BottomSheetBehavior<View>,
            viewModel: UiViewModel,
            block: () -> Boolean,
        ) {
            fun <T : Any> observeIf(flow: Flow<T>, function: (T) -> Unit) =
                observe(flow) { if (block()) function(it) }
            viewModel.run {
                observeIf(startBackProgress) { behavior.startBackProgress(it) }
                observeIf(updateBackProgress) { behavior.updateBackProgress(it) }
                observeIf(handleBackInvoked) { behavior.handleBackInvoked() }
                observeIf(cancelBackProgress) { behavior.cancelBackProgress() }
            }
        }

        fun LifecycleOwner.setupPlayerBehavior(viewModel: UiViewModel, view: View) {
            val behavior = BottomSheetBehavior.from(view)
            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    val expanded = newState == STATE_EXPANDED
                    behavior.isHideable = !expanded
                    if (newState == STATE_SETTLING || newState == STATE_DRAGGING) return
                    viewModel.playerSheetState.value = newState
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    val offset = max(0f, slideOffset)
                    viewModel.playerSheetOffset.value = offset
                }
            })
            observe(viewModel.infoSheetState) { behavior.isDraggable = it == STATE_COLLAPSED }
            observeBack(behavior, viewModel) { viewModel.infoSheetState.value != STATE_EXPANDED }
            val combined = viewModel.run {
                playerNavViewInsets.combine(systemInsets) { nav, _ -> nav }
            }
            observe(combined) {
                if (behavior.state == STATE_HIDDEN) return@observe
                val collapsedCoverSize =
                    view.resources.getDimensionPixelSize(R.dimen.collapsed_cover_size)
                val peekHeight =
                    view.resources.getDimensionPixelSize(R.dimen.bottom_player_peek_height)
                val height = if (it.bottom == 0) collapsedCoverSize else peekHeight
                val newHeight = viewModel.systemInsets.value.bottom + height
                behavior.animatePeekHeight(view, newHeight)
            }
            behavior.state = viewModel.playerSheetState.value
            observe(viewModel.fromNotification) {
                if (it) behavior.state = STATE_EXPANDED
            }
        }

        fun LifecycleOwner.setupPlayerInfoBehavior(viewModel: UiViewModel, view: View) {
            val behavior = BottomSheetBehavior.from(view)
            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    viewModel.infoSheetState.value = newState
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    val offset = max(0f, slideOffset)
                    viewModel.infoSheetOffset.value = offset
                }
            })
            observeBack(behavior, viewModel) { behavior.state == STATE_EXPANDED }
        }
    }
}