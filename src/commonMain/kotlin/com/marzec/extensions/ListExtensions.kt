package com.marzec.extensions

fun <T> List<T>.replaceIf(condition: (T) -> Boolean, replace: (T) -> T): List<T> = map { item: T ->
    if (condition(item)) {
        replace(item)
    } else {
        item
    }
}

fun <T> List<T>.replaceIfIndexed(condition: (Int, T) -> Boolean, replace: (T) -> T): List<T> =
    mapIndexed { index: Int, item: T ->
        if (condition(index, item)) {
            replace(item)
        } else {
            item
        }
    }

fun <T> List<T>.filterWithSearch(
    search: String,
    stringsToCompare: (T) -> List<String>
): List<T> {
    val searchQuery = search.trim().split(" ")
    return filter { item ->
        searchQuery == listOf("") || searchQuery.all { searchPart ->
            stringsToCompare(item).any { text ->
                text.contains(
                    searchPart,
                    ignoreCase = true
                )
            }
        }
    }
}

fun <T> MutableList<T>.swap(index1: Int, index2: Int) = apply {
    val tmp = this[index1]
    this[index1] = this[index2]
    this[index2] = tmp
}
