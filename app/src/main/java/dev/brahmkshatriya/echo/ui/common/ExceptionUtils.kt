package dev.brahmkshatriya.echo.ui.common

import android.content.Context
import android.view.View
import androidx.fragment.app.FragmentActivity
import dev.brahmkshatriya.echo.MainActivity
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.common.helpers.ContinuationCallback.Companion.await
import dev.brahmkshatriya.echo.common.models.Message
import dev.brahmkshatriya.echo.download.exceptions.DownloadException
import dev.brahmkshatriya.echo.download.exceptions.DownloaderExtensionNotFoundException
import dev.brahmkshatriya.echo.download.tasks.BaseTask.Companion.getTitle
import dev.brahmkshatriya.echo.extensions.db.models.UserEntity
import dev.brahmkshatriya.echo.extensions.exceptions.AppException
import dev.brahmkshatriya.echo.extensions.exceptions.ExtensionLoaderException
import dev.brahmkshatriya.echo.extensions.exceptions.ExtensionNotFoundException
import dev.brahmkshatriya.echo.extensions.exceptions.InvalidExtensionListException
import dev.brahmkshatriya.echo.extensions.exceptions.RequiredExtensionsMissingException
import dev.brahmkshatriya.echo.playback.MediaItemUtils.extensionId
import dev.brahmkshatriya.echo.playback.MediaItemUtils.serverIndex
import dev.brahmkshatriya.echo.playback.MediaItemUtils.track
import dev.brahmkshatriya.echo.playback.exceptions.NoServersException
import dev.brahmkshatriya.echo.playback.exceptions.NoSourceException
import dev.brahmkshatriya.echo.playback.exceptions.PlayerException
import dev.brahmkshatriya.echo.ui.common.FragmentUtils.openFragment
import dev.brahmkshatriya.echo.ui.extensions.login.LoginFragment
import dev.brahmkshatriya.echo.ui.extensions.login.LoginUserListViewModel
import dev.brahmkshatriya.echo.utils.AppUpdater
import dev.brahmkshatriya.echo.utils.ContextUtils.appVersion
import dev.brahmkshatriya.echo.utils.ContextUtils.observe
import dev.brahmkshatriya.echo.utils.Serializer.rootCause
import dev.brahmkshatriya.echo.utils.Serializer.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

object ExceptionUtils {

    private fun Context.getTitle(throwable: Throwable): String? = when (throwable) {
        is LinkageError, is ReflectiveOperationException -> getString(R.string.extension_out_of_date)
        is UnknownHostException, is UnresolvedAddressException -> getString(R.string.no_internet)
        is ExtensionLoaderException ->
            getString(R.string.error_loading_extension_from_x, throwable.clazz)

        is ExtensionNotFoundException -> getString(R.string.extension_x_not_found, throwable.id)
        is RequiredExtensionsMissingException -> getString(
            R.string.required_extensions_missing_x,
            throwable.required.joinToString(", ")
        )

        is AppException -> when (throwable) {
            is AppException.Unauthorized ->
                getString(R.string.account_session_expired_in_x, throwable.extension.name)

            is AppException.LoginRequired ->
                getString(R.string.x_login_required, throwable.extension.name)

            is AppException.NotSupported -> getString(
                R.string.x_is_not_supported_in_x,
                throwable.operation,
                throwable.extension.name
            )

            is AppException.Other -> "${throwable.extension.name}: ${getFinalTitle(throwable.cause)}"
        }

        is InvalidExtensionListException -> getString(R.string.invalid_extension_list)
        is AppUpdater.UpdateException -> getString(R.string.error_updating_extension)

        is PlayerException -> "${throwable.mediaItem?.track?.title}: ${getFinalTitle(throwable.cause)}"
        is NoServersException -> getString(R.string.no_servers_found)
        is NoSourceException -> getString(R.string.no_source_found)

        is DownloadException -> {
            val title = getTitle(throwable.type, throwable.downloadEntity.track.title)
            "${title}: ${getFinalTitle(throwable.cause)}"
        }

        is DownloaderExtensionNotFoundException -> getString(R.string.no_download_extension)

        else -> null
    }

