package dev.brahmkshatriya.echo.ui.main.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.databinding.FragmentSettingsBinding
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.addIfNull
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.common.UiViewModel
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.common.UiViewModel.Companion.applyInsetsMain
import dev.brahmkshatriya.echo.ui.download.DownloadFragment
import dev.brahmkshatriya.echo.ui.extensions.list.ExtensionsListBottomSheet
import dev.brahmkshatriya.echo.ui.extensions.manage.ManageExtensionsFragment
import dev.brahmkshatriya.echo.ui.main.MainFragment.Companion.applyPlayerBg
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import dev.brahmkshatriya.echo.utils.ui.UiUtils.configureAppBar
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class SettingsFragment : Fragment() {
    private var binding by autoCleared<FragmentSettingsBinding>()
    private val uiViewModel by activityViewModel<UiViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    private inline fun <reified F : Fragment> Fragment.addIfNull(tag: String): String {
        addIfNull<F>(R.id.settingsContainer, tag)
        return tag
    }

    private fun change() {
        val toShow = when (uiViewModel.selectedSettingsTab.value) {
            0 -> addIfNull<DownloadFragment>("downloads")
            1 -> addIfNull<AudioFragment.AudioPreference>("audio")
            2 -> addIfNull<LookFragment.LookPreference>("look")
            else -> addIfNull<MiscFragment.AboutPreference>("other")
        }

        childFragmentManager.commit(true) {
            childFragmentManager.fragments.forEach { fragment ->
                if (fragment.tag != toShow) hide(fragment)
                else show(fragment)
            }
            setReorderingAllowed(true)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        applyPlayerBg(view, false)
        applyInsetsMain(binding.appBarLayout, null)
        applyBackPressCallback()
        binding.toolBar.setOnMenuItemClickListener {
            requireActivity().openFragment<ManageExtensionsFragment>()
            true
        }
        binding.appBarLayout.configureAppBar { offset ->
            binding.appBarOutline.alpha = offset
            binding.appBarOutline.isVisible = offset > 0
            binding.toolBar.alpha = 1 - offset
        }
        binding.tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
            override fun onTabSelected(tab: TabLayout.Tab) {
                uiViewModel.selectedSettingsTab.value = binding.tabLayout.selectedTabPosition
            }
        })
        observe(uiViewModel.selectedSettingsTab) {
            binding.tabLayout.getTabAt(it)?.select()
            change()
        }
        observe(uiViewModel.navigationReselected) {
            if (it == 3) ExtensionsListBottomSheet.newInstance(ExtensionType.MUSIC)
                .show(parentFragmentManager, null)
        }
    }

}