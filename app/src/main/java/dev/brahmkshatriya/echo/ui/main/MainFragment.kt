package dev.brahmkshatriya.echo.ui.main

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import dev.brahmkshatriya.echo.ui.UiViewModel
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.addIfNull
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.extensions.ExtensionsViewModel
import dev.brahmkshatriya.echo.ui.extensions.list.ExtensionsListBottomSheet
import dev.brahmkshatriya.echo.ui.extensions.login.LoginUserBottomSheet
import dev.brahmkshatriya.echo.ui.extensions.login.LoginUserListViewModel
import dev.brahmkshatriya.echo.ui.main.home.HomeFragment
import dev.brahmkshatriya.echo.ui.main.library.LibraryFragment
import dev.brahmkshatriya.echo.ui.main.search.SearchFragment
import dev.brahmkshatriya.echo.ui.settings.SettingsFragment
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.image.ImageUtils.loadAsCircle
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import dev.brahmkshatriya.echo.utils.ui.GradientDrawable
import kotlinx.coroutines.flow.combine
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class MainFragment : Fragment() {

    init {
        println("MainFragment init $this")
    }

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
                else -> addIfNull<HomeFragment>("home")
            }

            childFragmentManager.commit {
                childFragmentManager.fragments.forEach { fragment ->
                    if (fragment.tag != toShow) hide(fragment)
                    else show(fragment)
                }
                setReorderingAllowed(true)
            }
        }
    }

    companion object {
        fun Fragment.applyPlayerBg(view: View, appBar: View) {
            val uiViewModel by activityViewModel<UiViewModel>()
            appBar.setBackgroundColor(Color.TRANSPARENT)
            val combined = uiViewModel.run {
                playerColors.combine(extensionColor) { a, b -> a?.accent ?: b }
            }
            observe(combined) {
                val color = it ?: MaterialColors.getColor(
                    view, com.google.android.material.R.attr.colorPrimary
                )
                view.background = GradientDrawable.createBg(view, color)
            }
        }

        fun MaterialToolbar.configureMainMenu(fragment: MainFragment) {
            val extVM by fragment.activityViewModel<ExtensionsViewModel>()
            val loginUserViewModel by fragment.activityViewModel<LoginUserListViewModel>()
            val settingsMenu = findViewById<View>(R.id.menu_settings)
            settingsMenu.transitionName = "settings"
            val extMenu = findViewById<View>(R.id.menu_extensions)
            extMenu.transitionName = "extensions"

            fragment.observe(extVM.extensions.current) { extension ->
                loginUserViewModel.currentExtension.value = extension
                extension?.metadata?.icon.loadAsCircle(extMenu, R.drawable.ic_extension_48dp) {
                    menu.findItem(R.id.menu_extensions).icon = it
                }
            }
            extMenu.setOnClickListener {
                ExtensionsListBottomSheet.newInstance(ExtensionType.MUSIC)
                    .show(fragment.parentFragmentManager, null)
            }
            extMenu.setOnLongClickListener {
                extVM.extensions.run {
                    val list = music.value?.filter { it.isEnabled } ?: return@run false
                    val index = list.indexOf(current.value)
                    if (index == -1) return@run false
                    val next = list[(index + 1) % list.size]
                    if (next == current.value) return@run false
                    setupMusicExtension(next)
                    true
                }
            }

            fragment.observe(loginUserViewModel.allUsers) { (extension, all) ->
                val isLoginClient = extension?.isClient<LoginClient>() ?: false
                val user = loginUserViewModel.currentUser.value.second
                if (isLoginClient) {
                    user?.cover.loadAsCircle(settingsMenu, R.drawable.ic_account_circle_48dp) {
                        menu.findItem(R.id.menu_settings).icon = it
                    }
                    settingsMenu.setOnClickListener {
                        LoginUserBottomSheet().show(fragment.parentFragmentManager, null)
                    }
                    settingsMenu.setOnLongClickListener LongClick@{
                        val ext = extension ?: return@LongClick false
                        val index = all?.indexOf(user) ?: return@LongClick false
                        if (index == -1) return@LongClick false
                        val next = all[(index + 1) % all.size]
                        if (next == user) return@LongClick false
                        loginUserViewModel.setLoginUser(next.toEntity(ext.type, ext.id))
                        true
                    }
                } else {
                    menu.findItem(R.id.menu_settings).setIcon(R.drawable.ic_settings_outline)
                    settingsMenu.setOnClickListener {
                        fragment.openFragment<SettingsFragment>(settingsMenu)
                    }
                    settingsMenu.setOnLongClickListener(null)
                }

            }
        }

    }
}