package dev.brahmkshatriya.echo.extensions.plugger

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tel.jeelpa.plugger.PluginSource

class ApkPluginSource(
    packageChangeListener: PackageChangeListener,
    private val context: Context,
    private val featureName: String,
) : PluginSource<AppInfo>, PackageChangeListener.Listener {

    private val loadedPlugins = MutableStateFlow(listOf<AppInfo>())

    override fun getSourceFiles() = loadedPlugins.asStateFlow()

    private suspend fun Context.getStaticPackages(featureName: String) =
        withContext(Dispatchers.IO) {
            runCatching {
                packageManager.getInstalledPackages(PACKAGE_FLAGS).filter {
                    it.reqFeatures.orEmpty().any { featureInfo ->
                        featureInfo.name == featureName
                    }
                }.mapNotNull { info ->
                    info.applicationInfo?.let {
                        AppInfo(it.sourceDir, it)
                    }
                }
            }.getOrElse { emptyList() }
        }

    companion object {
        @Suppress("Deprecation")
        val PACKAGE_FLAGS = PackageManager.GET_CONFIGURATIONS or
                PackageManager.GET_META_DATA or
                PackageManager.GET_SIGNATURES or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else 0
    }

    override suspend fun onPackageChanged() {
        loadedPlugins.value = context.getStaticPackages(featureName)
    }

    init {
        packageChangeListener.add(this)
        packageChangeListener.scope.launch {
            loadedPlugins.value = context.getStaticPackages(featureName)
        }
    }
}