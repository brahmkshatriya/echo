package dev.brahmkshatriya.echo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class BaseFragment<TBindingType : ViewBinding>(
    val inflaterCallback: (LayoutInflater, ViewGroup?, Boolean) -> TBindingType
) : Fragment() {

    private var _binding: TBindingType? = null
    protected val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = inflaterCallback(inflater, container, false)
        handleBackPressForPlayerView()
        onCreateBindingView()
        return binding.root
    }

    override fun onDestroyView() {
        onDestroyBindingView()
        super.onDestroyView()
        _binding = null
    }

    open fun onCreateBindingView() {}
    open fun onDestroyBindingView() {}

    fun updateRootPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(insets.left, insets.top, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    fun updateRootBottomMargin() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom
            }
            windowInsets
        }
    }

    private val mainViewModel by activityViewModels<MainViewModel>()

    private fun handleBackPressForPlayerView() {
        val backPress = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                mainViewModel.collapsePlayer?.invoke()
            }
        }
        lifecycleScope.launch {
            mainViewModel.playerCollapsed.collectLatest {
                backPress.isEnabled = it
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPress)
    }
}