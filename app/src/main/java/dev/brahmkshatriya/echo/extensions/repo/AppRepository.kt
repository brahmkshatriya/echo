package dev.brahmkshatriya.echo.extensions.repo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.common.helpers.Injectable
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.extensions.repo.ExtensionParser.Companion.FEATURE
import dev.brahmkshatriya.echo.extensions.repo.ExtensionParser.Companion.PACKAGE_FLAGS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.WeakHashMap
import kotlin.coroutines.suspendCoroutine

class AppRepository(
    private val scope: CoroutineScope,
    private val context: Context,
    private val parser: ExtensionParser
) : ExtensionRepository {
    private suspend fun Context.getStaticPackages() = withContext(Dispatchers.IO) {
        packageManager.getInstalledPackages(PACKAGE_FLAGS).mapNotNull {
            runCatching {
                val isExtension = it.reqFeatures.orEmpty().any { featureInfo ->
                    featureInfo?.name?.startsWith(FEATURE) ?: false
                }
                if (isExtension) File(it.applicationInfo!!.sourceDir!!) else null
            }.getOrNull()
        }
    }

    private val map =
        WeakHashMap<String, Pair<String, Result<Pair<Metadata, Injectable<ExtensionClient>>>>>()

    override suspend fun loadExtensions() =
        parser.getAllDynamically(ImportType.App, map, context.getStaticPackages())

    override val flow = channelFlow {
        send(loadExtensions())
        suspendCoroutine {
            val receiver = Receiver {
                println("Received broadcast")
                scope.launch {
                    println("launching loadExtensions")
                    send(loadExtensions())
                    println("loadExtensions sent")
                }
            }
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addDataScheme("package")
            }
            ContextCompat.registerReceiver(
                context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }


    class Receiver(private val onReceive: () -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = onReceive()
    }
}