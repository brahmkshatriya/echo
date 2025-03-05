package dev.brahmkshatriya.echo.ui.exceptions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentExceptionBinding
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.ui.UiViewModel.Companion.applyInsets
import dev.brahmkshatriya.echo.ui.exceptions.ExceptionUtils.getPasteLink
import dev.brahmkshatriya.echo.utils.ContextUtils.copyToClipboard
import dev.brahmkshatriya.echo.utils.Serializer.getSerialized
import dev.brahmkshatriya.echo.utils.Serializer.putSerialized
import dev.brahmkshatriya.echo.utils.ui.AnimationUtils.setupTransition
import dev.brahmkshatriya.echo.utils.ui.AutoClearedValue.Companion.autoCleared
import dev.brahmkshatriya.echo.utils.ui.UiUtils.onAppBarChangeListener
import kotlinx.coroutines.launch

class ExceptionFragment : Fragment() {
    companion object {
        fun getBundle(data: ExceptionUtils.Data) = Bundle().apply {
            putSerialized("data", data)
        }
    }

    private val data by lazy {
        requireArguments().getSerialized<ExceptionUtils.Data>("data")!!
    }

    private var binding by autoCleared<FragmentExceptionBinding>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentExceptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTransition(view)
        applyInsets {
            binding.nestedScrollView.applyContentInsets(it)
        }
        applyBackPressCallback()
        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.toolbarOutline.alpha = offset
        }

        binding.exceptionMessage.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.exceptionMessage.title = data.title
        binding.exceptionDetails.text = data.trace

        binding.exceptionMessage.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.exception_copy -> {
                    lifecycleScope.launch {
                        val toCopy = getPasteLink(data).getOrElse { data.trace }
                        requireContext().copyToClipboard("Error", toCopy)
                    }
                    true
                }

                else -> false
            }
        }
    }
}