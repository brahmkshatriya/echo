package dev.brahmkshatriya.echo.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialFade
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentSettingsContainerBinding
import dev.brahmkshatriya.echo.player.ui.PlayerBackButtonHelper
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.updatePaddingWithPlayerAndSystemInsets

class SettingsFragment : Fragment(){
    private var binding: FragmentSettingsContainerBinding by autoCleared()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsContainerBinding.inflate(inflater, container, false)
        enterTransition = MaterialFade()
        exitTransition = MaterialFade()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        PlayerBackButtonHelper.addCallback(this) {
            binding.fragmentContainer.updatePaddingWithPlayerAndSystemInsets(it, false)
        }

        binding.appBarLayout.addOnOffsetChangedListener { appbar, verticalOffset ->
            val offset = (-verticalOffset) / appbar.totalScrollRange.toFloat()
            binding.toolbarOutline.alpha = offset
        }

        binding.title.setupWithNavController(findNavController())
        sharedElementEnterTransition = MaterialContainerTransform(requireContext(), true).apply {
            drawingViewId = R.id.nav_host_fragment
        }

        binding.title.title = getString(R.string.settings)
        childFragmentManager.beginTransaction().add(R.id.fragment_container, PreferenceFragment()).commit()
    }
}