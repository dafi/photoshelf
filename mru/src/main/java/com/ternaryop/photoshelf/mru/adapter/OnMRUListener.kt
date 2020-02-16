package com.ternaryop.photoshelf.mru.adapter

/**
 * Created by dave on 16/12/17.
 * MRU actions
 */

interface OnMRUListener {
    fun onItemSelect(item: String)
    fun onItemDelete(item: String)
}
