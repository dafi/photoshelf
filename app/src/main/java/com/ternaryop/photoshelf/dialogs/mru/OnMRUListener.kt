package com.ternaryop.photoshelf.dialogs.mru

/**
 * Created by dave on 16/12/17.
 * MRU actions
 */

interface OnMRUListener {
    fun onItemsSelected(dialog: MRUDialog, positions: IntArray)
    fun onItemDelete(dialog: MRUDialog, position: Int)
}
