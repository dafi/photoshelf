package com.ternaryop.photoshelf.util.sort

/**
 * Created by dave on 12/06/16.
 * Common implementation
 */
abstract class AbsSortable<T>(final override val isDefaultAscending: Boolean, val sortId: Int) : Sortable<T> {
    final override var isAscending = isDefaultAscending

    fun resetDefault() {
        isAscending = isDefaultAscending
    }
}
