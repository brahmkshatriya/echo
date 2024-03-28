package dev.brahmkshatriya.echo.newui.exception

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentExceptionBinding
import dev.brahmkshatriya.echo.utils.autoCleared

class ExceptionFragment : Fragment() {
    private var binding: FragmentExceptionBinding by autoCleared()
    private val viewmodel: ThrowableViewModel by activityViewModels()

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
        binding.appBarLayout.addOnOffsetChangedListener { appbar, verticalOffset ->
            val offset = (-verticalOffset) / appbar.totalScrollRange.toFloat()
            binding.toolbarOutline.alpha = offset
        }

        binding.exceptionMessage.setupWithNavController(findNavController())
        val theme = requireContext().theme
        binding.exceptionMessage.navigationIcon =
            ResourcesCompat.getDrawable(resources, R.drawable.ic_back, theme)

        val throwable = viewmodel.throwable ?: return
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