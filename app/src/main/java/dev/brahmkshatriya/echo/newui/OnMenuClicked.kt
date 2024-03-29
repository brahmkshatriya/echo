package dev.brahmkshatriya.echo.newui

import androidx.navigation.findNavController
import com.google.android.material.appbar.MaterialToolbar
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.newui.extension.ExtensionsFragmentDirections

fun MaterialToolbar.onMenuClicked() {
    setOnMenuItemClickListener {
        when (it.itemId) {
            R.id.menu_settings -> {
                true
            }
            R.id.menu_extensions -> {
                val action = ExtensionsFragmentDirections.actionExtensions()
                findNavController().navigate(action)
                true
            }
            else -> false
        }
    }
}