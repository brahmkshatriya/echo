package dev.brahmkshatriya.echo.utils.ui

import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView


object FastScrollerHelper {
    fun applyTo(view: RecyclerView) {

//        = FastScrollerBuilder(view).apply {
//            useMd2Style()
//            val pad = 8.dpToPx(view.context)
//            setPadding(pad, pad, pad, pad)
        view.isVerticalScrollBarEnabled = false
//        }.build()
    }

    fun applyTo(view: NestedScrollView) {
//        = FastScrollerBuilder(view).apply {
//            useMd2Style()
//            val pad = 8.dpToPx(view.context)
//            setPadding(pad, pad, pad, pad)
        view.isVerticalScrollBarEnabled = false
//        }.build()
    }
}
