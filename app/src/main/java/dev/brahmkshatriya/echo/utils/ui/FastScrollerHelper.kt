package dev.brahmkshatriya.echo.utils.ui

import android.view.View
import android.view.animation.Interpolator
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.recyclerview.widget.RecyclerView
import me.zhanghai.android.fastscroll.FastScrollNestedScrollView
import me.zhanghai.android.fastscroll.FastScroller.AnimationHelper
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import kotlin.math.max


class FastScrollerHelper(private val mView: View) : AnimationHelper {
    private var mScrollbarAutoHideEnabled = true
    private var mShowingScrollbar = true
    private var mShowingPopup = false
    override fun showScrollbar(trackView: View, thumbView: View) {
        if (!mShowingScrollbar && trackView.isPressed) {
            mShowingScrollbar = true
            trackView.animate().alpha(1.0f).translationX(0.0f)
                .setDuration(SHOW_DURATION_MILLIS).setInterpolator(SHOW_SCROLLBAR_INTERPOLATOR)
                .start()
            thumbView.animate().alpha(1.0f).translationX(0.0f)
                .setDuration(SHOW_DURATION_MILLIS).setInterpolator(SHOW_SCROLLBAR_INTERPOLATOR)
                .start()
        }
    }

    override fun hideScrollbar(trackView: View, thumbView: View) {
        if (mShowingScrollbar) {
            mShowingScrollbar = false
            val isLayoutRtl = mView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL
            val width = max(trackView.width.toDouble(), thumbView.width.toDouble()).toInt()
            val translationX: Float = if (isLayoutRtl) {
                if (trackView.left == 0) (-width).toFloat() else 0.0f
            } else {
                if (trackView.right == mView.width) width.toFloat() else 0.0f
            }
            trackView.animate().alpha(0.0f).translationX(translationX)
                .setDuration(HIDE_DURATION_MILLIS).setInterpolator(HIDE_SCROLLBAR_INTERPOLATOR)
                .start()
            thumbView.animate().alpha(0.0f).translationX(translationX)
                .setDuration(HIDE_DURATION_MILLIS).setInterpolator(HIDE_SCROLLBAR_INTERPOLATOR)
                .start()
        }
    }

    override fun isScrollbarAutoHideEnabled(): Boolean {
        return mScrollbarAutoHideEnabled
    }

    override fun getScrollbarAutoHideDelayMillis() = AUTO_HIDE_SCROLLBAR_DELAY_MILLIS

    override fun showPopup(popupView: View) {
        if (!mShowingPopup) {
            mShowingPopup = true
            popupView.animate().alpha(1.0f).setDuration(SHOW_DURATION_MILLIS).start()
        }
    }

    override fun hidePopup(popupView: View) {
        if (mShowingPopup) {
            mShowingPopup = false
            popupView.animate().alpha(0.0f).setDuration(HIDE_DURATION_MILLIS).start()
        }
    }

    companion object {
        private const val SHOW_DURATION_MILLIS = 150L
        private const val HIDE_DURATION_MILLIS = 200L
        private val SHOW_SCROLLBAR_INTERPOLATOR: Interpolator = LinearOutSlowInInterpolator()
        private val HIDE_SCROLLBAR_INTERPOLATOR: Interpolator = FastOutLinearInInterpolator()
        private const val AUTO_HIDE_SCROLLBAR_DELAY_MILLIS = 500

        fun applyTo(view: RecyclerView) = FastScrollerBuilder(view).apply {
            useMd2Style()
            setAnimationHelper(FastScrollerHelper(view))
            view.isVerticalScrollBarEnabled = false
        }.build()

        fun applyTo(view: FastScrollNestedScrollView) = FastScrollerBuilder(view).apply {
            useMd2Style()
            setAnimationHelper(FastScrollerHelper(view))
            view.isVerticalScrollBarEnabled = false
        }.build()
    }
}
