package dev.brahmkshatriya.echo.ui.common

import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.google.android.material.appbar.MaterialToolbar
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.ui.extension.ExtensionsFragmentDirections
import dev.brahmkshatriya.echo.ui.settings.SettingsFragmentDirections
import dev.brahmkshatriya.echo.utils.load
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel

fun MaterialToolbar.configureMainMenu(fragment: Fragment) {
    val extensionViewModel by fragment.activityViewModels<ExtensionViewModel>()

    fragment.observe(extensionViewModel.extensionFlow) { client ->
        client?.metadata?.iconUrl.load(this, R.drawable.ic_extension) {
            menu.findItem(R.id.menu_extensions).icon = it
        }
    }

    setOnMenuItemClickListener {
        when (it.itemId) {
            R.id.menu_settings -> {
                val action = SettingsFragmentDirections.actionSettings()
                findNavController().navigate(action)
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


