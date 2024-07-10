package dev.brahmkshatriya.echo.utils

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

fun SwipeRefreshLayout.configure(block:()->Unit){
    setProgressViewOffset(true, 0, 64.dpToPx(context))
    setOnRefreshListener(block)
}