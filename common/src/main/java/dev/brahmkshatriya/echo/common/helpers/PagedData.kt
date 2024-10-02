package dev.brahmkshatriya.echo.common.helpers

sealed class PagedData<T : Any> {
    abstract fun clear()
    abstract suspend fun loadFirst(): List<T>
    abstract suspend fun loadAll(): List<T>

    class Single<T : Any>(
        val load: suspend () -> List<T>
    ) : PagedData<T>() {
        private var loaded = false
        val items = mutableListOf<T>()
        suspend fun loadList(): List<T> {
            if (loaded) return items
            items.addAll(load())
            loaded = true
            return items
        }

        override fun clear() {
            items.clear()
            loaded = false
        }

        override suspend fun loadFirst() = loadList()
        override suspend fun loadAll() = loadList()
    }

    class Continuous<T : Any>(
        val load: suspend (String?) -> Page<T, String?>,
    ) : PagedData<T>() {

        private val itemMap = mutableMapOf<String?, Page<T, String?>>()
        suspend fun loadList(continuation: String?): Page<T, String?> {
            val page = itemMap.getOrPut(continuation) {
                val (data, cont) = load(continuation)
                Page(data, cont)
            }
            return page
        }

        fun invalidate(continuation: String?) = itemMap.remove(continuation)
        override fun clear() = itemMap.clear()

        override suspend fun loadFirst() = loadList(null).data

        override suspend fun loadAll(): List<T> {
            val list = mutableListOf<T>()
            val init = loadList(null)
            list.addAll(init.data)
            var cont = init.continuation
            while (cont != null) {
                val page = load(cont)
                list.addAll(page.data)
                cont = page.continuation
            }
            return list
        }
    }

    class Concat<T : Any>(
        private vararg val sources: PagedData<T>
    ) : PagedData<T>() {
        init {
            require(sources.isNotEmpty()) { "Concat must have at least one source" }
        }

        override fun clear() = sources.forEach { it.clear() }
        override suspend fun loadFirst(): List<T> = sources.first().loadFirst()
        override suspend fun loadAll(): List<T> = sources.flatMap { it.loadAll() }
        suspend fun loadList(continuation: Pair<Int, String?>?): Page<T, Pair<Int, String?>?> {
            val (index, token) = continuation ?: (0 to null)
            val source = sources.getOrNull(index) ?: return Page(emptyList(), null)

            val page: Page<T, Pair<Int, String?>?> = when (source) {
                is Single -> Page(source.loadList(), index + 1 to null)

                is Continuous -> {
                    val page = source.loadList(token)
                    if (page.continuation != null)
                        Page(page.data, index to page.continuation)
                    else Page(page.data, index + 1 to null)
                }

                is Concat -> error("Nested Concat not supported")
            }
            return page
        }

        fun invalidate(continuation: Pair<Int, String?>?) {
            val (index, token) = continuation ?: return
            when (val source = sources[index]) {
                is Single -> source.clear()
                is Continuous -> source.invalidate(token)
                is Concat -> error("Nested Concat not supported")
            }
        }
    }

    companion object {
        fun <T : Any> empty() = Single<T> { emptyList() }
    }
}