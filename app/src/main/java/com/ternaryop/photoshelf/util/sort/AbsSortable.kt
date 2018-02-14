package com.ternaryop.photoshelf.util.sort

/**
 * Created by dave on 12/06/16.
 * Common implementation
 */
abstract class AbsSortable(final override val isDefaultAscending: Boolean, val sortId: Int) : Sortable {
    final override var isAscending: Boolean = false

    init {
        this.isAscending = isDefaultAscending
    }
}
