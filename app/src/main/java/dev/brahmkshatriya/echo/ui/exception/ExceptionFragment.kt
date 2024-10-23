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
import dev.brahmkshatriya.echo.EchoApplication.Companion.appVersion
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.databinding.FragmentExceptionBinding
import dev.brahmkshatriya.echo.extensions.ExtensionLoadingException
import dev.brahmkshatriya.echo.extensions.InvalidExtensionListException
import dev.brahmkshatriya.echo.extensions.RequiredExtensionsException
import dev.brahmkshatriya.echo.playback.MediaItemUtils.audioIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.clientId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.getSerialized
import dev.brahmkshatriya.echo.utils.onAppBarChangeListener
import dev.brahmkshatriya.echo.utils.putSerialized
import dev.brahmkshatriya.echo.utils.setupTransition
import dev.brahmkshatriya.echo.viewmodels.PlayerViewModel
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets
import kotlinx.serialization.Serializable
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

class ExceptionFragment : Fragment() {
    private var binding by autoCleared<FragmentExceptionBinding>()
    private val throwable by lazy {
        requireArguments().getSerialized<ExceptionDetails>("exception")!!
    }

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

        binding.exceptionMessage.title = throwable.title
        binding.exceptionDetails.text = throwable.causedBy

        binding.exceptionMessage.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.exception_copy -> {
                    with(requireContext()) {
                        copyToClipboard(throwable.title, "```\n${throwable.causedBy}\n```")
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
            is UnknownHostException, is UnresolvedAddressException -> getString(R.string.no_internet)
            is PlayerViewModel.PlayerException -> throwable.details.title
            is ExtensionLoadingException -> getString(R.string.invalid_extension)
            is RequiredExtensionsException -> getString(
                R.string.extension_requires_following_extensions,
                throwable.name,
                throwable.requiredExtensions.joinToString(", ")
            )
            is InvalidExtensionListException -> getString(R.string.invalid_extension_list)
            is AppException -> throwable.run {
                when (this) {
                    is AppException.Unauthorized ->
                        getString(R.string.unauthorized, extension.name)

                    is AppException.LoginRequired ->
                        getString(R.string.login_required, extension.name)

                    is AppException.NotSupported ->
                        getString(R.string.is_not_supported, operation, extension.name)

                    is AppException.Other -> "${extension.name} : ${getTitle(cause)}"
                }
            }

            else -> throwable.message ?: getString(R.string.error)
        }

        fun Context.getDetails(throwable: Throwable): String = when (throwable) {
            is PlayerViewModel.PlayerException -> """
Client Id : ${throwable.mediaItem?.clientId}
Track : ${throwable.mediaItem?.track}
Stream : ${throwable.mediaItem?.run { track.audioStreamables.getOrNull(audioIndex) }}

${throwable.details.causedBy}
""".trimIndent()

            is RequiredExtensionsException -> """
Extension : ${throwable.name}
Required Extensions : ${throwable.requiredExtensions.joinToString(", ")}
""".trimIndent()

            is AppException -> """
Extension : ${throwable.extension.name}
Id : ${throwable.extension.name}
Type : ${throwable.extension.type}
Version : ${throwable.extension.version}
App Version : ${appVersion()}

${getDetails(throwable.cause)}
""".trimIndent()

            else -> throwable.stackTraceToString()
        }


        fun newInstance(details: ExceptionDetails) = ExceptionFragment().apply {
            arguments = Bundle().apply {
                putSerialized("exception", details)
            }
        }

        fun newInstance(context: Context, throwable: Throwable): ExceptionFragment {
            val details =
                ExceptionDetails(context.getTitle(throwable), context.getDetails(throwable))
            return newInstance(details)
        }

        fun Throwable.toExceptionDetails(context: Context) =
            ExceptionDetails(context.getTitle(this), context.getDetails(this))
    }

    @Serializable
    class ExceptionDetails(val title: String, val causedBy: String)
}