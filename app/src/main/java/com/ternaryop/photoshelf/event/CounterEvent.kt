package com.ternaryop.photoshelf.event

/**
 * Created by dave on 27/10/17.
 * Event posted when a count is available
 */

data class CounterEvent(var type: Int, var count: Int) {
    companion object {
        const val NONE = 0
        const val BIRTHDAY = 1
        const val DRAFT = 2
        const val SCHEDULE = 3
    }
}
