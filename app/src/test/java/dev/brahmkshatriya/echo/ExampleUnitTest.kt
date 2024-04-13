package dev.brahmkshatriya.echo

import dev.brahmkshatriya.echo.data.offline.resolvers.sortedBy
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testLevenshtein() {
        val query = "tears"
        val target = listOf("Humans in the evening","tears in the evening","Skrillex", "Fox Stevenson", "steven fox")
        println("${target.sortedBy(query) { it }}")
    }
}