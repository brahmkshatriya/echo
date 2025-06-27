package dev.brahmkshatriya.echo.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.databinding.FragmentMainBinding
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.isClient
import dev.brahmkshatriya.echo.extensions.db.models.UserEntity.Companion.toEntity
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.addIfNull
import dev.brahmkshatriya.echo.ui.common.UiViewModel
import dev.brahmkshatriya.echo.ui.extensions.ExtensionsViewModel
import dev.brahmkshatriya.echo.ui.extensions.list.ExtensionsListBottomSheet
import dev.brahmkshatriya.echo.ui.extensions.login.LoginUserBottomSheet
import dev.brahmkshatriya.echo.ui.extensions.login.LoginUserListViewModel
import dev.brahmkshatriya.echo.ui.main.home.HomeFragment
import dev.brahmkshatriya.echo.ui.main.library.LibraryFragment
import dev.brahmkshatriya.echo.ui.main.search.SearchFragment
import dev.brahmkshatriya.echo.ui.main.settings.NewSettingsFragment
import dev.brahmkshatriya.echo.utils.ContextUtils.getSettings
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadAsCircle
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import dev.brahmkshatriya.echo.utils.ui.GradientDrawable
import dev.brahmkshatriya.echo.utils.ui.GradientDrawable.BACKGROUND_GRADIENT
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class MainFragment : Fragment() {

    private var binding by autoCleared<FragmentMainBinding>()
    private val viewModel by activityViewModel<UiViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    private inline fun <reified F : Fragment> Fragment.addIfNull(tag: String): String {
        addIfNull<F>(R.id.main_fragment_container_view, tag)
        return tag
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        observe(viewModel.navigation) {
            val toShow = when (it) {
                1 -> addIfNull<SearchFragment>("search")
                2 -> addIfNull<LibraryFragment>("library")
                3 -> addIfNull<NewSettingsFragment>("settings")
                else -> addIfNull<HomeFragment>("home")
            }

            childFragmentManager.commit(true) {
                childFragmentManager.fragments.forEach { fragment ->
                    if (fragment.tag != toShow) hide(fragment)
                    else show(fragment)
                }
                setReorderingAllowed(true)
            }
        }
    }

    companion object {
        fun Fragment.applyPlayerBg(view: View, useExtensionColors: Boolean = true) {
            val uiViewModel by activityViewModel<UiViewModel>()
            val combined = uiViewModel.run {
                if (!useExtensionColors) playerColors.map { it?.accent }
                else playerColors.combine(extensionColor) { a, b -> a?.accent ?: b }
            }
            val settings = requireContext().getSettings()
            observe(combined) {
                val isGradient = settings.getBoolean(BACKGROUND_GRADIENT, true)
                val color = if (isGradient) it ?: MaterialColors.getColor(
                    view, androidx.appcompat.R.attr.colorPrimary
                ) else MaterialColors.getColor(view, R.attr.echoBackground)
                view.background = GradientDrawable.createBg(view, color)
            }
        }

        private fun <T> View.setLoopedLongClick(
            list: List<T>,
            getCurrent: (View) -> T?,
            onSelect: (T) -> Unit
        ) {
            setOnLongClickListener {
                val current = getCurrent(this)
                val index = list.indexOf(current)
                if (index == -1) return@setOnLongClickListener false
                val next = list[(index + 1) % list.size]
                if (next == current) return@setOnLongClickListener false
                onSelect(next)
                true
            }
        }

        fun MaterialToolbar.configureMainMenu(fragment: MainFragment) {
            val extVM by fragment.activityViewModel<ExtensionsViewModel>()
            val loginUserViewModel by fragment.activityViewModel<LoginUserListViewModel>()
            val accountsMenu = findViewById<View>(R.id.menu_accounts)
            accountsMenu.transitionName = "settings"
            val extMenu = findViewById<View>(R.id.menu_extensions)
            extMenu.transitionName = "extensions"

            fragment.observe(extVM.extensionLoader.current) { extension ->
                loginUserViewModel.currentExtension.value = extension
                extension?.metadata?.icon.loadAsCircle(extMenu, R.drawable.ic_extension_48dp) {
                    menu.findItem(R.id.menu_extensions).icon = it
                }
            }
            extMenu.setOnClickListener {
                ExtensionsListBottomSheet.newInstance(ExtensionType.MUSIC)
                    .show(fragment.parentFragmentManager, null)
            }
            val list = extVM.extensionLoader.music.value.filter { it.isEnabled }
            extMenu.setLoopedLongClick(list, { extVM.extensionLoader.current.value }) { next ->
                extVM.extensionLoader.setupMusicExtension(next, true)
            }

            fragment.observe(loginUserViewModel.allUsers) { (extension, all) ->
                val isLoginClient = extension?.isClient<LoginClient>() ?: false
                accountsMenu.isVisible = isLoginClient
                val user = loginUserViewModel.currentUser.value.second
                if (isLoginClient) {
                    val ext = extension!!
                    user?.cover.loadAsCircle(accountsMenu, R.drawable.ic_account_circle_48dp) {
                        menu.findItem(R.id.menu_accounts).icon = it
                    }
                    accountsMenu.setOnClickListener {
                        LoginUserBottomSheet().show(fragment.parentFragmentManager, null)
                    }
                    accountsMenu.setLoopedLongClick(
                        all.orEmpty(), { loginUserViewModel.currentUser.value.second }
                    ) { next ->
                        loginUserViewModel.setLoginUser(next.toEntity(ext.type, ext.id))
                    }
                }
            }
        }
    }
}