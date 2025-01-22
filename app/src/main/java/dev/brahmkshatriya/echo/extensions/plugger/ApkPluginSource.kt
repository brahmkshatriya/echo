package dev.brahmkshatriya.echo.extensions.plugger

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import tel.jeelpa.plugger.PluginSource

class ApkPluginSource(
    packageChangeListener: PackageChangeListener,
    private val context: Context,
    private val featureName: String,
) : PluginSource<AppInfo>, PackageChangeListener.Listener {

    override fun getSourceFiles() = loadedPlugins.asStateFlow()

    private val loadedPlugins = MutableStateFlow(context.getStaticPackages(featureName))

    private fun Context.getStaticPackages(featureName: String): List<AppInfo> {
        return packageManager.getInstalledPackages(PACKAGE_FLAGS).filter {
            it.reqFeatures.orEmpty().any { featureInfo ->
                featureInfo.name == featureName
            }
        }.mapNotNull { info ->
            info.applicationInfo?.let {
                AppInfo(it.sourceDir, it)
            }
        }
    }

    companion object {
        @Suppress("Deprecation")
        val PACKAGE_FLAGS = PackageManager.GET_CONFIGURATIONS or
                PackageManager.GET_META_DATA or
                PackageManager.GET_SIGNATURES or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else 0
    }

    override fun onPackageChanged() {
        loadedPlugins.value = context.getStaticPackages(featureName)
    }

    init {
        packageChangeListener.add(this)
    }
}