package dev.brahmkshatriya.echo.ui.adapters

interface ClickListener<T> {
    fun onClick(item: T)
    fun onLongClick(item: T)
}