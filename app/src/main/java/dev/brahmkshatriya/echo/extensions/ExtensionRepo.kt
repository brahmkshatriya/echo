package dev.brahmkshatriya.echo.extensions

import android.content.Context
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.common.clients.TrackerClient
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.common.helpers.Injectable
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.extensions.plugger.AndroidPluginLoader
import dev.brahmkshatriya.echo.extensions.plugger.ApkFileManifestParser
import dev.brahmkshatriya.echo.extensions.plugger.ApkManifestParser
import dev.brahmkshatriya.echo.extensions.plugger.ApkPluginSource
import dev.brahmkshatriya.echo.extensions.plugger.FileChangeListener
import dev.brahmkshatriya.echo.extensions.plugger.FilePluginSource
import dev.brahmkshatriya.echo.extensions.plugger.PackageChangeListener
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

sealed class ExtensionRepo<T : ExtensionClient>(
    private val context: Context,
    private val listener: PackageChangeListener,
    private val fileChangeListener: FileChangeListener,
    private vararg val repo: Pair<Metadata, Injectable<T>>
) : InjectablePluginRepo<Metadata, T> {
    abstract val type: ExtensionType

    private val composed by lazy {
        val loader = AndroidPluginLoader<T>(context)
        val dir = context.getPluginFileDir(type)
        val apkFilePluginRepo = InjectablePluginRepoImpl(
            FilePluginSource(dir, fileChangeListener.scope, fileChangeListener.getFlow(type)),
            ApkFileManifestParser(context.packageManager, ApkManifestParser(ImportType.File)),
            loader,
        )
        val appPluginRepo = InjectablePluginRepoImpl(
            ApkPluginSource(listener, context, "$FEATURE${type.feature}"),
            ApkManifestParser(ImportType.App),
            loader
        )
        val builtInRepo = BuiltInRepo(repo.toList())
        InjectableRepoComposer(context, type, builtInRepo, appPluginRepo, apkFilePluginRepo)
    }

    override fun getAllPlugins() = composed.getAllPlugins()

    companion object {
        const val FEATURE = "dev.brahmkshatriya.echo."
        fun Context.getPluginFileDir(type: ExtensionType) =
            File(filesDir, type.feature).apply { mkdirs() }
    }
}

class MusicExtensionRepo(
    context: Context,
    listener: PackageChangeListener,
    fileChangeListener: FileChangeListener,
    vararg repo: Pair<Metadata, Injectable<ExtensionClient>>
) : ExtensionRepo<ExtensionClient>(context, listener, fileChangeListener, *repo) {
    override val type = ExtensionType.MUSIC
}

class TrackerExtensionRepo(
    context: Context,
    listener: PackageChangeListener,
    fileChangeListener: FileChangeListener,
    vararg repo: Pair<Metadata, Injectable<TrackerClient>>
) : ExtensionRepo<TrackerClient>(context, listener, fileChangeListener, *repo) {
    override val type = ExtensionType.TRACKER
}

class LyricsExtensionRepo(
    context: Context,
    listener: PackageChangeListener,
    fileChangeListener: FileChangeListener,
    vararg repo: Pair<Metadata, Injectable<LyricsClient>>
) : ExtensionRepo<LyricsClient>(context, listener, fileChangeListener, *repo) {
    override val type = ExtensionType.LYRICS
}

class MiscExtensionRepo(
    context: Context,
    listener: PackageChangeListener,
    fileChangeListener: FileChangeListener,
    vararg repo: Pair<Metadata, Injectable<ExtensionClient>>
) : ExtensionRepo<ExtensionClient>(context, listener, fileChangeListener, *repo) {
    override val type = ExtensionType.MISC
}

class BuiltInRepo<T : ExtensionClient>(
    private val list: List<Pair<Metadata, Injectable<T>>>
) : InjectablePluginRepo<Metadata, T> {
    override fun getAllPlugins() = MutableStateFlow(list.map { Result.success(it) })
}