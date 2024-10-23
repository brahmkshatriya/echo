package dev.brahmkshatriya.echo.extensions

import android.content.Context
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.common.clients.TrackerClient
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.helpers.ImportType
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.extensions.plugger.AndroidPluginLoader
import dev.brahmkshatriya.echo.extensions.plugger.ApkFileManifestParser
import dev.brahmkshatriya.echo.extensions.plugger.ApkManifestParser
import dev.brahmkshatriya.echo.extensions.plugger.ApkPluginSource
import dev.brahmkshatriya.echo.extensions.plugger.FileChangeListener
import dev.brahmkshatriya.echo.extensions.plugger.FilePluginSource
import dev.brahmkshatriya.echo.extensions.plugger.LazyPluginRepo
import dev.brahmkshatriya.echo.extensions.plugger.LazyPluginRepoImpl
import dev.brahmkshatriya.echo.extensions.plugger.LazyRepoComposer
import dev.brahmkshatriya.echo.extensions.plugger.PackageChangeListener
import dev.brahmkshatriya.echo.extensions.plugger.catchLazy
import dev.brahmkshatriya.echo.utils.getSettings
import tel.jeelpa.plugger.utils.mapState
import java.io.File

sealed class ExtensionRepo<T : ExtensionClient>(
    private val context: Context,
    private val listener: PackageChangeListener,
    private val fileChangeListener: FileChangeListener,
    private vararg val repo: LazyPluginRepo<Metadata, T>
) : LazyPluginRepo<Metadata, T> {
    abstract val type: ExtensionType

    private val composed by lazy {
        val loader = AndroidPluginLoader<T>(context)
        val dir = context.getPluginFileDir(type)
        val apkFilePluginRepo = LazyPluginRepoImpl(
            FilePluginSource(dir, fileChangeListener.scope, fileChangeListener.getFlow(type)),
            ApkFileManifestParser(context.packageManager, ApkManifestParser(ImportType.File)),
            loader,
        )
        val appPluginRepo = LazyPluginRepoImpl(
            ApkPluginSource(listener, context, "$FEATURE${type.feature}"),
            ApkManifestParser(ImportType.App),
            loader
        )
        LazyRepoComposer(*repo, appPluginRepo, apkFilePluginRepo)
    }

    private fun injected() = composed.getAllPlugins().mapState { list ->
        list.map {
            runCatching {
                val plugin = it.getOrThrow()
                val (metadata, resultLazy) = plugin
                metadata to catchLazy {
                    val instance = resultLazy.value.getOrThrow()
                    //Injection
                    instance.setSettings(getSettings(context, type, metadata))

                    instance
                }
            }
        }
    }

    override fun getAllPlugins() = injected()

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
    vararg repo: LazyPluginRepo<Metadata, ExtensionClient>
) : ExtensionRepo<ExtensionClient>(context, listener, fileChangeListener, *repo) {
    override val type = ExtensionType.MUSIC
}

class TrackerExtensionRepo(
    context: Context,
    listener: PackageChangeListener,
    fileChangeListener: FileChangeListener,
    vararg repo: LazyPluginRepo<Metadata, TrackerClient>
) : ExtensionRepo<TrackerClient>(context, listener, fileChangeListener, *repo) {
    override val type = ExtensionType.TRACKER
}

class LyricsExtensionRepo(
    context: Context,
    listener: PackageChangeListener,
    fileChangeListener: FileChangeListener,
    vararg repo: LazyPluginRepo<Metadata, LyricsClient>
) : ExtensionRepo<LyricsClient>(context, listener, fileChangeListener, *repo) {
    override val type = ExtensionType.LYRICS
}