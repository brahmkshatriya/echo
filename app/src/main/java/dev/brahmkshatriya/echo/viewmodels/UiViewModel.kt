package dev.brahmkshatriya.echo.viewmodels

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.view.View
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.playback.Current
import dev.brahmkshatriya.echo.ui.settings.LookFragment
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.utils.ui.animateTranslation
import dev.brahmkshatriya.echo.utils.ui.dpToPx
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlin.math.max

@HiltViewModel
class UiViewModel @Inject constructor(
    private val settings: SharedPreferences,
    currentFlow: MutableStateFlow<Current?>,
) : ViewModel() {

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

    val navigation = MutableStateFlow(0)
    val navigationReselected = MutableSharedFlow<Int>()
    val navIds = listOf(
        R.id.homeFragment,
        R.id.searchFragment,
        R.id.libraryFragment
    )
    private val navViewInsets = MutableStateFlow(Insets())
    private val playerNavViewInsets = MutableStateFlow(Insets())
    private val playerInsets = MutableStateFlow(Insets())
    val systemInsets = MutableStateFlow(Insets())
    var isMainFragment = MutableStateFlow(true)

    val combined = systemInsets.combine(navViewInsets) { system, nav ->
        if (isMainFragment.value) system.add(nav) else system
    }.combine(playerInsets) { system, player ->
        system.add(player)
    }

    fun getCombined() = (if (isMainFragment.value) systemInsets.value.add(navViewInsets.value)
    else systemInsets.value).add(playerInsets.value)

    fun setPlayerNavViewInsets(context: Context, isNavVisible: Boolean, isRail: Boolean): Insets {
        val insets = context.resources.run {
            if (!isNavVisible) return@run Insets()
            val height = getDimensionPixelSize(R.dimen.nav_height)
            if (!isRail) return@run Insets(bottom = height)
            val width = getDimensionPixelSize(R.dimen.nav_width)
            if (context.isRTL()) Insets(end = width) else Insets(start = width)
        }
        playerNavViewInsets.value = insets
        return insets
    }

    fun setNavInsets(insets: Insets) {
        navViewInsets.value = insets
    }

    fun setSystemInsets(context: Context, insets: WindowInsetsCompat) {
        val system = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars())
        val inset = system.run {
            if (context.isRTL()) Insets(top, bottom, right, left)
            else Insets(top, bottom, left, right)
        }
        systemInsets.value = inset
    }

    fun setPlayerInsets(context: Context, isVisible: Boolean) {
        val insets = if (isVisible) {
            val height = context.resources.getDimensionPixelSize(R.dimen.collapsed_cover_size)
            Insets(bottom = height + 8.dpToPx(context))
        } else Insets()
        playerInsets.value = insets
    }

    val fromNotification = MutableStateFlow(false)
    val playerSheetState = MutableStateFlow(
        if (currentFlow.value == null) STATE_HIDDEN else STATE_COLLAPSED
    )
    val infoSheetState = MutableStateFlow(STATE_COLLAPSED)
    val playerBgVisibleState = MutableStateFlow(false)

    val playerSheetOffset = MutableStateFlow(
        if (playerSheetState.value == STATE_EXPANDED) 1f else 0f
    )
    val infoSheetOffset = MutableStateFlow(
        if (infoSheetState.value == STATE_EXPANDED) 1f else 0f
    )

    private var playerBackPressCallback: OnBackPressedCallback? = null
    private var infoBackPressCallback: OnBackPressedCallback? = null
    fun backPressCallback() = object : OnBackPressedCallback(false) {
        val backPress
            get() = infoBackPressCallback ?: playerBackPressCallback

        override fun handleOnBackStarted(backEvent: BackEventCompat) {
            if (settings.getBoolean(LookFragment.ANIMATIONS_KEY, true))
                backPress?.handleOnBackStarted(backEvent)
        }

        override fun handleOnBackProgressed(backEvent: BackEventCompat) {
            if (settings.getBoolean(LookFragment.ANIMATIONS_KEY, true))
                backPress?.handleOnBackProgressed(backEvent)
        }

        override fun handleOnBackPressed() {
            backPress?.handleOnBackPressed()
        }

        override fun handleOnBackCancelled() {
            if (settings.getBoolean(LookFragment.ANIMATIONS_KEY, true))
                backPress?.handleOnBackCancelled()
        }
    }

    fun collapsePlayer() {
        if (playerSheetState.value == STATE_EXPANDED) {
            changePlayerState(STATE_COLLAPSED)
            changeInfoState(STATE_COLLAPSED)
        }
    }

    private var playerBehaviour = WeakReference<BottomSheetBehavior<View>>(null)
    fun changePlayerState(state: Int) {
        val behavior = playerBehaviour.get() ?: return
        if (state == STATE_HIDDEN) behavior.isHideable = true
        playerSheetState.value = state
        playerSheetOffset.value = if (state == STATE_EXPANDED) 1f else 0f
        behavior.state = state
    }

    private var infoBehaviour = WeakReference<BottomSheetBehavior<View>>(null)
    fun changeInfoState(stateCollapsed: Int) {
        val behavior = infoBehaviour.get() ?: return
        infoSheetState.value = stateCollapsed
        behavior.state = stateCollapsed
    }

    fun changeBgVisible(show: Boolean) {
        playerBgVisibleState.value = show
        if (!show && infoSheetState.value == STATE_EXPANDED)
            changeInfoState(STATE_COLLAPSED)
    }

    companion object {
        fun Context.isRTL() =
            resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

        fun Context.isLandscape() =
            resources.configuration.orientation == ORIENTATION_LANDSCAPE

        fun Context.isNightMode() =
            resources.configuration.uiMode and UI_MODE_NIGHT_MASK != UI_MODE_NIGHT_NO

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
                    top = insets.top,
                    start = insets.start,
                    end = insets.end
                )

                uiViewModel.block(insets)
            }
        }

        fun View.applyContentInsets(insets: Insets, paddingDp: Int = 8) {
            val verticalPadding = paddingDp.dpToPx(context)
            updatePaddingRelative(
                top = verticalPadding,
                bottom = insets.bottom + verticalPadding,
                start = insets.start,
                end = insets.end
            )
        }

        fun View.applyInsets(it: Insets, paddingDp: Int = 0) {
            val padding = paddingDp.dpToPx(context)
            updatePaddingRelative(
                top = it.top + padding,
                bottom = it.bottom + padding,
                start = it.start + padding,
                end = it.end + padding,
            )
        }

        fun View.applyFabInsets(it: Insets, system: Insets, paddingDp: Int = 0) {
            val padding = paddingDp.dpToPx(context)
            updatePaddingRelative(
                bottom = it.bottom - system.bottom + padding,
                start = it.start + padding,
                end = it.end + padding,
            )
        }

        fun Fragment.applyBackPressCallback(callback: ((Int) -> Unit)? = null) {
            val activity = requireActivity()
            val viewModel by activity.viewModels<UiViewModel>()
            val backPress = viewModel.backPressCallback()
            observe(viewModel.playerSheetState) {
                backPress.isEnabled = it == STATE_EXPANDED
                callback?.invoke(it)
            }
            activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPress)
        }

        private fun BottomSheetBehavior<View>.backPressCallback() =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackStarted(backEvent: BackEventCompat) =
                    startBackProgress(backEvent)

                override fun handleOnBackProgressed(backEvent: BackEventCompat) =
                    updateBackProgress(backEvent)

                override fun handleOnBackPressed() =
                    handleBackInvoked()

                override fun handleOnBackCancelled() =
                    cancelBackProgress()
            }

        fun LifecycleOwner.setupPlayerBehavior(viewModel: UiViewModel, view: View) {
            val behavior = BottomSheetBehavior.from(view)
            viewModel.playerBehaviour = WeakReference(behavior)
            observe(viewModel.infoSheetState) { behavior.isDraggable = it == STATE_COLLAPSED }
            viewModel.playerBackPressCallback = behavior.backPressCallback()

            val combined =
                viewModel.run { playerNavViewInsets.combine(systemInsets) { nav, _ -> nav } }
            observe(combined) {
                val bottomPadding = 8.dpToPx(view.context)
                val collapsedCoverSize =
                    view.resources.getDimensionPixelSize(R.dimen.collapsed_cover_size) + bottomPadding
                val peekHeight =
                    view.resources.getDimensionPixelSize(R.dimen.bottom_player_peek_height)
                val height = if (it.bottom == 0) collapsedCoverSize else peekHeight
                val newHeight = viewModel.systemInsets.value.bottom + height
                behavior.peekHeight = newHeight
                if (viewModel.playerSheetState.value != STATE_HIDDEN)
                    animateTranslation(view, behavior.peekHeight, newHeight)
            }

            runCatching { behavior.state = viewModel.playerSheetState.value }
            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    val expanded = newState == STATE_EXPANDED
                    behavior.isHideable = !expanded
                    viewModel.playerSheetState.value = newState
                    viewModel.setPlayerInsets(view.context, newState != STATE_HIDDEN)
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    viewModel.playerSheetOffset.value = slideOffset
                }
            })
            viewModel.playerSheetOffset.value = if (behavior.state == STATE_EXPANDED) 1f else 0f
            viewModel.run {
                observe(fromNotification) {
                    if (!it) return@observe
                    fromNotification.value = false
                    changePlayerState(STATE_EXPANDED)
                    changeInfoState(STATE_COLLAPSED)
                }
            }
        }

        fun setupPlayerInfoBehavior(viewModel: UiViewModel, view: View) {
            val behavior = BottomSheetBehavior.from(view)
            viewModel.infoBehaviour = WeakReference(behavior)

            behavior.state = viewModel.infoSheetState.value
            val backPress = behavior.backPressCallback()
            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    viewModel.infoSheetState.value = newState
                    viewModel.infoBackPressCallback =
                        backPress.takeIf { newState != STATE_COLLAPSED }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    val offset = max(0f, slideOffset)
                    viewModel.infoSheetOffset.value = offset
                }
            })
        }
    }
}