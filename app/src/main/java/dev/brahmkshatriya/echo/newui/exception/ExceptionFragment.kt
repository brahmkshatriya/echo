package dev.brahmkshatriya.echo.newui.exception

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.transition.platform.MaterialArcMotion
import com.google.android.material.transition.platform.MaterialContainerTransform
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentExceptionBinding
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.onAppBarChangeListener
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets

class ExceptionFragment : Fragment() {
    private var binding by autoCleared<FragmentExceptionBinding>()
    private val viewmodel by activityViewModels<ThrowableViewModel>()

    class ThrowableViewModel : ViewModel() {
        var throwable: Throwable? = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExceptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        applyInsets {
            binding.exceptionIconContainer.updatePadding(top = it.top)
            binding.nestedScrollView.updatePadding(bottom = it.bottom)
        }
        applyBackPressCallback()
        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.toolbarOutline.alpha = offset
        }

        binding.exceptionMessage.setupWithNavController(findNavController())
        val theme = requireContext().theme
        binding.exceptionMessage.navigationIcon =
            ResourcesCompat.getDrawable(resources, R.drawable.ic_back, theme)

        val throwable = viewmodel.throwable ?: return

        val transitionName = throwable.hashCode().toString()
        postponeEnterTransition()
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            pathMotion = MaterialArcMotion()
        }
        binding.root.transitionName = transitionName
        view.doOnPreDraw { startPostponedEnterTransition() }

        binding.exceptionMessage.title = throwable.message
        binding.exceptionDetails.text = throwable.stackTraceToString()

        binding.exceptionMessage.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.exception_copy -> {
                    copyToClipboard(throwable.message, throwable.stackTraceToString())
                    true
                }

                else -> false
            }
        }
    }

    private fun copyToClipboard(label: String?, string: String) {
        val clipboard =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, string)
        clipboard.setPrimaryClip(clip)
    }
}