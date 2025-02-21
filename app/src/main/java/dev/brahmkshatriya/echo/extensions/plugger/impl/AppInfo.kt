package dev.brahmkshatriya.echo.extensions.plugger.impl

import android.content.pm.ApplicationInfo
import android.content.pm.FeatureInfo
import android.content.pm.PackageInfo
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.extensions.plugger.ExtensionsRepo.Companion.FEATURE

data class AppInfo(
    val path: String,
    val appInfo: ApplicationInfo,
    val type: ExtensionType
) {

    constructor(path: String, packageInfo: PackageInfo) : this(
        path,
        packageInfo.applicationInfo!!,
        packageInfo.reqFeatures!!.toExtensionType()
    )

    constructor(packageInfo: PackageInfo) : this(
        packageInfo.applicationInfo!!.sourceDir,
        packageInfo
    )

    companion object {
        private fun Array<FeatureInfo>.toExtensionType(): ExtensionType {
            val feature = first { it.name.startsWith(FEATURE) }
            val type = feature.name.substringAfter(FEATURE)
            return ExtensionType.entries.first { it.feature == type }
        }
    }
}