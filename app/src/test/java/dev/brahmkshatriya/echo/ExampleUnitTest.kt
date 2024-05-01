package dev.brahmkshatriya.echo

import dev.brahmkshatriya.echo.utils.ListAction
import dev.brahmkshatriya.echo.utils.ListAction.Companion.getActions
import org.junit.Test
import java.util.Collections.swap

class ExampleUnitTest {

    @Test
    fun testComparatorActions() {
        val oldList = listOf("C")
        val newList = listOf("A", "F", "Z", "C")
        val modifiedList = oldList.toMutableList()
        getActions(oldList, newList).forEach {
            when (it) {
                is ListAction.Add -> modifiedList.addAll(it.index, it.items)
                is ListAction.Move -> swap(modifiedList, it.to, it.from)
                is ListAction.Remove -> modifiedList.removeAll(it.items)
            }
        }
        assert(modifiedList == newList)
    }

}