package dev.brahmkshatriya.echo

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testLazy() {
        val lazyValue = lazy {
            println("computed!")
            runCatching { 42 }
        }

        runBlocking {
            delay(1000)
            println("lazyValue: ${lazyValue.value.getOrThrow()}")
        }
    }
}