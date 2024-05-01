package dev.brahmkshatriya.echo.utils

sealed class ListAction<T> {
    data class Add<T>(val index: Int, val items: MutableList<T>) : ListAction<T>()
    data class Remove<T>(val items: List<T>) : ListAction<T>()
    data class Move<T>(val from: Int, val to: Int) : ListAction<T>()

    companion object {
        fun <T> getActions(
            oldList: List<T>,
            newList: List<T>,
            predicate: T.(T) -> Boolean = { this == it }
        ): List<ListAction<T>> {
            val actions = mutableListOf<ListAction<T>>()
            val modified = oldList.toMutableList()

            // Handle removed items
            val removedItems = oldList.filterNot { item -> newList.any { item.predicate(it) } }
            if (removedItems.isNotEmpty()) {
                modified.removeAll(removedItems)
                actions.add(Remove(removedItems))
            }

            // Handle added and moved items
            newList.forEachIndexed { index, item ->
                val oldIndex = modified.indexOfFirst { it.predicate(item) }
                if (oldIndex == -1) {
                    modified.add(index, item)
                    actions.add(Add(index, mutableListOf(item)))
                } else if (index != oldIndex) {
                    modified.removeAt(oldIndex)
                    modified.add(index, item)
                    actions.add(Move(index, oldIndex))
                }
            }

            // Combine continuous Add ListActions
            var i = 0
            while (i < actions.size - 1) {
                val curr = actions[i]
                val next = actions[i + 1]
                if (curr is Add && next is Add && curr.index + curr.items.size == next.index) {
                    curr.items.addAll(next.items)
                    actions.removeAt(i + 1)
                } else if (curr is Move && next is Move && curr.to == next.from) {
                    actions[i] = Move(curr.from, next.to)
                    actions.removeAt(i + 1)
                } else {
                    i++
                }
            }

            return actions
        }
    }
}