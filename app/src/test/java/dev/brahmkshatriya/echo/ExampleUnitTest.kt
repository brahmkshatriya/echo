package dev.brahmkshatriya.echo

import dev.brahmkshatriya.echo.ui.editplaylist.EditPlaylistViewModel
import dev.brahmkshatriya.echo.ui.editplaylist.EditPlaylistViewModel.Companion.computeActions
import org.junit.Test

class ExampleUnitTest {

    private fun test(old: List<String>, new: List<String>) {
        val actions = computeActions(old, new)
        val result = old.toMutableList()
        println("old: $old")
        println("new: $new")
        actions.forEach {
            println("Action: $it")
            when (it) {
                is EditPlaylistViewModel.Action.Add -> result.addAll(it.index, it.items)
                is EditPlaylistViewModel.Action.Move -> result.add(it.to, result.removeAt(it.from))
                is EditPlaylistViewModel.Action.Remove -> it.indexes.forEach { i ->
                    result.removeAt(i)
                }
            }
        }
        assert(result == new)
    }

    @Test
    fun testInsert() {
        val old = listOf("A", "B", "C")
        val new = listOf("A", "D", "B", "C")
        test(old, new)
    }

    @Test
    fun testRemove() {
        val old = listOf("A", "B", "C")
        val new = listOf("A", "C")
        test(old, new)
    }

    @Test
    fun testMove() {
        val old = listOf("A", "B", "C")
        val new = listOf("B", "C", "A")
        test(old, new)
    }

    @Test
    fun testComplex() {
        val old = listOf("A", "B", "C", "D", "E")
        val new = listOf("A", "D", "B", "C", "F", "E")
        test(old, new)
    }

    @Test
    fun testInsertAtEnd() {
        val old = listOf("A", "B", "C")
        val new = listOf("A", "B", "C", "D")
        test(old, new)
    }

    @Test
    fun testMoveToLast() {
        val old = listOf("A", "B", "C", "D", "E")
        val new = listOf("B", "C", "D", "E", "A")
        test(old, new)
    }

    @Test
    fun testMoveToSecondLast() {
        val old = listOf("A", "B", "C", "D", "E", "F", "G")
        val new = listOf("B", "C", "D", "A", "F", "G", "E")
        test(old, new)
    }

    @Test
    fun testMoveToFirst() {
        val old = listOf("A", "B", "C", "D", "E")
        val new = listOf("D", "B", "C", "A", "E")
        test(old, new)
    }

    @Test
    fun testReverse() {
        val old = listOf("A", "B", "C", "D", "E")
        val new = listOf("E", "D", "C", "B", "A")
        test(old, new)
    }
}