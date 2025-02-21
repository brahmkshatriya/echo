package dev.brahmkshatriya.echo.extensions.plugger

import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.clients.ExtensionClient
import dev.brahmkshatriya.echo.common.clients.LoginClient
import dev.brahmkshatriya.echo.common.clients.LyricsClient
import dev.brahmkshatriya.echo.common.clients.TrackerClient
import dev.brahmkshatriya.echo.common.helpers.ExtensionType
import dev.brahmkshatriya.echo.common.helpers.Injectable
import dev.brahmkshatriya.echo.common.models.Metadata
import dev.brahmkshatriya.echo.common.providers.LyricsExtensionsProvider
import dev.brahmkshatriya.echo.common.providers.MessageFlowProvider
import dev.brahmkshatriya.echo.common.providers.MetadataProvider
import dev.brahmkshatriya.echo.common.providers.MiscExtensionsProvider
import dev.brahmkshatriya.echo.common.providers.MusicExtensionsProvider
import dev.brahmkshatriya.echo.common.providers.TrackerExtensionsProvider
import dev.brahmkshatriya.echo.extensions.ExtensionLoader
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.await
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.inject
import dev.brahmkshatriya.echo.extensions.ExtensionUtils.injectWith
import dev.brahmkshatriya.echo.extensions.Extensions
import dev.brahmkshatriya.echo.extensions.db.models.CurrentUser
import dev.brahmkshatriya.echo.extensions.exceptions.RequiredExtensionsMissingException
import dev.brahmkshatriya.echo.extensions.plugger.interfaces.PluginRepo
import dev.brahmkshatriya.echo.utils.CoroutineUtils.collectWith
import dev.brahmkshatriya.echo.utils.Delegated.Companion.delegated
import dev.brahmkshatriya.echo.utils.SettingsUtils.getSettings
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class InjectedRepo(
    private val extensionLoader: ExtensionLoader,
    private vararg val repos: PluginRepo<Metadata, ExtensionClient>
) : PluginRepo<Metadata, ExtensionClient> {

    override fun getAllPlugins() = repos.map {
        it.getAllPlugins()
    }.reduce { a, b -> a.combine(b) { x, y -> x + y } }.map { list ->
        list.groupBy { it.getOrNull()?.first?.run { type to id } }.map { entry ->
            entry.value.minBy { it.getOrNull()?.first?.importType?.ordinal ?: Int.MAX_VALUE }
        }
    }.onEach { list ->
        list.onEach { it.getOrNull()?.run { second.injected(first) } }
    }.combine(extensionLoader.db.extensionEnabledFlow) { list, enabledList ->
        val enabledMap = enabledList.associate { (it.type to it.id) to it.enabled }
        list.map { result ->
            result.mapCatching { (metadata, injectable) ->
                val key = metadata.run { type to id }
                val isEnabled = enabledMap[key] ?: metadata.isEnabled
                metadata.copy(isEnabled = isEnabled) to injectable
            }
        }
    }

    private fun Injectable<ExtensionClient>.injected(
        metadata: Metadata
    ) = inject {
        verifyType(metadata.type)
        if (this is MetadataProvider) setMetadata(metadata)
        if (this is MessageFlowProvider) setMessageFlow(extensionLoader.app.messageFlow)
        setSettings(getSettings(extensionLoader.app.context, metadata))
        onInitialize()
    }

    init {
        extensionLoader.run {
            scope.launch {
                extensions.combined.collect { list ->
                    list.forEach {
                        if (!it.isEnabled) return@forEach
                        it.inject(app.throwFlow) { injectProviders(extensions, this) }
                    }
                }
            }
            scope.launch {
                extensions.all.collectWith(db.currentUsersFlow) { list, users ->
                    list?.forEach { ext ->
                        val newCurr = users.getUser(ext)
                        val item = ext.instance.user
                        val shouldInject = !item.initialized || item.current != newCurr
                        if (!shouldInject) return@forEach
                        item.initialized = true
                        item.current = newCurr
                        scope.launch {
                            ext.injectWith<LoginClient>(app.throwFlow) {
                                val user = newCurr?.let { db.getUser(it) }
                                onSetLoginUser(user)
                            }
                        }
                    }
                }
            }
        }
    }

    private class UserExt(
        var initialized: Boolean = false,
        var current: CurrentUser? = null
    )

    companion object {

        private fun ExtensionClient.verifyType(type: ExtensionType) {
            val className = when (type) {
                ExtensionType.TRACKER ->
                    if (this is TrackerClient) null else TrackerClient::class.simpleName

                ExtensionType.LYRICS ->
                    if (this is LyricsClient) null else LyricsClient::class.simpleName

                else -> null
            }
            if (className != null)
                throw Exception("${this::class.simpleName} needs to be $className")
        }

        private val Injectable<*>.user by delegated { UserExt(false) }

        private fun <T, R : Extension<*>> T.inject(
            required: List<String>,
            extensions: List<R>,
            set: T.(List<R>) -> Unit
        ) {
            if (required.isEmpty()) set(extensions)
            else {
                val filtered = extensions.filter { it.metadata.id in required }
                if (filtered.size == required.size) set(filtered)
                else throw RequiredExtensionsMissingException(required)
            }
        }

        suspend fun injectProviders(extensions: Extensions, client: ExtensionClient) {
            (client as? MusicExtensionsProvider)?.run {
                inject(requiredMusicExtensions, extensions.music.await()) {
                    setMusicExtensions(it)
                }
            }
            (client as? TrackerExtensionsProvider)?.run {
                inject(requiredTrackerExtensions, extensions.tracker.await()) {
                    setTrackerExtensions(it)
                }
            }
            (client as? LyricsExtensionsProvider)?.run {
                inject(requiredLyricsExtensions, extensions.lyrics.await()) {
                    setLyricsExtensions(it)
                }
            }
            (client as? MiscExtensionsProvider)?.run {
                inject(requiredMiscExtensions, extensions.misc.await()) {
                    setMiscExtensions(it)
                }
            }
        }

        fun List<CurrentUser>.getUser(ext: Extension<*>): CurrentUser? {
            val curr = find { it.type == ext.type && it.extId == ext.id }
            return curr
        }
    }
}