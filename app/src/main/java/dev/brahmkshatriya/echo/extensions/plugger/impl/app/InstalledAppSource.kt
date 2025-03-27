package dev.brahmkshatriya.echo.extensions.plugger.impl.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import dev.brahmkshatriya.echo.extensions.plugger.ExtensionsRepo.Companion.FEATURE
import dev.brahmkshatriya.echo.extensions.plugger.ExtensionsRepo.Companion.PACKAGE_FLAGS
import dev.brahmkshatriya.echo.extensions.plugger.impl.AppInfo
import dev.brahmkshatriya.echo.extensions.plugger.interfaces.PluginSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InstalledAppSource(
    private val context: Context,
    private val scope: CoroutineScope,
) : PluginSource<AppInfo>, BroadcastReceiver() {

    private val loadedPlugins = MutableStateFlow(listOf<AppInfo>())

    override fun getSourceFiles() = loadedPlugins.asStateFlow()

    private suspend fun Context.getStaticPackages() =
        withContext(Dispatchers.IO) {
            val packages = runCatching {
                packageManager.getInstalledPackages(PACKAGE_FLAGS).filter {
                    it.reqFeatures.orEmpty().any { featureInfo ->
                        featureInfo?.name?.startsWith(FEATURE) ?: false
                    }
                }
            }.getOrNull().orEmpty()
            packages.mapNotNull { runCatching { AppInfo(it) }.getOrNull() }
        }

    private fun onPackageChanged() {
        scope.launch { loadedPlugins.value = context.getStaticPackages() }
    }

    init {
        onPackageChanged()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        context.registerReceiver(this, filter)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        onPackageChanged()
    }
}