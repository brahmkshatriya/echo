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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_DRAGGING
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_SETTLING
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.utils.Animator.animatePeekHeight
import dev.brahmkshatriya.echo.utils.dpToPx
import dev.brahmkshatriya.echo.utils.observe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
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
    private val playerInsets = MutableStateFlow(Insets())
    private val systemInsets = MutableStateFlow(Insets())

    val combined = systemInsets.combine(navViewInsets) { system, nav ->
        system.add(nav)
    }.combine(playerInsets) { system, player ->
        system.add(player)
    }

    fun setNavInsets(context: Context, isNavVisible: Boolean, isRail: Boolean) {
        context.resources.run {
            val insets = if (isNavVisible) {
                if (isRail) {
                    val width = getDimensionPixelSize(R.dimen.nav_width)
                    if (context.isRTL()) Insets(end = width)
                    else Insets(start = width)
                } else Insets(bottom = getDimensionPixelSize(R.dimen.nav_height))
            } else Insets()
            navViewInsets.value = insets
        }
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
        println("setPlayerInsets: $insets")
        playerInsets.value = insets
    }


    val playerSheetState = MutableStateFlow(STATE_HIDDEN)
    val infoSheetState = MutableStateFlow(STATE_COLLAPSED)
    val playerSheetOffset = MutableStateFlow(0f)
    val infoSheetOffset = MutableStateFlow(0f)

    val startBackProgress = MutableSharedFlow<BackEventCompat>()
    val updateBackProgress = MutableSharedFlow<BackEventCompat>()
    val handleBackInvoked = MutableSharedFlow<Unit>()
    val cancelBackProgress = MutableSharedFlow<Unit>()

    private fun observeBack(
        behavior: BottomSheetBehavior<View>,
        block: () -> Boolean,
    ) {
        fun <T : Any> observe(flow: Flow<T>, function: (T) -> Unit) = viewModelScope.launch {
            flow.collect { if (block()) function(it) }
        }
        observe(startBackProgress) { behavior.startBackProgress(it) }
        observe(updateBackProgress) { behavior.updateBackProgress(it) }
        observe(handleBackInvoked) { behavior.handleBackInvoked() }
        observe(cancelBackProgress) { behavior.cancelBackProgress() }
    }

    fun setupPlayerBehavior(view: View) {
        val behavior = BottomSheetBehavior.from(view)
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                val expanded = newState == STATE_EXPANDED
                behavior.isHideable = !expanded
                if (newState == STATE_SETTLING || newState == STATE_DRAGGING) return
                playerSheetState.value = newState
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                val offset = max(0f, slideOffset)
                playerSheetOffset.value = offset
            }
        })
        viewModelScope.launch {
            infoSheetState.collect { behavior.isDraggable = it == STATE_COLLAPSED }
        }
        observeBack(behavior) { infoSheetState.value != STATE_EXPANDED }
        viewModelScope.launch {
            navViewInsets.combine(systemInsets) { nav, _ -> nav }.collect {
                val collapsedCoverSize =
                    view.resources.getDimensionPixelSize(R.dimen.collapsed_cover_size)
                val peekHeight =
                    view.resources.getDimensionPixelSize(R.dimen.bottom_player_peek_height)
                val newHeight =
                    systemInsets.value.bottom + if (it.bottom == 0) collapsedCoverSize else peekHeight
                behavior.animatePeekHeight(view, newHeight)
            }
        }
        behavior.state = playerSheetState.value
    }

    fun setupPlayerInfoBehavior(view: View) {
        val behavior = BottomSheetBehavior.from(view)
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                infoSheetState.value = newState
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                val offset = max(0f, slideOffset)
                infoSheetOffset.value = offset
            }
        })
        observeBack(behavior) { behavior.state == STATE_EXPANDED }
    }

    fun backPress() = object : OnBackPressedCallback(false) {
        private fun <T> emit(flow: MutableSharedFlow<T>, value: T) {
            viewModelScope.launch { flow.emit(value) }
        }

        override fun handleOnBackStarted(backEvent: BackEventCompat) =
            emit(startBackProgress, backEvent)

        override fun handleOnBackProgressed(backEvent: BackEventCompat) =
            emit(updateBackProgress, backEvent)

        override fun handleOnBackPressed() =
            emit(handleBackInvoked, Unit)

        override fun handleOnBackCancelled() =
            emit(cancelBackProgress, Unit)
    }

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
                val verticalPadding = 8.dpToPx(requireContext())
                child.updatePaddingRelative(
                    top = verticalPadding,
                    bottom = insets.bottom + verticalPadding,
                    start = insets.start,
                    end = insets.end
                )
                appBar.updatePaddingRelative(
                    start = insets.start,
                    end = insets.end
                )
                uiViewModel.block(insets)
            }
        }

        fun Fragment.applyBackPressCallback(callback: ((Int) -> Unit)? = null) {
            val activity = requireActivity()
            val viewModel by activity.viewModels<UiViewModel>()
            val backPress = viewModel.backPress()
            observe(viewModel.playerSheetState) {
                backPress.isEnabled = it == STATE_EXPANDED
                callback?.invoke(it)
            }
            activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPress)
        }
    }
}