    private fun getDetails(throwable: Throwable): String? = when (throwable) {
        is ExtensionLoaderException -> """
            Class: ${throwable.clazz}
            Source: ${throwable.source}
        """.trimIndent()

        is ExtensionNotFoundException -> "Extension ID: ${throwable.id}"
        is RequiredExtensionsMissingException ->
            "Required Extension: ${throwable.required.joinToString(", ")}"

        is AppException -> """
            Type: ${throwable.extension.type}
            ID: ${throwable.extension.id}
            Extension: ${throwable.extension.name}(${throwable.extension.version})
            ${if (throwable is AppException.NotSupported) "Operation: ${throwable.operation}" else ""}
        """.trimIndent()

        is InvalidExtensionListException -> "Link: ${throwable.link}"

        is PlayerException -> throwable.mediaItem?.let {
            """
            Extension ID: ${it.extensionId}
            Track: ${it.track.toJson()}
            Stream: ${it.run { track.servers.getOrNull(serverIndex)?.toJson() }}
        """.trimIndent()
        }

        is DownloadException -> """
            Type: ${throwable.type}
            Track: ${throwable.downloadEntity.toJson()}
        """.trimIndent()

        else -> null
    }

    fun Context.getFinalTitle(throwable: Throwable): String? =
        getTitle(throwable) ?: throwable.cause?.let { getFinalTitle(it) } ?: throwable.message


    private fun getFinalDetails(throwable: Throwable): String = buildString {
        getDetails(throwable)?.let { appendLine(it) }
        throwable.cause?.let { append(getFinalDetails(it)) }
    }

    private fun getStackTrace(throwable: Throwable): String = buildString {
        appendLine("Version: ${appVersion()}")
        appendLine(getFinalDetails(throwable))
        appendLine("---Stack Trace---")
        appendLine(throwable.stackTraceToString())
    }

    @Serializable
    data class Data(val title: String, val trace: String)

    fun Throwable.toData(context: Context) = run {
        val title = context.getFinalTitle(this) ?: context.getString(
            R.string.error_x,
            message ?: this::class.run { simpleName ?: java.name }
        )
        Data(title, getStackTrace(this))
    }


    fun FragmentActivity.getMessage(throwable: Throwable, view: View?): Message {
        val title = getFinalTitle(throwable) ?: getString(
            R.string.error_x,
            throwable.message ?: throwable::class.run { simpleName ?: java.name }
        )
        val root = throwable.rootCause
        return Message(
            message = title,
            when (root) {
                is AppException.Unauthorized ->
                    Message.Action(getString(R.string.logout_and_login)) {
                        runCatching { openLoginException(root, view) }
                    }

                is AppException.LoginRequired -> Message.Action(getString(R.string.login)) {
                    runCatching { openLoginException(root, view) }
                }

                else -> Message.Action(getString(R.string.view)) {
                    runCatching { openException(Data(title, getStackTrace(throwable)), view) }
                }
            }
        )
    }

    private fun FragmentActivity.openException(data: Data, view: View? = null) {
        openFragment<ExceptionFragment>(view, ExceptionFragment.getBundle(data))
    }

    fun FragmentActivity.openLoginException(
        it: AppException.LoginRequired, view: View? = null
    ) {
        if (it is AppException.Unauthorized) {
            val model by viewModel<LoginUserListViewModel>()
            model.logout(UserEntity(it.extension.type, it.extension.id, it.userId, ""))
        }
        openFragment<LoginFragment>(view, LoginFragment.getBundle(it))
    }


    fun MainActivity.setupExceptionHandler(handler: SnackBarHandler) {
        observe(handler.app.throwFlow) { throwable ->
            val message = getMessage(throwable, null)
            handler.create(message)
        }
    }

    private val client = OkHttpClient()
    suspend fun getPasteLink(data: Data) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://paste.rs")
            .post(data.trace.toRequestBody())
            .build()
        runCatching { client.newCall(request).await().body.string() }
    }
}