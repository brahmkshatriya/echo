package dev.brahmkshatriya.echo.ui.exception

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import dev.brahmkshatriya.echo.ExceptionActivity
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentExceptionBinding
import dev.brahmkshatriya.echo.playback.MediaItemUtils.audioIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.clientId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.ui.settings.AboutFragment.AboutPreference.Companion.appVersion
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.getSerial
import dev.brahmkshatriya.echo.utils.onAppBarChangeListener
import dev.brahmkshatriya.echo.utils.setupTransition
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

class ExceptionFragment : Fragment() {
    private var binding by autoCleared<FragmentExceptionBinding>()
    private val throwable by lazy { requireArguments().getSerial<Throwable>("exception")!! }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExceptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        setupTransition(view)
        applyInsets {
            binding.exceptionIconContainer.updatePadding(top = it.top)
            binding.nestedScrollView.applyContentInsets(it)
        }
        applyBackPressCallback()
        binding.appBarLayout.onAppBarChangeListener { offset ->
            binding.toolbarOutline.alpha = offset
        }
        binding.exceptionMessage.apply {
            val icon = navigationIcon
            navigationIcon = icon.takeIf { parentFragmentManager.fragments.size > 1 }

            setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
        }

        binding.exceptionMessage.title = requireContext().getTitle(throwable)
        binding.exceptionDetails.text = requireContext().getDetails(throwable)

        binding.exceptionMessage.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.exception_copy -> {
                    with(requireContext()) {
                        copyToClipboard(throwable.message, "```\n${getDetails(throwable)}\n```")
                    }
                    true
                }

                else -> false
            }
        }
    }

    companion object {
        fun Context.copyToClipboard(label: String?, string: String) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, string)
            clipboard.setPrimaryClip(clip)
        }

        fun Context.getTitle(throwable: Throwable): String = when (throwable) {
            is IncompatibleClassChangeError -> getString(R.string.extension_out_of_date)
            is ExceptionActivity.AppCrashException -> getString(R.string.app_crashed)
            is UnknownHostException, is UnresolvedAddressException -> getString(R.string.no_internet)
            is PlayerViewModel.PlayerException -> getTitle(throwable.cause)
            is AppException -> throwable.run {
                when (this) {
                    is AppException.Unauthorized ->
                        getString(R.string.unauthorized, extensionName)

                    is AppException.LoginRequired ->
                        getString(R.string.login_required, extensionName)

                    is AppException.NotSupported ->
                        getString(R.string.is_not_supported, operation, extensionName)

                    is AppException.Other -> getTitle(cause)
                }
            }

            else -> throwable.message ?: getString(R.string.error)
        }

        fun Context.getDetails(throwable: Throwable): String = when (throwable) {
            is PlayerViewModel.PlayerException -> """
Client Id : ${throwable.mediaItem?.clientId}
Track : ${throwable.mediaItem?.track}
Stream : ${throwable.mediaItem?.run { track.audioStreamables.getOrNull(audioIndex) }}

${getDetails(throwable.cause)}
""".trimIndent()

            is AppException -> """
Extension : ${throwable.extensionName}
Id : ${throwable.extensionId}
Type : ${throwable.extensionType}
Version : ${throwable.extensionMetadata.version}
App Version : ${appVersion()}

${getDetails(throwable.cause)}
""".trimIndent()

            is ExceptionActivity.AppCrashException -> throwable.causedBy
            else -> throwable.stackTraceToString()
        }

        fun newInstance(throwable: Throwable) = ExceptionFragment().apply {
            arguments = Bundle().apply {
                putSerializable("exception", throwable)
            }
        }
    }
}