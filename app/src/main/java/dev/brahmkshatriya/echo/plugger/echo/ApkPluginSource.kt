package dev.brahmkshatriya.echo.plugger.echo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tel.jeelpa.plugger.PluginSource


class ApkPluginSource(
    context: Context,
    private val featureName: String,
) : PluginSource<ApplicationInfo> {

    override fun getSourceFiles(): StateFlow<List<ApplicationInfo>> = loadedPlugins.asStateFlow()

    private val loadedPlugins = MutableStateFlow(context.getStaticPackages(featureName))
    private val appInstallReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            loadedPlugins.value = context.getStaticPackages(featureName)
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        context.registerReceiver(appInstallReceiver, filter)
    }

    companion object {

        @Suppress("Deprecation")
        private val PACKAGE_FLAGS = PackageManager.GET_CONFIGURATIONS or
                PackageManager.GET_META_DATA or
                PackageManager.GET_SIGNATURES or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else 0)

        private fun Context.getStaticPackages(featureName: String): List<ApplicationInfo> {
            return packageManager.getInstalledPackages(PACKAGE_FLAGS)
                .filter {
                    it.reqFeatures.orEmpty().any { featureInfo ->
                        featureInfo.name == featureName
                    }
                }.mapNotNull { it.applicationInfo }
        }
    }
}