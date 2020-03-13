package com.ternaryop.photoshelf.util

fun <T> List<T>.near(startIndex: Int, distance: Int): List<T> {
    val leftLower = 0.coerceAtLeast(startIndex - distance)
    val leftUpper = startIndex - 1

    val sublist = mutableListOf<T>()

    for (i in leftLower..leftUpper) {
        sublist.add(this[i])
    }

    val rightLower = startIndex + 1
    val rightUpper = (startIndex + distance).coerceAtMost(size - 1)

    for (i in rightLower..rightUpper) {
        sublist.add(this[i])
    }

    return sublist
}
