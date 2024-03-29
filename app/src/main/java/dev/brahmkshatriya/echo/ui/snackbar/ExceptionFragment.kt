package dev.brahmkshatriya.echo.ui.snackbar

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.transition.platform.MaterialFade
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentExceptionBinding
import dev.brahmkshatriya.echo.player.ui.PlayerBackButtonHelper
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.updatePaddingWithPlayerAndSystemInsets

class ExceptionFragment : Fragment() {
    private var binding: FragmentExceptionBinding by autoCleared()
    private val viewmodel : ThrowableViewModel by activityViewModels()

    class ThrowableViewModel : ViewModel() {
        var throwable : Throwable? = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExceptionBinding.inflate(inflater, container, false)
        enterTransition = MaterialFade()
        exitTransition = MaterialFade()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        PlayerBackButtonHelper.addCallback(this) {
            binding.nestedScrollView.updatePaddingWithPlayerAndSystemInsets(it, false)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.exceptionIconContainer.updatePadding(top = insets.top)
            windowInsets
        }

        binding.appBarLayout.addOnOffsetChangedListener { appbar, verticalOffset ->
            val offset = (-verticalOffset) / appbar.totalScrollRange.toFloat()
            binding.toolbarOutline.alpha = offset
        }

        binding.exceptionMessage.setupWithNavController(findNavController())

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