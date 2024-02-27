package dev.brahmkshatriya.echo.ui

interface ClickListener<T> {
    fun onClick(item: T)
    fun onLongClick(item: T)
}