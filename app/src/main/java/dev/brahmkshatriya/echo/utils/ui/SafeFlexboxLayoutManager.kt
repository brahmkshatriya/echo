package dev.brahmkshatriya.echo.utils.ui

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager

class SafeFlexboxLayoutManager(context: Context) : FlexboxLayoutManager(context) {

    override fun generateLayoutParams(lp: ViewGroup.LayoutParams): RecyclerView.LayoutParams {
        return LayoutParams(lp)
    }
}