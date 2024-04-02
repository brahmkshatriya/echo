package dev.brahmkshatriya.echo.ui.common

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import com.google.android.material.appbar.MaterialToolbar
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.ui.extension.ExtensionsFragmentDirections
import dev.brahmkshatriya.echo.ui.settings.SettingsFragmentDirections
import dev.brahmkshatriya.echo.utils.load
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel

fun MaterialToolbar.configureMainMenu(fragment: Fragment) {
    val extensionViewModel by fragment.activityViewModels<ExtensionViewModel>()

    val settings = findViewById<View>(R.id.menu_settings)
    settings.transitionName = "settings"
    val extensions = findViewById<View>(R.id.menu_extensions)
    extensions.transitionName = "extensions"

    fragment.observe(extensionViewModel.extensionFlow) { client ->
        client?.metadata?.iconUrl.load(extensions, R.drawable.ic_extension) {
            menu.findItem(R.id.menu_extensions).icon = it
        }
    }
    setOnMenuItemClickListener {
        val view = findViewById<View>(it.itemId)
        val extras = FragmentNavigatorExtras(view to view.transitionName)
        val action = when (it.itemId) {
            R.id.menu_settings -> SettingsFragmentDirections.actionSettings()
            R.id.menu_extensions -> ExtensionsFragmentDirections.actionExtensions()
            else -> null
        }
        action?.let { navDirections ->
            findNavController().navigate(navDirections, extras)
            true
        } ?: false
    }
}


