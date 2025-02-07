package dev.brahmkshatriya.echo.ui.exception

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dev.brahmkshatriya.echo.EchoApplication.Companion.appVersion
import dev.brahmkshatriya.echo.ExceptionActivity
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.helpers.ContinuationCallback.Companion.await
import dev.brahmkshatriya.echo.databinding.FragmentExceptionBinding
import dev.brahmkshatriya.echo.download.DownloadException
import dev.brahmkshatriya.echo.download.TaskException
import dev.brahmkshatriya.echo.extensions.ExtensionLoadingException
import dev.brahmkshatriya.echo.extensions.InvalidExtensionListException
import dev.brahmkshatriya.echo.extensions.RequiredExtensionsException
import dev.brahmkshatriya.echo.extensions.UpdateException
import dev.brahmkshatriya.echo.playback.MediaItemUtils.extensionId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.sourcesIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.PlayerException
import dev.brahmkshatriya.echo.utils.autoCleared
import dev.brahmkshatriya.echo.utils.getSerialized
import dev.brahmkshatriya.echo.utils.putSerialized
import dev.brahmkshatriya.echo.utils.ui.onAppBarChangeListener
import dev.brahmkshatriya.echo.utils.ui.setupTransition
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyBackPressCallback
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyContentInsets
import dev.brahmkshatriya.echo.viewmodels.UiViewModel.Companion.applyInsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException
import kotlin.coroutines.cancellation.CancellationException

class ExceptionFragment : Fragment() {
    private var binding by autoCleared<FragmentExceptionBinding>()
    private val throwable by lazy {
        requireArguments().getSerialized<ExceptionActivity.ExceptionDetails>("exception")!!
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
                    lifecycleScope.launch {
                        val pasteLink = getPasteLink(throwable).getOrElse {
                            throwable.causedBy
                        }
                        requireContext().copyToClipboard("Error", pasteLink)
                    }
                    true
                }

                else -> false
            }
        }
    }

    companion object {

        private val client = OkHttpClient()
        suspend fun getPasteLink(throwable: ExceptionActivity.ExceptionDetails) =
            withContext(Dispatchers.IO) {
                val details = throwable.causedBy
                val request = Request.Builder()
                    .url("https://paste.rs")
                    .post(details.toRequestBody())
                    .build()
                runCatching { client.newCall(request).await().body.string() }
            }

        fun Context.copyToClipboard(label: String?, string: String) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, string)
            clipboard.setPrimaryClip(clip)
        }

        fun Context.getTitle(throwable: Throwable): String = when (throwable) {
            is LinkageError -> getString(R.string.extension_out_of_date)
            is UnknownHostException, is UnresolvedAddressException -> getString(R.string.no_internet)
            is PlayerException -> throwable.details.title
            is ExtensionLoadingException -> "${getString(R.string.invalid_extension)}: ${throwable.type}"
            is RequiredExtensionsException -> getString(
                R.string.extension_requires_following_extensions,
                throwable.name,
                throwable.requiredExtensions.joinToString(", ")
            )

            is InvalidExtensionListException -> getString(R.string.invalid_extension_list)
            is UpdateException -> "${getString(R.string.update_error)}: ${getTitle(throwable.cause)}"
            is AppException -> throwable.run {
                when (this) {
                    is AppException.Unauthorized ->
                        getString(R.string.unauthorized, extension.name)

                    is AppException.LoginRequired ->
                        getString(R.string.login_required, extension.name)

                    is AppException.NotSupported ->
                        getString(R.string.is_not_supported, operation, extension.name)

                    is AppException.Other -> "${extension.name}: ${getTitle(cause)}"
                }
            }

            is DownloadException -> "${getString(R.string.download)}: ${throwable.trackEntity.track.title}: ${getTitle(throwable.cause)}"
            is TaskException -> "${throwable.taskEntity.run { title ?: id }} - ${getTitle(throwable.cause)}"
            is CancellationException -> getString(R.string.cancelled)
            else -> throwable.message ?: getString(R.string.error)
        }

        fun Context.getDetails(throwable: Throwable): String = when (throwable) {
            is PlayerException -> """
Client Id : ${throwable.mediaItem?.extensionId}
Track : ${throwable.mediaItem?.track}
Stream : ${throwable.mediaItem?.run { track.servers.getOrNull(sourcesIndex) }}

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

            is DownloadException -> """
Track : ${throwable.trackEntity}
${getDetails(throwable.cause)}
""".trimIndent()

            is TaskException -> """
Task : ${throwable.taskEntity}

${getDetails(throwable.cause)}
""".trimIndent()

            is UpdateException -> throwable.cause.stackTraceToString()

            else -> throwable.stackTraceToString()
        }


        fun newInstance(details: ExceptionActivity.ExceptionDetails) = ExceptionFragment().apply {
            arguments = Bundle().apply {
                putSerialized("exception", details)
            }
        }

        fun newInstance(context: Context, throwable: Throwable): ExceptionFragment {
            val details =
                ExceptionActivity.ExceptionDetails(
                    context.getTitle(throwable),
                    context.getDetails(throwable)
                )
            return newInstance(details)
        }

        fun Throwable.toExceptionDetails(context: Context) =
            ExceptionActivity.ExceptionDetails(context.getTitle(this), context.getDetails(this))
    }

}