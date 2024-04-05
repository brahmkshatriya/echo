package dev.brahmkshatriya.echo.ui.common

import android.view.View
import androidx.fragment.app.activityViewModels
import com.google.android.material.appbar.MaterialToolbar
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.ui.extension.ExtensionsFragment
import dev.brahmkshatriya.echo.ui.settings.SettingsFragment
import dev.brahmkshatriya.echo.utils.load
import dev.brahmkshatriya.echo.utils.observe
import dev.brahmkshatriya.echo.viewmodels.ExtensionViewModel

fun MaterialToolbar.configureMainMenu(fragment: MainFragment) {
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
        when (it.itemId) {
            R.id.menu_settings -> {
                fragment.openFragment(SettingsFragment(), view)
                true
            }
            R.id.menu_extensions -> {
                ExtensionsFragment().show(fragment.parentFragmentManager, null)
                true
            }
            else -> false
        }
    }
}


