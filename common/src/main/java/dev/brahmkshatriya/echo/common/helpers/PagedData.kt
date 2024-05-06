package dev.brahmkshatriya.echo.common.helpers

sealed interface PagedData<T : Any> {

    fun interface Single<T : Any> : PagedData<T> {
        suspend fun load(): List<T>
    }

    fun interface Continuous<T : Any> : PagedData<T> {
        suspend fun load(continuation: String?): Page<T, String?>
    }

    companion object {
        suspend fun <T : Any> PagedData<T>.all() = when (this) {
            is Continuous -> {
                val list = mutableListOf<T>()
                val init = load(null)
                list.addAll(init.data)
                var cont = init.continuation
                while (cont != null) {
                    val page = load(cont)
                    list.addAll(page.data)
                    cont = page.continuation
                }
                list
            }

            is Single -> load()
        }

        suspend fun <T : Any> PagedData<T>.first() = when (this) {
            is Continuous<T> -> load(null).data
            is Single<T> -> load()
        }
    }
}