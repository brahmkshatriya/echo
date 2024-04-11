package dev.brahmkshatriya.echo.ui.common

import android.view.View
import androidx.fragment.app.activityViewModels
import com.google.android.material.appbar.MaterialToolbar
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.ui.extension.ExtensionsFragment
import dev.brahmkshatriya.echo.ui.login.LoginFragment
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

    settings.setOnClickListener {
        fragment.openFragment(SettingsFragment(), settings)
    }

    settings.setOnLongClickListener {
        extensionViewModel.currentExtension?.let {
            fragment.openFragment(
                LoginFragment.newInstance(it.metadata.id, it.metadata.name),
                settings
            )
        }
        true
    }
    extensions.setOnClickListener {
        ExtensionsFragment().show(fragment.parentFragmentManager, null)
    }
    extensions.setOnLongClickListener {
        extensionViewModel.run {
            val list = extensionListFlow.flow.value ?: return@run false
            val index = list.indexOf(currentExtension)
            val next = list[(index + 1) % list.size]
            setExtension(next)
            true
        }
    }
}


