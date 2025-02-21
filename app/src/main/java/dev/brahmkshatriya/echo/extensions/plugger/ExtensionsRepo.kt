package dev.brahmkshatriya.echo.extensions.plugger

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.common.helpers.Injectable
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.plugger.impl.BuiltInRepo
import dev.brahmkshatriya.echo.extensions.plugger.impl.DexLoader
import dev.brahmkshatriya.echo.extensions.plugger.impl.InjectablePluginRepo
import dev.brahmkshatriya.echo.extensions.plugger.impl.app.ApkManifestParser
import dev.brahmkshatriya.echo.extensions.plugger.impl.app.InstalledAppSource
import dev.brahmkshatriya.echo.extensions.plugger.impl.file.FileParser
import dev.brahmkshatriya.echo.extensions.plugger.impl.file.FilePluginSource
import dev.brahmkshatriya.echo.extensions.plugger.interfaces.PluginRepo
import java.io.File

class ExtensionsRepo(
    extensionLoader: ExtensionLoader,
    vararg extensions: Pair<Metadata, Injectable<ExtensionClient>>
) : PluginRepo<Metadata, ExtensionClient> {

    private val context = extensionLoader.app.context
    private val scope = extensionLoader.scope

    private val injectedRepo by lazy {
        val fileSource = FilePluginSource(context, scope)
        val fileParser = FileParser(context.packageManager, ApkManifestParser(ImportType.File))

        val installedAppSource = InstalledAppSource(context, scope)
        val appParser = ApkManifestParser(ImportType.App)

        val loader = DexLoader(context)

        val apkFilePluginRepo = InjectablePluginRepo(fileSource, fileParser, loader)
        val appPluginRepo = InjectablePluginRepo(installedAppSource, appParser, loader)
        val builtInRepo = BuiltInRepo(extensions.toList())
        InjectedRepo(
            extensionLoader, builtInRepo, appPluginRepo, apkFilePluginRepo
        )
    }

    override fun getAllPlugins() = injectedRepo.getAllPlugins()

    companion object {
        const val FEATURE = "dev.brahmkshatriya.echo."

        @Suppress("Deprecation")
        val PACKAGE_FLAGS = PackageManager.GET_CONFIGURATIONS or
                PackageManager.GET_META_DATA or
                PackageManager.GET_SIGNATURES or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else 0

        fun Context.getPluginFileDir() = File(filesDir, "extensions").apply { mkdirs() }
    }
}