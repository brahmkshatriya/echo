package dev.brahmkshatriya.echo.utils

import java.util.WeakHashMap
import kotlin.reflect.KProperty

class Sticky<R, T>(private val initializer: R.() -> T) {

    private val map = WeakHashMap<R, Result<T>>()
    operator fun getValue(thisRef: R, property: KProperty<*>): T {
        return map.getOrPut(thisRef) {
            runCatching { initializer(thisRef) }
        }.getOrThrow()
    }

    operator fun setValue(thisRef: R, property: KProperty<*>, value: T) {
        map[thisRef] = Result.success(value)
    }

    companion object {
        fun <R, T> sticky(initializer: R.() -> T) = Sticky(initializer)
    }
}